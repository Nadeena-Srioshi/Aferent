package handler

import (
	"net/http"
	"strconv"
	"strings"
	"time"

	"document-service/internal/model"
	"document-service/internal/storage"
)

const (
	defaultExpiry = 3600      // 1 hour in seconds
	maxExpiry     = 7 * 86400 // 7 days in seconds
)

// SignPrivate handles GET /sign?key=<objectKey>&expires=<seconds>
//
// The caller (another microservice) passes the object_key it stored when the
// file was first uploaded. This handler returns a short-lived presigned GET URL.
type SignPrivate struct {
	store *storage.Client
}

func NewSignPrivate(store *storage.Client) *SignPrivate {
	return &SignPrivate{store: store}
}

func (h *SignPrivate) ServeHTTP(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodGet {
		writeJSON(w, http.StatusMethodNotAllowed, errorBody("method not allowed"))
		return
	}

	objectKey := strings.TrimSpace(r.URL.Query().Get("key"))
	if objectKey == "" {
		writeJSON(w, http.StatusBadRequest, errorBody("query parameter 'key' is required"))
		return
	}

	expiresIn := defaultExpiry
	if raw := r.URL.Query().Get("expires"); raw != "" {
		v, err := strconv.Atoi(raw)
		if err != nil || v <= 0 {
			writeJSON(w, http.StatusBadRequest, errorBody("'expires' must be a positive integer (seconds)"))
			return
		}
		if v > maxExpiry {
			writeJSON(w, http.StatusBadRequest, errorBody("'expires' cannot exceed 604800 seconds (7 days)"))
			return
		}
		expiresIn = v
	}

	ttl := time.Duration(expiresIn) * time.Second
	signedURL, err := h.store.PresignGet(r.Context(), objectKey, ttl)
	if err != nil {
		writeJSON(w, http.StatusInternalServerError, errorBody("could not generate signed URL"))
		return
	}

	writeJSON(w, http.StatusOK, model.SignedURLResponse{URL: signedURL})
}
