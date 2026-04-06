package config

import (
	"fmt"
	"os"
)

// Config holds all values injected via environment variables.
// No defaults are assumed for credentials — the service refuses to start
// if any required value is missing.
type Config struct {
	MinioEndpoint   string
	MinioAccessKey  string
	MinioSecretKey  string
	MinioUseSSL     bool
	MinioBucket     string
	MinioPublicHost string // e.g. http://localhost:9000  (used to build permanent_url)
	ServerPort      string
}

func Load() (*Config, error) {
	cfg := &Config{
		MinioEndpoint:   requireEnv("MINIO_ENDPOINT"),
		MinioAccessKey:  requireEnv("MINIO_ACCESS_KEY"),
		MinioSecretKey:  requireEnv("MINIO_SECRET_KEY"),
		MinioBucket:     getEnvOrDefault("MINIO_BUCKET", "app-storage"),
		MinioPublicHost: requireEnv("MINIO_PUBLIC_HOST"),
		ServerPort:      getEnvOrDefault("SERVER_PORT", "8080"),
		MinioUseSSL:     os.Getenv("MINIO_USE_SSL") == "true",
	}
	return cfg, nil
}

func requireEnv(key string) string {
	v := os.Getenv(key)
	if v == "" {
		// Panic early with a clear message so the operator knows exactly what's missing.
		panic(fmt.Sprintf("required environment variable %q is not set", key))
	}
	return v
}

func getEnvOrDefault(key, fallback string) string {
	if v := os.Getenv(key); v != "" {
		return v
	}
	return fallback
}
