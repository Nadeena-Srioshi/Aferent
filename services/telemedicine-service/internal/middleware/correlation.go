// Package middleware provides HTTP middleware for cross-cutting concerns.
package middleware

import (
	"context"
	"net/http"
	"strings"

	"github.com/google/uuid"
)

// contextKey is an unexported type to prevent collisions in context values.
// This is a Go best practice — using a plain string as a context key can clash
// with keys from other packages.
type contextKey string

// CorrelationIDKey is the context key for the correlation ID.
const CorrelationIDKey contextKey = "correlationId"

// CorrelationID is a middleware that ensures every request has a unique correlation ID.
// The API gateway generates one and passes it in the X-Correlation-ID header.
// If a request arrives without one (e.g. direct calls), we generate a UUID.
//
// This ID is:
//   - stored in the request context (so handlers can access it)
//   - set on the response header (so the caller can reference it)
//   - included in every log message and event for distributed tracing
func CorrelationID(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		cid := strings.TrimSpace(r.Header.Get("X-Correlation-ID"))
		if cid == "" {
			cid = uuid.NewString()
		}
		w.Header().Set("X-Correlation-ID", cid)
		ctx := context.WithValue(r.Context(), CorrelationIDKey, cid)
		next.ServeHTTP(w, r.WithContext(ctx))
	})
}

// GetCorrelationID extracts the correlation ID from the request context.
// Returns a new UUID if none was set (defensive — should always be present).
func GetCorrelationID(ctx context.Context) string {
	v := ctx.Value(CorrelationIDKey)
	if s, ok := v.(string); ok && s != "" {
		return s
	}
	return uuid.NewString()
}
