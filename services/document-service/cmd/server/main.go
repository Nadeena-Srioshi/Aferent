package main

import (
	"fmt"
	"log/slog"
	"net/http"
	"os"

	"document-service/internal/config"
	"document-service/internal/handler"
	"document-service/internal/middleware"
	"document-service/internal/storage"
)

func main() {
	// Structured logging — slog is part of the stdlib since Go 1.21.
	logger := slog.New(slog.NewJSONHandler(os.Stdout, nil))
	slog.SetDefault(logger)

	cfg, err := config.Load()
	if err != nil {
		slog.Error("failed to load config", "error", err)
		os.Exit(1)
	}

	store, err := storage.NewClient(cfg)
	if err != nil {
		slog.Error("failed to connect to MinIO", "error", err)
		os.Exit(1)
	}
	slog.Info("MinIO client ready", "bucket", cfg.MinioBucket)

	mux := http.NewServeMux()

	// All routes require the service-ID header.
	mux.Handle("/upload/presign", middleware.RequireServiceID(handler.NewPresignUpload(store)))
	mux.Handle("/sign", middleware.RequireServiceID(handler.NewSignPrivate(store)))

	// Health check — no auth required, used by Docker / load balancers.
	mux.HandleFunc("/health", func(w http.ResponseWriter, r *http.Request) {
		w.WriteHeader(http.StatusOK)
		fmt.Fprintln(w, `{"status":"ok"}`)
	})

	addr := ":" + cfg.ServerPort
	slog.Info("document-service listening", "addr", addr)

	if err := http.ListenAndServe(addr, mux); err != nil {
		slog.Error("server stopped", "error", err)
		os.Exit(1)
	}
}
