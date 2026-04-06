// Package main is the entry point for the telemedicine service.
// It wires up all dependencies (database, Kafka, Agora, Prometheus)
// and starts the HTTP server.
//
// The actual business logic lives in the internal/ packages:
//   - internal/config     — environment-based configuration
//   - internal/model      — data structures (DTOs, DB models)
//   - internal/store      — PostgreSQL queries (sessions, participants, events)
//   - internal/event      — Kafka + DB dual-write event publisher
//   - internal/agora      — Agora RTC token generation
//   - internal/handler    — HTTP endpoint handlers
//   - internal/middleware — correlation ID and Prometheus metrics
package main

import (
	"context"
	"net/http"
	"os"
	"time"

	"github.com/aferent/telemedicine-service/internal/agora"
	"github.com/aferent/telemedicine-service/internal/config"
	"github.com/aferent/telemedicine-service/internal/event"
	"github.com/aferent/telemedicine-service/internal/handler"
	"github.com/aferent/telemedicine-service/internal/middleware"
	"github.com/aferent/telemedicine-service/internal/store"
	"github.com/go-chi/chi/v5"
	"github.com/jackc/pgx/v5/pgxpool"
	"github.com/prometheus/client_golang/prometheus"
	"github.com/prometheus/client_golang/prometheus/promhttp"
	"github.com/rs/zerolog"
	"github.com/segmentio/kafka-go"
)

func main() {
	// ── 1. Load config and create logger ──────────────────────────────
	cfg := config.Load()
	logger := zerolog.New(os.Stdout).With().
		Timestamp().
		Str("service", "telemedicine-service").
		Logger()

	// ── 2. Connect to PostgreSQL ──────────────────────────────────────
	ctx := context.Background()
	db, err := pgxpool.New(ctx, cfg.DBDSN)
	if err != nil {
		logger.Fatal().Err(err).Msg("failed to create db pool")
	}
	defer db.Close()

	// Run DDL migrations (CREATE TABLE IF NOT EXISTS — safe on every startup)
	if err := store.InitSchema(ctx, db); err != nil {
		logger.Fatal().Err(err).Msg("failed to init schema")
	}

	// ── 3. Set up Kafka writer ────────────────────────────────────────
	kafkaWriter := &kafka.Writer{
		Addr:                   kafka.TCP(cfg.KafkaBootstrapServers),
		Topic:                  cfg.KafkaTopicSessionEvents,
		Balancer:               &kafka.LeastBytes{},
		AllowAutoTopicCreation: true,
		WriteTimeout:           3 * time.Second,
	}
	defer kafkaWriter.Close()

	// ── 4. Set up Prometheus metrics ──────────────────────────────────
	reqCtr := prometheus.NewCounterVec(prometheus.CounterOpts{
		Name: "telemedicine_http_requests_total",
		Help: "Total HTTP requests",
	}, []string{"method", "path", "status"})
	latency := prometheus.NewHistogramVec(prometheus.HistogramOpts{
		Name:    "telemedicine_http_request_duration_seconds",
		Help:    "HTTP latency",
		Buckets: prometheus.DefBuckets,
	}, []string{"method", "path"})
	prometheus.MustRegister(reqCtr, latency)

	// ── 5. Build stores (database access layer) ──────────────────────
	sessionStore := &store.SessionStore{DB: db}
	participantStore := &store.ParticipantStore{DB: db}
	eventStore := &store.EventStore{DB: db}
	appointmentStore := &store.AppointmentStore{
		DB:                        db,
		DefaultSessionDurationSec: cfg.DefaultSessionDurationSec,
		Mode:                      cfg.AppointmentMode,
	}

	// ── 6. Build event publisher (Kafka + DB dual-write) ─────────────
	publisher := &event.Publisher{
		EventStore: eventStore,
		Kafka:      kafkaWriter,
		Log:        logger,
	}

	// ── 7. Build Agora token builder ─────────────────────────────────
	tokenBuilder := &agora.TokenBuilder{
		AppID:          cfg.AgoraAppID,
		AppCertificate: cfg.AgoraAppCertificate,
		TokenTTLSec:    cfg.AgoraTokenTTLSec,
	}

	// ── 8. Build the handler with all dependencies ───────────────────
	h := &handler.Handler{
		Config:            cfg,
		Log:               logger,
		Sessions:          sessionStore,
		Participants:      participantStore,
		Appointments:      appointmentStore,
		Events:            eventStore,
		Publisher:         publisher,
		AgoraTokenBuilder: tokenBuilder,
	}

	// ── 9. Configure routes ──────────────────────────────────────────
	r := chi.NewRouter()

	// Global middleware — runs on every request
	r.Use(middleware.CorrelationID)
	r.Use(middleware.Metrics(reqCtr, latency))

	// Infrastructure endpoints (healthcheck, metrics, liveness)
	r.Get("/health", h.HandleHealth)
	r.Get("/ready", h.HandleHealth)
	r.Get("/actuator/health", h.HandleHealth)
	r.Get("/actuator/health/liveness", h.HandleLiveness)
	r.Handle("/actuator/prometheus", promhttp.Handler())

	// Test frontend
	r.Get("/test", h.HandleFrontend)
	r.Get("/", func(w http.ResponseWriter, _ *http.Request) {
		w.WriteHeader(http.StatusOK)
		_, _ = w.Write([]byte("telemedicine-service up"))
	})

	// Session endpoints — these are the core API
	r.Route("/sessions", func(sr chi.Router) {
		sr.Post("/join/{appointmentId}", h.HandleJoin)
		sr.Get("/{appointmentId}/status", h.HandleStatus)
		sr.Post("/{appointmentId}/end", h.HandleEnd)
		sr.Get("/history", h.HandleHistory)
		sr.Get("/{appointmentId}/remaining-duration", h.HandleRemainingDuration)
		sr.Patch("/{appointmentId}/duration", h.HandleDurationPatch)
		sr.Post("/webhooks", h.HandleWebhook)
	})

	// ── 10. Start server ─────────────────────────────────────────────
	logger.Info().Str("port", cfg.Port).Msg("telemedicine service started")
	if err := http.ListenAndServe(":"+cfg.Port, r); err != nil {
		logger.Fatal().Err(err).Msg("server stopped")
	}
}
