package main

import (
	"context"
	"crypto/hmac"
	"crypto/sha256"
	"database/sql"
	"encoding/hex"
	"encoding/json"
	"errors"
	"fmt"
	"io"
	"net/http"
	"os"
	"strconv"
	"strings"
	"time"

	rtctokenbuilder2 "github.com/AgoraIO-Community/go-tokenbuilder/rtctokenbuilder2"
	"github.com/go-chi/chi/v5"
	"github.com/google/uuid"
	"github.com/jackc/pgx/v5/pgxpool"
	"github.com/minio/minio-go/v7"
	"github.com/minio/minio-go/v7/pkg/credentials"
	"github.com/prometheus/client_golang/prometheus"
	"github.com/prometheus/client_golang/prometheus/promhttp"
	"github.com/rs/zerolog"
	"github.com/segmentio/kafka-go"
)

type Config struct {
	Port                      string
	DBDSN                     string
	KafkaBootstrapServers     string
	KafkaTopicSessionEvents   string
	AgoraAppID                string
	AgoraAppCertificate       string
	AgoraWebhookSecret        string
	AgoraTokenTTLSec          uint32
	MinioEndpoint             string
	MinioAccessKey            string
	MinioSecretKey            string
	MinioBucket               string
	MinioUseSSL               bool
	AppointmentMode           string
	DefaultSessionDurationSec int64
	ReconnectGraceSec         int64
	WarningThresholdSec       int64
	NoShowThresholdSec        int64
}

type App struct {
	cfg    Config
	log    zerolog.Logger
	db     *pgxpool.Pool
	kafka  *kafka.Writer
	minio  *minio.Client
	reqCtr *prometheus.CounterVec
	latH   *prometheus.HistogramVec
}

type SessionStatusResponse struct {
	AppointmentID        string     `json:"appointmentId"`
	ChannelName          string     `json:"channelName"`
	Status               string     `json:"status"`
	StartedAt            *time.Time `json:"startedAt,omitempty"`
	EndedAt              *time.Time `json:"endedAt,omitempty"`
	DoctorJoined         bool       `json:"doctorJoined"`
	PatientJoined        bool       `json:"patientJoined"`
	RemainingDurationSec int64      `json:"remainingDurationSec"`
	DurationOverrideSec  *int64     `json:"durationOverrideSec,omitempty"`
	NoShowNotified       bool       `json:"noShowNotified"`
	RecordingStatus      string     `json:"recordingStatus"`
	RecordingObjectKey   *string    `json:"recordingObjectKey,omitempty"`
}

type JoinResponse struct {
	AppointmentID          string `json:"appointmentId"`
	ChannelName            string `json:"channelName"`
	AppID                  string `json:"appId"`
	Token                  string `json:"token"`
	UID                    string `json:"uid"`
	Role                   string `json:"role"`
	Status                 string `json:"status"`
	OtherParticipantJoined bool   `json:"otherParticipantJoined"`
	ExpiresAt              string `json:"expiresAt"`
}

type DurationPatchRequest struct {
	DurationSeconds int64  `json:"durationSeconds"`
	Reason          string `json:"reason"`
}

type WebhookPayload struct {
	Type          string `json:"type"`
	AppointmentID string `json:"appointmentId"`
	UserID        string `json:"userId"`
	Role          string `json:"role"`
	RecordingURL  string `json:"recordingUrl"`
	ObjectKey     string `json:"objectKey"`
}

type EventEnvelope struct {
	EventID       string      `json:"eventId"`
	EventType     string      `json:"eventType"`
	Timestamp     string      `json:"timestamp"`
	CorrelationID string      `json:"correlationId"`
	AppointmentID string      `json:"appointmentId"`
	Producer      string      `json:"producer"`
	Version       string      `json:"version"`
	Payload       interface{} `json:"payload"`
}

type actor struct {
	UserID string
	Role   string
	Email  string
}

