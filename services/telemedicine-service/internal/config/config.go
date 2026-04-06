// Package config loads all environment-based configuration for the telemedicine service.
// Every setting has a sensible default so the service can start locally without an .env file.
package config

import (
	"os"
	"strconv"
	"strings"
)

// Config holds every tuneable the service needs.
// Values are read from environment variables at startup via Load().
type Config struct {
	// Port the HTTP server listens on (default "3006").
	Port string

	// DBDSN is the PostgreSQL connection string.
	// Example: postgres://telemedicine:telemedicine@localhost:5432/telemedicine_db?sslmode=disable
	DBDSN string

	// Kafka
	KafkaBootstrapServers   string // comma-separated broker addresses
	KafkaTopicSessionEvents string // topic name for telemedicine events

	// Agora video platform credentials
	AgoraAppID          string
	AgoraAppCertificate string
	AgoraWebhookSecret  string
	AgoraTokenTTLSec    uint32 // token time-to-live in seconds

	// Appointment mode: "stub" creates appointments locally in Postgres,
	// "remote" would call the appointment-service (not yet implemented).
	AppointmentMode string

	// Session timing
	DefaultSessionDurationSec int64 // default video call length (seconds)
	ReconnectGraceSec         int64 // grace period before marking disconnect
	WarningThresholdSec       int64 // remaining seconds when a warning is emitted
	NoShowThresholdSec        int64 // seconds past schedule before no-show event fires
}

// Load reads configuration from environment variables and applies defaults.
func Load() Config {
	return Config{
		Port:                      envOr("PORT", "3006"),
		DBDSN:                     envOr("TELEMEDICINE_DB_DSN", "postgres://telemedicine:telemedicine@localhost:5432/telemedicine_db?sslmode=disable"),
		KafkaBootstrapServers:     envOr("KAFKA_BOOTSTRAP_SERVERS", "localhost:9092"),
		KafkaTopicSessionEvents:   envOr("KAFKA_TOPIC_SESSION_EVENTS", "telemedicine.events"),
		AgoraAppID:                envOr("AGORA_APP_ID", ""),
		AgoraAppCertificate:       envOr("AGORA_APP_CERTIFICATE", ""),
		AgoraWebhookSecret:        envOr("AGORA_WEBHOOK_SECRET", ""),
		AgoraTokenTTLSec:          uint32(envInt("AGORA_TOKEN_TTL_SEC", 3600)),
		AppointmentMode:           strings.ToLower(envOr("APPOINTMENT_MODE", "stub")),
		DefaultSessionDurationSec: int64(envInt("DEFAULT_SESSION_DURATION_SEC", 1800)),
		ReconnectGraceSec:         int64(envInt("RECONNECT_GRACE_SEC", 90)),
		WarningThresholdSec:       int64(envInt("WARNING_THRESHOLD_SEC", 300)),
		NoShowThresholdSec:        int64(envInt("NO_SHOW_THRESHOLD_SEC", 60)),
	}
}

// envOr returns the trimmed value of an environment variable or the fallback.
func envOr(key, fallback string) string {
	v := strings.TrimSpace(os.Getenv(key))
	if v == "" {
		return fallback
	}
	return v
}

// envInt returns the integer value of an environment variable or the fallback.
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
