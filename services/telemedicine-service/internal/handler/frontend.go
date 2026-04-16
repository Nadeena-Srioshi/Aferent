package handler

import (
	"net/http"
	"os"
)

// HandleFrontend serves the test HTML page at /test.
// This is a simple vanilla JS page that lets you test the video call flow
// from a browser without needing a full frontend build.
//
// In the Docker container, the file is at /app/web/index.html.
// During local development, it falls back to ./web/index.html.
//
// Endpoint: GET /test
func (h *Handler) HandleFrontend(w http.ResponseWriter, _ *http.Request) {
	// Try the Docker container path first, then the local dev path
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