func main() {
	cfg := loadConfig()
	logger := zerolog.New(os.Stdout).With().Timestamp().Str("service", "telemedicine-service").Logger()

	ctx := context.Background()
	db, err := pgxpool.New(ctx, cfg.DBDSN)
	if err != nil {
		logger.Fatal().Err(err).Msg("failed to create db pool")
	}
	defer db.Close()

	if err := initSchema(ctx, db); err != nil {
		logger.Fatal().Err(err).Msg("failed to init schema")
	}

	minioClient, err := minio.New(cfg.MinioEndpoint, &minio.Options{
		Creds:  credentials.NewStaticV4(cfg.MinioAccessKey, cfg.MinioSecretKey, ""),
		Secure: cfg.MinioUseSSL,
	})
	if err != nil {
		logger.Warn().Err(err).Msg("minio unavailable, continuing without object checks")
	}
	if minioClient != nil {
		exists, err := minioClient.BucketExists(ctx, cfg.MinioBucket)
		if err == nil && !exists {
			err = minioClient.MakeBucket(ctx, cfg.MinioBucket, minio.MakeBucketOptions{})
		}
		if err != nil {
			logger.Warn().Err(err).Msg("failed to ensure minio bucket")
		}
	}

	writer := &kafka.Writer{
		Addr:                   kafka.TCP(cfg.KafkaBootstrapServers),
		Topic:                  cfg.KafkaTopicSessionEvents,
		Balancer:               &kafka.LeastBytes{},
		AllowAutoTopicCreation: true,
		WriteTimeout:           3 * time.Second,
	}
	defer writer.Close()

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

	app := &App{
		cfg:    cfg,
		log:    logger,
		db:     db,
		kafka:  writer,
		minio:  minioClient,
		reqCtr: reqCtr,
		latH:   latency,
	}

	r := chi.NewRouter()
	r.Use(app.correlationMiddleware)
	r.Use(app.metricsMiddleware)

	r.Get("/health", app.handleHealth)
	r.Get("/ready", app.handleHealth)
	r.Get("/actuator/health", app.handleHealth)
	r.Get("/actuator/health/liveness", app.handleLiveness)
	r.Handle("/actuator/prometheus", promhttp.Handler())

	r.Get("/test", app.handleFrontend)
	r.Get("/", func(w http.ResponseWriter, _ *http.Request) {
		w.WriteHeader(http.StatusOK)
		_, _ = w.Write([]byte("telemedicine-service up"))
	})

	r.Route("/sessions", func(sr chi.Router) {
		sr.Post("/join/{appointmentId}", app.handleJoin)
		sr.Get("/{appointmentId}/status", app.handleStatus)
		sr.Post("/{appointmentId}/end", app.handleEnd)
		sr.Get("/history", app.handleHistory)
		sr.Get("/{appointmentId}/remaining-duration", app.handleRemainingDuration)
		sr.Patch("/{appointmentId}/duration", app.handleDurationPatch)
		sr.Post("/webhooks", app.handleWebhook)
	})

	app.log.Info().Str("port", cfg.Port).Msg("telemedicine service started")
	if err := http.ListenAndServe(":"+cfg.Port, r); err != nil {
		app.log.Fatal().Err(err).Msg("server stopped")
	}
}

func loadConfig() Config {
	return Config{
		Port:                      envOr("PORT", "3006"),
		DBDSN:                     envOr("TELEMEDICINE_DB_DSN", "postgres://telemedicine:telemedicine@localhost:5432/telemedicine_db?sslmode=disable"),
		KafkaBootstrapServers:     envOr("KAFKA_BOOTSTRAP_SERVERS", "localhost:9092"),
		KafkaTopicSessionEvents:   envOr("KAFKA_TOPIC_SESSION_EVENTS", "telemedicine.events"),
		AgoraAppID:                envOr("AGORA_APP_ID", ""),
		AgoraAppCertificate:       envOr("AGORA_APP_CERTIFICATE", ""),
		AgoraWebhookSecret:        envOr("AGORA_WEBHOOK_SECRET", ""),
		AgoraTokenTTLSec:          uint32(envInt("AGORA_TOKEN_TTL_SEC", 3600)),
		MinioEndpoint:             envOr("MINIO_ENDPOINT", "localhost:9000"),
		MinioAccessKey:            envOr("MINIO_ACCESS_KEY", "minioadmin"),
		MinioSecretKey:            envOr("MINIO_SECRET_KEY", "minioadmin"),
		MinioBucket:               envOr("MINIO_BUCKET", "telemedicine-recordings"),
		MinioUseSSL:               envOr("MINIO_USE_SSL", "false") == "true",
		AppointmentMode:           strings.ToLower(envOr("APPOINTMENT_MODE", "stub")),
		DefaultSessionDurationSec: int64(envInt("DEFAULT_SESSION_DURATION_SEC", 1800)),
		ReconnectGraceSec:         int64(envInt("RECONNECT_GRACE_SEC", 90)),
		WarningThresholdSec:       int64(envInt("WARNING_THRESHOLD_SEC", 300)),
		NoShowThresholdSec:        int64(envInt("NO_SHOW_THRESHOLD_SEC", 60)),
	}
}

func envOr(key, fallback string) string {
	v := strings.TrimSpace(os.Getenv(key))
	if v == "" {
		return fallback
	}
	return v
}

func envInt(key string, fallback int) int {
	v := strings.TrimSpace(os.Getenv(key))
	if v == "" {
		return fallback
	}
	n, err := strconv.Atoi(v)
	if err != nil {
		return fallback
	}
	return n
}

