// Package handler contains all HTTP endpoint handlers for the telemedicine service.
// Each file in this package corresponds to one or more related endpoints.
//
// Handler is the shared struct that all endpoint functions are methods on.
// It holds references to stores, the event publisher, Agora token builder,
// config, and logger — everything a handler needs to process a request.
package handler

import (
	"encoding/json"
	"fmt"
	"net/http"
	"strings"

	"github.com/aferent/telemedicine-service/internal/agora"
	"github.com/aferent/telemedicine-service/internal/config"
	"github.com/aferent/telemedicine-service/internal/event"
	"github.com/aferent/telemedicine-service/internal/model"
	"github.com/aferent/telemedicine-service/internal/store"
	"github.com/rs/zerolog"
)

// Handler groups all dependencies needed by the HTTP endpoint functions.
// In Go, there's no Spring-style dependency injection — we pass dependencies
// explicitly through this struct. Each endpoint is a method on Handler.
type Handler struct {
	Config            config.Config
	Log               zerolog.Logger
	Sessions          *store.SessionStore
	Participants      *store.ParticipantStore
	Appointments      *store.AppointmentStore
	Events            *store.EventStore
	Publisher         *event.Publisher
	AgoraTokenBuilder *agora.TokenBuilder
}

// writeJSON is a helper that sends a JSON response with a 200 OK status.
// Used by every handler to return data to the client.
func writeJSON(w http.ResponseWriter, v interface{}) {
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(http.StatusOK)
	_ = json.NewEncoder(w).Encode(v)
}

// actorFromHeaders extracts the authenticated user info from request headers.
// The API gateway validates the JWT and injects these headers:
//   - X-User-ID:    the user's unique ID (from JWT subject claim)
//   - X-User-Role:  "PATIENT", "DOCTOR", or "ADMIN" (from JWT role claim)
//   - X-User-Email: the user's email address (from JWT email claim)
//
// If these headers are missing, the request wasn't routed through the gateway
// (or JWT validation failed), so we reject it.
func actorFromHeaders(r *http.Request) (model.Actor, error) {
	userID := strings.TrimSpace(r.Header.Get("X-User-ID"))
	role := strings.ToUpper(strings.TrimSpace(r.Header.Get("X-User-Role")))
	email := strings.TrimSpace(r.Header.Get("X-User-Email"))
	if userID == "" || role == "" {
		return model.Actor{}, fmt.Errorf("missing X-User-ID or X-User-Role")
	}
	return model.Actor{UserID: userID, Role: role, Email: email}, nil
}
