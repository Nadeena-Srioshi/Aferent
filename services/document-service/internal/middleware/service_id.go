package middleware

import (
	"context"
	"encoding/json"
	"net/http"
	"strings"
)

// contextKey is an unexported type to prevent collisions with other packages.
type contextKey string

const serviceIDKey contextKey = "serviceID"

// RequireServiceID is an HTTP middleware that enforces the presence of the
// X-Internal-Service-ID header. Requests missing this header are rejected
// with 400 before they reach any handler.
func RequireServiceID(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		serviceID := strings.TrimSpace(r.Header.Get("X-Internal-Service-ID"))
		if serviceID == "" {
			writeError(w, http.StatusBadRequest, "X-Internal-Service-ID header is required")
			return
		}

		// Inject the value into context so handlers can read it without
		// touching the header again.
		ctx := context.WithValue(r.Context(), serviceIDKey, serviceID)
		next.ServeHTTP(w, r.WithContext(ctx))
	})
}

// ServiceIDFromContext retrieves the service ID injected by RequireServiceID.
// Returns an empty string if the middleware was not applied.
func ServiceIDFromContext(ctx context.Context) string {
	v, _ := ctx.Value(serviceIDKey).(string)
	return v
}

func writeError(w http.ResponseWriter, status int, msg string) {
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(status)
	json.NewEncoder(w).Encode(map[string]string{"error": msg})
}