func initSchema(ctx context.Context, db *pgxpool.Pool) error {
	ddl := []string{
		`CREATE TABLE IF NOT EXISTS appointments_stub (
			appointment_id TEXT PRIMARY KEY,
			patient_id TEXT,
			doctor_id TEXT,
			scheduled_start TIMESTAMPTZ NOT NULL DEFAULT NOW(),
			scheduled_duration_seconds BIGINT NOT NULL DEFAULT 1800,
			created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
			updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
		);`,
		`CREATE TABLE IF NOT EXISTS sessions (
			appointment_id TEXT PRIMARY KEY,
			channel_name TEXT NOT NULL,
			status TEXT NOT NULL DEFAULT 'scheduled',
			started_at TIMESTAMPTZ,
			ended_at TIMESTAMPTZ,
			scheduled_start TIMESTAMPTZ NOT NULL,
			scheduled_duration_seconds BIGINT NOT NULL,
			duration_override_seconds BIGINT,
			no_show_notified BOOLEAN NOT NULL DEFAULT FALSE,
			recording_status TEXT NOT NULL DEFAULT 'none',
			recording_object_key TEXT,
			created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
			updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
		);`,
		`CREATE TABLE IF NOT EXISTS session_participants (
			appointment_id TEXT NOT NULL,
			user_id TEXT NOT NULL,
			role TEXT NOT NULL,
			joined_at TIMESTAMPTZ NOT NULL,
			last_seen_at TIMESTAMPTZ NOT NULL,
			left_at TIMESTAMPTZ,
			PRIMARY KEY (appointment_id, user_id, role)
		);`,
		`CREATE TABLE IF NOT EXISTS session_events (
			event_id UUID PRIMARY KEY,
			appointment_id TEXT NOT NULL,
			event_type TEXT NOT NULL,
			payload JSONB NOT NULL,
			correlation_id TEXT NOT NULL,
			created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
		);`,
		`CREATE INDEX IF NOT EXISTS idx_sessions_status ON sessions(status);`,
		`CREATE INDEX IF NOT EXISTS idx_sessions_started_at ON sessions(started_at);`,
		`CREATE INDEX IF NOT EXISTS idx_session_events_appointment ON session_events(appointment_id, created_at DESC);`,
	}

	for _, q := range ddl {
		if _, err := db.Exec(ctx, q); err != nil {
			return err
		}
	}
	return nil
}

func (a *App) correlationMiddleware(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		cid := strings.TrimSpace(r.Header.Get("X-Correlation-ID"))
		if cid == "" {
			cid = uuid.NewString()
		}
		w.Header().Set("X-Correlation-ID", cid)
		ctx := context.WithValue(r.Context(), "correlationId", cid)
		next.ServeHTTP(w, r.WithContext(ctx))
	})
}

type rwStatus struct {
	http.ResponseWriter
	status int
}

func (w *rwStatus) WriteHeader(code int) {
	w.status = code
	w.ResponseWriter.WriteHeader(code)
}

func (a *App) metricsMiddleware(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		start := time.Now()
		rw := &rwStatus{ResponseWriter: w, status: http.StatusOK}
		next.ServeHTTP(rw, r)
		path := routePattern(r)
		a.reqCtr.WithLabelValues(r.Method, path, strconv.Itoa(rw.status)).Inc()
		a.latH.WithLabelValues(r.Method, path).Observe(time.Since(start).Seconds())
	})
}

func routePattern(r *http.Request) string {
	if rc := chi.RouteContext(r.Context()); rc != nil {
		return rc.RoutePattern()
	}
	return r.URL.Path
}

func (a *App) handleHealth(w http.ResponseWriter, r *http.Request) {
	ctx, cancel := context.WithTimeout(r.Context(), 2*time.Second)
	defer cancel()
	status := "UP"
	dbStatus := "UP"
	if err := a.db.Ping(ctx); err != nil {
		status = "DOWN"
		dbStatus = "DOWN"
	}
	writeJSON(w, map[string]interface{}{
		"status": status,
		"checks": map[string]string{"db": dbStatus},
	})
}

func (a *App) handleLiveness(w http.ResponseWriter, _ *http.Request) {
	writeJSON(w, map[string]string{"status": "UP"})
}

func (a *App) handleFrontend(w http.ResponseWriter, _ *http.Request) {
	f, err := os.ReadFile("/app/web/index.html")
	if err != nil {
		f, err = os.ReadFile("web/index.html")
		if err != nil {
			http.Error(w, "frontend not found", http.StatusInternalServerError)
			return
		}
	}
	w.Header().Set("Content-Type", "text/html; charset=utf-8")
	w.WriteHeader(http.StatusOK)
	_, _ = w.Write(f)
}

