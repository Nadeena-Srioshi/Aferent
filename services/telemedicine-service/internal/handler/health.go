package handler

import (
	"context"
	"net/http"
	"time"
)

// handleHealth checks if the service and its database are reachable.
// Returns {"status":"UP","checks":{"db":"UP"}} when everything is healthy.
// The gateway and Kubernetes use this endpoint to decide whether to route traffic here.
//
// Endpoints:
//   - GET /health
//   - GET /ready
//   - GET /actuator/health
func (h *Handler) HandleHealth(w http.ResponseWriter, r *http.Request) {
	ctx, cancel := context.WithTimeout(r.Context(), 2*time.Second)
	defer cancel()

	status := "UP"
	dbStatus := "UP"
	if err := h.Sessions.DB.Ping(ctx); err != nil {
		status = "DOWN"
		dbStatus = "DOWN"
	}
	writeJSON(w, map[string]interface{}{
		"status": status,
		"checks": map[string]string{"db": dbStatus},
	})
}

// HandleLiveness is a lightweight check that just confirms the process is running.
// Unlike health, it doesn't check the database — it's meant for Kubernetes liveness probes.
//
// Endpoint: GET /actuator/health/liveness
func (h *Handler) HandleLiveness(w http.ResponseWriter, _ *http.Request) {
	writeJSON(w, map[string]string{"status": "UP"})
}
