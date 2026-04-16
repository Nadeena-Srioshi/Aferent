package handler

import (
	"encoding/json"
	"net/http"
	"strings"

	"github.com/aferent/telemedicine-service/internal/middleware"
	"github.com/aferent/telemedicine-service/internal/model"
	"github.com/go-chi/chi/v5"
)

// HandleDurationPatch lets a doctor or admin change the session duration while
// the call is still active. Useful when a consultation needs more time.
//
// Only roles DOCTOR and ADMIN are allowed — a patient cannot extend their own session.
// The new duration must be at least 60 seconds.
//
// Endpoint: PATCH /sessions/{appointmentId}/duration
//
// Required headers: X-User-ID, X-User-Role
//
// Request body:
//
//	{
//	  "durationSeconds": 3600,
//	  "reason": "complex case, need more time"
//	}
//
// Response:
//
//	{
//	  "appointmentId": "appt-001",
//	  "durationSeconds": 3600,
//	  "updatedBy": "doctor-001"
//	}
func (h *Handler) HandleDurationPatch(w http.ResponseWriter, r *http.Request) {
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

	var req model.DurationPatchRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		http.Error(w, "invalid request body", http.StatusBadRequest)
		return
	}
	if req.DurationSeconds < 60 {
		http.Error(w, "durationSeconds must be >= 60", http.StatusBadRequest)
		return
	}

	ctx := r.Context()
	cid := middleware.GetCorrelationID(ctx)

	if err := h.Sessions.UpdateDuration(ctx, appointmentID, req.DurationSeconds); err != nil {
		http.Error(w, "failed to update duration", http.StatusInternalServerError)
		return
	}

	_ = h.Publisher.Emit(ctx, cid, appointmentID, "telemedicine.session.duration_changed", map[string]interface{}{
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