func (a *App) handleJoin(w http.ResponseWriter, r *http.Request) {
	appointmentID := strings.TrimSpace(chi.URLParam(r, "appointmentId"))
	if appointmentID == "" {
		http.Error(w, "appointmentId required", http.StatusBadRequest)
		return
	}
	actor, err := actorFromHeaders(r)
	if err != nil {
		http.Error(w, err.Error(), http.StatusUnauthorized)
		return
	}
	if actor.Role != "DOCTOR" && actor.Role != "PATIENT" && actor.Role != "ADMIN" {
		http.Error(w, "unsupported role", http.StatusForbidden)
		return
	}

	ctx := r.Context()
	if err := a.ensureAppointment(ctx, appointmentID, actor); err != nil {
		http.Error(w, err.Error(), http.StatusBadRequest)
		return
	}

	session, err := a.ensureSession(ctx, appointmentID)
	if err != nil {
		a.logErr(r, err, "failed to ensure session")
		http.Error(w, "failed to create session", http.StatusInternalServerError)
		return
	}

	now := time.Now().UTC()
	_, err = a.db.Exec(ctx, `
		INSERT INTO session_participants (appointment_id, user_id, role, joined_at, last_seen_at, left_at)
		VALUES ($1, $2, $3, $4, $4, NULL)
		ON CONFLICT (appointment_id, user_id, role)
		DO UPDATE SET left_at = NULL, last_seen_at = EXCLUDED.last_seen_at;
	`, appointmentID, actor.UserID, actor.Role, now)
	if err != nil {
		a.logErr(r, err, "failed to upsert participant")
		http.Error(w, "failed to update participant", http.StatusInternalServerError)
		return
	}

	if session.StartedAt == nil {
		_, err = a.db.Exec(ctx, `UPDATE sessions SET status='in_progress', started_at=$2, updated_at=NOW() WHERE appointment_id=$1`, appointmentID, now)
		if err != nil {
			a.logErr(r, err, "failed to start session")
		}
		_ = a.emitEvent(ctx, r, appointmentID, "telemedicine.session.started", map[string]interface{}{
			"startedAt": now.Format(time.RFC3339),
			"joinedBy":  actor.Role,
			"userId":    actor.UserID,
		})
	}

	otherRole := "PATIENT"
	if actor.Role == "PATIENT" {
		otherRole = "DOCTOR"
	}
	otherJoined, err := a.isRoleActive(ctx, appointmentID, otherRole)
	if err != nil {
		a.logErr(r, err, "failed to check counterpart")
	}

	_ = a.emitEvent(ctx, r, appointmentID, "telemedicine.participant.joined", map[string]interface{}{
		"role":   actor.Role,
		"userId": actor.UserID,
		"joined": now.Format(time.RFC3339),
	})

	if !otherJoined {
		if err := a.checkAndEmitNoShow(ctx, r, appointmentID, otherRole); err != nil {
			a.logErr(r, err, "failed no-show evaluation")
		}
	}

	token, expiresAt, err := a.buildAgoraToken(session.ChannelName, actor.UserID)
	if err != nil {
		a.logErr(r, err, "failed to build agora token")
		http.Error(w, "token generation failed", http.StatusInternalServerError)
		return
	}

	writeJSON(w, JoinResponse{
		AppointmentID:          appointmentID,
		ChannelName:            session.ChannelName,
		AppID:                  a.cfg.AgoraAppID,
		Token:                  token,
		UID:                    actor.UserID,
		Role:                   actor.Role,
		Status:                 "in_progress",
		OtherParticipantJoined: otherJoined,
		ExpiresAt:              expiresAt.Format(time.RFC3339),
	})
}

func (a *App) handleStatus(w http.ResponseWriter, r *http.Request) {
	appointmentID := strings.TrimSpace(chi.URLParam(r, "appointmentId"))
	if appointmentID == "" {
		http.Error(w, "appointmentId required", http.StatusBadRequest)
		return
	}
	status, err := a.fetchSessionStatus(r.Context(), appointmentID)
	if err != nil {
		if errors.Is(err, sql.ErrNoRows) {
			http.Error(w, "session not found", http.StatusNotFound)
			return
		}
		http.Error(w, "failed to fetch status", http.StatusInternalServerError)
		return
	}
	writeJSON(w, status)
}

func (a *App) handleRemainingDuration(w http.ResponseWriter, r *http.Request) {
	appointmentID := strings.TrimSpace(chi.URLParam(r, "appointmentId"))
	if appointmentID == "" {
		http.Error(w, "appointmentId required", http.StatusBadRequest)
		return
	}
	status, err := a.fetchSessionStatus(r.Context(), appointmentID)
	if err != nil {
		http.Error(w, "failed to fetch duration", http.StatusInternalServerError)
		return
	}
	writeJSON(w, map[string]interface{}{
		"appointmentId":        appointmentID,
		"remainingDurationSec": status.RemainingDurationSec,
		"warningThresholdSec":  a.cfg.WarningThresholdSec,
	})
}

func (a *App) handleDurationPatch(w http.ResponseWriter, r *http.Request) {
	appointmentID := strings.TrimSpace(chi.URLParam(r, "appointmentId"))
	if appointmentID == "" {
		http.Error(w, "appointmentId required", http.StatusBadRequest)
		return
	}
	act, err := actorFromHeaders(r)
	if err != nil {
		http.Error(w, err.Error(), http.StatusUnauthorized)
		return
	}
	if act.Role != "DOCTOR" && act.Role != "ADMIN" {
		http.Error(w, "only doctor/admin can modify duration", http.StatusForbidden)
		return
	}
	var req DurationPatchRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		http.Error(w, "invalid request body", http.StatusBadRequest)
		return
	}
	if req.DurationSeconds < 60 {
		http.Error(w, "durationSeconds must be >= 60", http.StatusBadRequest)
		return
	}
	_, err = a.db.Exec(r.Context(), `
		UPDATE sessions
		SET duration_override_seconds=$2, updated_at=NOW()
		WHERE appointment_id=$1 AND ended_at IS NULL
	`, appointmentID, req.DurationSeconds)
	if err != nil {
		http.Error(w, "failed to update duration", http.StatusInternalServerError)
		return
	}
	_ = a.emitEvent(r.Context(), r, appointmentID, "telemedicine.session.duration_changed", map[string]interface{}{
		"newDurationSeconds": req.DurationSeconds,
		"reason":             req.Reason,
		"actorRole":          act.Role,
		"actorId":            act.UserID,
	})
	writeJSON(w, map[string]interface{}{
		"appointmentId":   appointmentID,
		"durationSeconds": req.DurationSeconds,
		"updatedBy":       act.UserID,
	})
}

func (a *App) handleEnd(w http.ResponseWriter, r *http.Request) {
	appointmentID := strings.TrimSpace(chi.URLParam(r, "appointmentId"))
	if appointmentID == "" {
		http.Error(w, "appointmentId required", http.StatusBadRequest)
		return
	}
	act, err := actorFromHeaders(r)
	if err != nil {
		http.Error(w, err.Error(), http.StatusUnauthorized)
		return
	}
	if act.Role != "DOCTOR" && act.Role != "ADMIN" && act.Role != "PATIENT" {
		http.Error(w, "forbidden", http.StatusForbidden)
		return
	}
	now := time.Now().UTC()
	_, err = a.db.Exec(r.Context(), `
		UPDATE sessions
		SET status='ended', ended_at=COALESCE(ended_at, $2), updated_at=NOW()
		WHERE appointment_id=$1
	`, appointmentID, now)
	if err != nil {
		http.Error(w, "failed to end session", http.StatusInternalServerError)
		return
	}
	_, _ = a.db.Exec(r.Context(), `
		UPDATE session_participants
		SET left_at=COALESCE(left_at, $2), last_seen_at=$2
		WHERE appointment_id=$1
	`, appointmentID, now)

	status, err := a.fetchSessionStatus(r.Context(), appointmentID)
	if err != nil {
		http.Error(w, "failed to read ended session", http.StatusInternalServerError)
		return
	}
	duration := int64(0)
	if status.StartedAt != nil {
		end := now
		if status.EndedAt != nil {
			end = *status.EndedAt
		}
		duration = int64(end.Sub(*status.StartedAt).Seconds())
		if duration < 0 {
			duration = 0
		}
	}
	_ = a.emitEvent(r.Context(), r, appointmentID, "telemedicine.session.ended", map[string]interface{}{
		"endedBy":            act.UserID,
		"endedByRole":        act.Role,
		"durationSeconds":    duration,
		"recordingStatus":    status.RecordingStatus,
		"recordingObjectKey": status.RecordingObjectKey,
	})
	writeJSON(w, map[string]interface{}{
		"appointmentId":   appointmentID,
		"status":          "ended",
		"durationSeconds": duration,
	})
}

func (a *App) handleHistory(w http.ResponseWriter, r *http.Request) {
	ctx := r.Context()
	patientID := strings.TrimSpace(r.URL.Query().Get("patientId"))
	doctorID := strings.TrimSpace(r.URL.Query().Get("doctorId"))
	limit := envInt("_HISTORY_LIMIT_OVERRIDE", 20)
	if v := strings.TrimSpace(r.URL.Query().Get("limit")); v != "" {
		if n, err := strconv.Atoi(v); err == nil {
			limit = n
		}
	}
	if limit <= 0 || limit > 200 {
		limit = 20
	}
	offset := 0
	if v := strings.TrimSpace(r.URL.Query().Get("offset")); v != "" {
		if n, err := strconv.Atoi(v); err == nil {
			offset = n
		}
	}

	rows, err := a.db.Query(ctx, `
		SELECT s.appointment_id, s.channel_name, s.status, s.started_at, s.ended_at,
		       s.scheduled_duration_seconds, s.duration_override_seconds, s.recording_status, s.recording_object_key,
		       ap.patient_id, ap.doctor_id
		FROM sessions s
		JOIN appointments_stub ap ON ap.appointment_id = s.appointment_id
		WHERE ($1 = '' OR ap.patient_id = $1)
		  AND ($2 = '' OR ap.doctor_id = $2)
		ORDER BY COALESCE(s.started_at, s.created_at) DESC
		LIMIT $3 OFFSET $4
	`, patientID, doctorID, limit, offset)
	if err != nil {
		http.Error(w, "failed to query history", http.StatusInternalServerError)
		return
	}
	defer rows.Close()

	items := make([]map[string]interface{}, 0)
	for rows.Next() {
		var appointmentID, channel, status, recordingStatus, pID, dID string
		var startedAt, endedAt sql.NullTime
		var scheduledDur int64
		var override sql.NullInt64
		var recKey sql.NullString
		if err := rows.Scan(&appointmentID, &channel, &status, &startedAt, &endedAt, &scheduledDur, &override, &recordingStatus, &recKey, &pID, &dID); err != nil {
			http.Error(w, "failed to scan history", http.StatusInternalServerError)
			return
		}
		duration := int64(0)
		if startedAt.Valid {
			endTime := time.Now().UTC()
			if endedAt.Valid {
				endTime = endedAt.Time
			}
			duration = int64(endTime.Sub(startedAt.Time).Seconds())
			if duration < 0 {
				duration = 0
			}
		}
		item := map[string]interface{}{
			"appointmentId":      appointmentID,
			"channelName":        channel,
			"status":             status,
			"patientId":          pID,
			"doctorId":           dID,
			"startedAt":          nullableTime(startedAt),
			"endedAt":            nullableTime(endedAt),
			"durationSeconds":    duration,
			"recordingStatus":    recordingStatus,
			"recordingObjectKey": nullableString(recKey),
		}
		if override.Valid {
			item["durationOverrideSeconds"] = override.Int64
		}
		items = append(items, item)
	}

	writeJSON(w, map[string]interface{}{
		"items":  items,
		"limit":  limit,
		"offset": offset,
	})
}

func (a *App) handleWebhook(w http.ResponseWriter, r *http.Request) {
	body, err := io.ReadAll(r.Body)
	if err != nil {
		http.Error(w, "invalid body", http.StatusBadRequest)
		return
	}
	if a.cfg.AgoraWebhookSecret != "" {
		sig := strings.TrimSpace(r.Header.Get("X-Webhook-Signature"))
		if !verifyHMAC(body, a.cfg.AgoraWebhookSecret, sig) {
			http.Error(w, "invalid webhook signature", http.StatusUnauthorized)
			return
		}
	}
	var payload WebhookPayload
	if err := json.Unmarshal(body, &payload); err != nil {
		http.Error(w, "invalid json", http.StatusBadRequest)
		return
	}
	payload.Type = strings.ToLower(strings.TrimSpace(payload.Type))
	if payload.AppointmentID == "" {
		http.Error(w, "appointmentId required", http.StatusBadRequest)
		return
	}

	ctx := r.Context()
	now := time.Now().UTC()
	switch payload.Type {
	case "participant_left", "user-left", "participant.left":
		_, err := a.db.Exec(ctx, `
			UPDATE session_participants
			SET left_at=$4, last_seen_at=$4
			WHERE appointment_id=$1 AND user_id=$2 AND role=$3
		`, payload.AppointmentID, payload.UserID, strings.ToUpper(payload.Role), now)
		if err != nil {
			http.Error(w, "failed to update participant leave", http.StatusInternalServerError)
			return
		}
		_ = a.emitEvent(ctx, r, payload.AppointmentID, "telemedicine.participant.left", map[string]interface{}{
			"userId": payload.UserID,
			"role":   strings.ToUpper(payload.Role),
			"leftAt": now.Format(time.RFC3339),
		})
	case "participant_joined", "user-joined", "participant.joined":
		_, err := a.db.Exec(ctx, `
			INSERT INTO session_participants (appointment_id, user_id, role, joined_at, last_seen_at, left_at)
			VALUES ($1,$2,$3,$4,$4,NULL)
			ON CONFLICT (appointment_id, user_id, role)
			DO UPDATE SET left_at=NULL, last_seen_at=$4;
		`, payload.AppointmentID, payload.UserID, strings.ToUpper(payload.Role), now)
		if err != nil {
			http.Error(w, "failed to update participant join", http.StatusInternalServerError)
			return
		}
		_ = a.emitEvent(ctx, r, payload.AppointmentID, "telemedicine.participant.joined", map[string]interface{}{
			"userId": payload.UserID,
			"role":   strings.ToUpper(payload.Role),
			"joined": now.Format(time.RFC3339),
		})
	case "recording_ready", "recording-ready", "recording.ready":
		objKey := payload.ObjectKey
		if objKey == "" {
			objKey = payload.RecordingURL
		}
		_, err := a.db.Exec(ctx, `
			UPDATE sessions
			SET recording_status='ready', recording_object_key=$2, updated_at=NOW()
			WHERE appointment_id=$1
		`, payload.AppointmentID, objKey)
		if err != nil {
			http.Error(w, "failed to update recording", http.StatusInternalServerError)
			return
		}
		_ = a.emitEvent(ctx, r, payload.AppointmentID, "telemedicine.recording.ready", map[string]interface{}{
			"recordingUrl": payload.RecordingURL,
			"objectKey":    objKey,
		})
	default:
		_ = a.emitEvent(ctx, r, payload.AppointmentID, "telemedicine.webhook.unknown", map[string]interface{}{
			"type": payload.Type,
		})
	}
	writeJSON(w, map[string]string{"status": "ok"})
}

func (a *App) ensureAppointment(ctx context.Context, appointmentID string, ac actor) error {
	if a.cfg.AppointmentMode != "stub" {
		return fmt.Errorf("appointment remote mode not implemented yet")
	}
	var patientID, doctorID string
	err := a.db.QueryRow(ctx, `SELECT patient_id, doctor_id FROM appointments_stub WHERE appointment_id=$1`, appointmentID).Scan(&patientID, &doctorID)
	if err != nil {
		if !errors.Is(err, sql.ErrNoRows) {
			return err
		}
		p := ""
		d := ""
		if ac.Role == "PATIENT" {
			p = ac.UserID
		}
		if ac.Role == "DOCTOR" {
			d = ac.UserID
		}
		_, err = a.db.Exec(ctx, `
			INSERT INTO appointments_stub (appointment_id, patient_id, doctor_id, scheduled_start, scheduled_duration_seconds)
			VALUES ($1,$2,$3,NOW(),$4)
		`, appointmentID, p, d, a.cfg.DefaultSessionDurationSec)
		return err
	}
	if ac.Role == "PATIENT" && patientID != "" && patientID != ac.UserID {
		return fmt.Errorf("patient is not owner of appointment")
	}
	if ac.Role == "DOCTOR" && doctorID != "" && doctorID != ac.UserID {
		return fmt.Errorf("doctor is not owner of appointment")
	}
	if ac.Role == "PATIENT" && patientID == "" {
		_, _ = a.db.Exec(ctx, `UPDATE appointments_stub SET patient_id=$2, updated_at=NOW() WHERE appointment_id=$1`, appointmentID, ac.UserID)
	}
	if ac.Role == "DOCTOR" && doctorID == "" {
		_, _ = a.db.Exec(ctx, `UPDATE appointments_stub SET doctor_id=$2, updated_at=NOW() WHERE appointment_id=$1`, appointmentID, ac.UserID)
	}
	return nil
}

type sessionRow struct {
	AppointmentID            string
	ChannelName              string
	Status                   string
	StartedAt                *time.Time
	EndedAt                  *time.Time
	ScheduledStart           time.Time
	ScheduledDurationSeconds int64
	DurationOverrideSeconds  *int64
	NoShowNotified           bool
	RecordingStatus          string
	RecordingObjectKey       *string
}

func (a *App) ensureSession(ctx context.Context, appointmentID string) (*sessionRow, error) {
	s := &sessionRow{}
	err := a.db.QueryRow(ctx, `
		SELECT appointment_id, channel_name, status, started_at, ended_at, scheduled_start,
		       scheduled_duration_seconds, duration_override_seconds, no_show_notified,
		       recording_status, recording_object_key
		FROM sessions WHERE appointment_id=$1
	`, appointmentID).Scan(&s.AppointmentID, &s.ChannelName, &s.Status, &s.StartedAt, &s.EndedAt, &s.ScheduledStart,
		&s.ScheduledDurationSeconds, &s.DurationOverrideSeconds, &s.NoShowNotified, &s.RecordingStatus, &s.RecordingObjectKey)
	if err == nil {
		return s, nil
	}
	if !errors.Is(err, sql.ErrNoRows) {
		return nil, err
	}

	var scheduledStart time.Time
	var scheduledDuration int64
	if err := a.db.QueryRow(ctx, `SELECT scheduled_start, scheduled_duration_seconds FROM appointments_stub WHERE appointment_id=$1`, appointmentID).
		Scan(&scheduledStart, &scheduledDuration); err != nil {
		return nil, err
	}
	channelName := fmt.Sprintf("appt_%s", sanitizeForChannel(appointmentID))
	_, err = a.db.Exec(ctx, `
		INSERT INTO sessions (appointment_id, channel_name, status, scheduled_start, scheduled_duration_seconds)
		VALUES ($1,$2,'scheduled',$3,$4)
	`, appointmentID, channelName, scheduledStart, scheduledDuration)
	if err != nil {
		return nil, err
	}
	return a.ensureSession(ctx, appointmentID)
}

func sanitizeForChannel(in string) string {
	in = strings.TrimSpace(in)
	in = strings.ReplaceAll(in, " ", "_")
	in = strings.ReplaceAll(in, "/", "_")
	if in == "" {
		return uuid.NewString()
	}
	return in
}

func (a *App) fetchSessionStatus(ctx context.Context, appointmentID string) (*SessionStatusResponse, error) {
	s, err := a.ensureSession(ctx, appointmentID)
	if err != nil {
		return nil, err
	}
	var doctorJoined, patientJoined bool
	if err := a.db.QueryRow(ctx, `
		SELECT
			EXISTS(SELECT 1 FROM session_participants WHERE appointment_id=$1 AND role='DOCTOR' AND left_at IS NULL),
			EXISTS(SELECT 1 FROM session_participants WHERE appointment_id=$1 AND role='PATIENT' AND left_at IS NULL)
	`, appointmentID).Scan(&doctorJoined, &patientJoined); err != nil {
		return nil, err
	}

	dur := s.ScheduledDurationSeconds
	if s.DurationOverrideSeconds != nil {
		dur = *s.DurationOverrideSeconds
	}
	remaining := dur
	if s.StartedAt != nil {
		end := time.Now().UTC()
		if s.EndedAt != nil {
			end = *s.EndedAt
		}
		elapsed := int64(end.Sub(*s.StartedAt).Seconds())
		remaining = dur - elapsed
		if remaining < 0 {
			remaining = 0
		}
	}

	return &SessionStatusResponse{
		AppointmentID:        appointmentID,
		ChannelName:          s.ChannelName,
		Status:               s.Status,
		StartedAt:            s.StartedAt,
		EndedAt:              s.EndedAt,
		DoctorJoined:         doctorJoined,
		PatientJoined:        patientJoined,
		RemainingDurationSec: remaining,
		DurationOverrideSec:  s.DurationOverrideSeconds,
		NoShowNotified:       s.NoShowNotified,
		RecordingStatus:      s.RecordingStatus,
		RecordingObjectKey:   s.RecordingObjectKey,
	}, nil
}

func (a *App) isRoleActive(ctx context.Context, appointmentID, role string) (bool, error) {
	var active bool
	err := a.db.QueryRow(ctx, `
		SELECT EXISTS(SELECT 1 FROM session_participants WHERE appointment_id=$1 AND role=$2 AND left_at IS NULL)
	`, appointmentID, role).Scan(&active)
	return active, err
}

func (a *App) checkAndEmitNoShow(ctx context.Context, r *http.Request, appointmentID, missingRole string) error {
	var scheduledStart time.Time
	var notified bool
	err := a.db.QueryRow(ctx, `SELECT scheduled_start, no_show_notified FROM sessions WHERE appointment_id=$1`, appointmentID).
		Scan(&scheduledStart, &notified)
	if err != nil {
		return err
	}
	if notified {
		return nil
	}
	threshold := scheduledStart.Add(time.Duration(a.cfg.NoShowThresholdSec) * time.Second)
	if time.Now().UTC().Before(threshold) {
		return nil
	}
	_, err = a.db.Exec(ctx, `UPDATE sessions SET no_show_notified=TRUE, updated_at=NOW() WHERE appointment_id=$1`, appointmentID)
	if err != nil {
		return err
	}
	return a.emitEvent(ctx, r, appointmentID, "telemedicine.no_show_detected", map[string]interface{}{
		"missingRole":   missingRole,
		"thresholdTime": threshold.Format(time.RFC3339),
	})
}

func (a *App) buildAgoraToken(channelName, account string) (string, time.Time, error) {
	expiresAt := time.Now().UTC().Add(time.Duration(a.cfg.AgoraTokenTTLSec) * time.Second)
	if strings.TrimSpace(a.cfg.AgoraAppID) == "" || strings.TrimSpace(a.cfg.AgoraAppCertificate) == "" {
		// Dev fallback lets API flow be testable before Agora secrets are provided.
		fallback := fmt.Sprintf("dev-%s-%d", uuid.NewString(), expiresAt.Unix())
		return fallback, expiresAt, nil
	}
	token, err := rtctokenbuilder2.BuildTokenWithUserAccount(
		a.cfg.AgoraAppID,
		a.cfg.AgoraAppCertificate,
		channelName,
		account,
		rtctokenbuilder2.RolePublisher,
		a.cfg.AgoraTokenTTLSec,
		a.cfg.AgoraTokenTTLSec,
	)
	if err != nil {
		return "", time.Time{}, err
	}
	return token, expiresAt, nil
}

func (a *App) emitEvent(ctx context.Context, r *http.Request, appointmentID, eventType string, payload interface{}) error {
	cid := correlationIDFromContext(ctx)
	envelope := EventEnvelope{
		EventID:       uuid.NewString(),
		EventType:     eventType,
		Timestamp:     time.Now().UTC().Format(time.RFC3339),
		CorrelationID: cid,
		AppointmentID: appointmentID,
		Producer:      "telemedicine-service",
		Version:       "v1",
		Payload:       payload,
	}
	buf, err := json.Marshal(envelope)
	if err != nil {
		return err
	}
	_, err = a.db.Exec(ctx, `
		INSERT INTO session_events (event_id, appointment_id, event_type, payload, correlation_id)
		VALUES ($1,$2,$3,$4,$5)
	`, envelope.EventID, appointmentID, eventType, buf, cid)
	if err != nil {
		return err
	}

	if err := a.kafka.WriteMessages(ctx, kafka.Message{Key: []byte(appointmentID), Value: buf}); err != nil {
		a.log.Warn().Err(err).Str("eventType", eventType).Str("appointmentId", appointmentID).Msg("kafka publish failed")
		return nil
	}
	a.log.Info().Str("eventType", eventType).Str("appointmentId", appointmentID).Str("correlationId", cid).Msg("event published")
	return nil
}

func actorFromHeaders(r *http.Request) (actor, error) {
	userID := strings.TrimSpace(r.Header.Get("X-User-ID"))
	role := strings.ToUpper(strings.TrimSpace(r.Header.Get("X-User-Role")))
	email := strings.TrimSpace(r.Header.Get("X-User-Email"))
	if userID == "" || role == "" {
		return actor{}, fmt.Errorf("missing X-User-ID or X-User-Role")
	}
	return actor{UserID: userID, Role: role, Email: email}, nil
}

func correlationIDFromContext(ctx context.Context) string {
	v := ctx.Value("correlationId")
	if s, ok := v.(string); ok && s != "" {
		return s
	}
	return uuid.NewString()
}

func (a *App) logErr(r *http.Request, err error, msg string) {
	cid := correlationIDFromContext(r.Context())
	a.log.Error().Err(err).Str("correlationId", cid).Str("path", r.URL.Path).Msg(msg)
}

func verifyHMAC(body []byte, secret, providedSig string) bool {
	providedSig = strings.TrimSpace(providedSig)
	if providedSig == "" {
		return false
	}
	mac := hmac.New(sha256.New, []byte(secret))
	mac.Write(body)
	expected := hex.EncodeToString(mac.Sum(nil))
	return hmac.Equal([]byte(strings.ToLower(expected)), []byte(strings.ToLower(providedSig)))
}

func nullableTime(v sql.NullTime) interface{} {
	if !v.Valid {
		return nil
	}
	return v.Time.Format(time.RFC3339)
}

func nullableString(v sql.NullString) interface{} {
	if !v.Valid {
		return nil
	}
	return v.String
}

func writeJSON(w http.ResponseWriter, v interface{}) {
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(http.StatusOK)
	_ = json.NewEncoder(w).Encode(v)
}
