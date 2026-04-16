package handler

import (
	"net/http"
	"strings"
	"time"

	"github.com/aferent/telemedicine-service/internal/middleware"
	"github.com/go-chi/chi/v5"
)

// HandleEnd terminates a video session. Any participant (doctor, patient, or admin)
// can end the session. Once ended:
//   - Session status is set to "ended"
//   - All participants are marked as having left
//   - A "telemedicine.session.ended" event is published to Kafka
//
// The ended event includes the total call duration, which downstream services
// (like payment-service) can use for billing.
//
// Endpoint: POST /sessions/{appointmentId}/end
//
// Required headers: X-User-ID, X-User-Role
//
// Response:
//
//	{
//	  "appointmentId": "appt-001",
//	  "status": "ended",
//	  "durationSeconds": 1234
//	}
func (h *Handler) HandleEnd(w http.ResponseWriter, r *http.Request) {
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
	if act.Role != "DOCTOR" && act.Role != "ADMIN" && act.Role != "PATIENT" {
		http.Error(w, "forbidden", http.StatusForbidden)
		return
	}

	ctx := r.Context()
	cid := middleware.GetCorrelationID(ctx)
	now := time.Now().UTC()

	// Mark session as ended
	if err := h.Sessions.EndSession(ctx, appointmentID, now); err != nil {
		http.Error(w, "failed to end session", http.StatusInternalServerError)
		return
	}

	// Mark all participants as having left
	_ = h.Participants.MarkAllLeft(ctx, appointmentID, now)

	// Get the final session state so we can calculate duration
	status, err := h.Sessions.FetchStatus(ctx, appointmentID)
	if err != nil {
		http.Error(w, "failed to read ended session", http.StatusInternalServerError)
		return
	}

	// Calculate total duration from startedAt to endedAt
	duration := int64(0)
	if status.StartedAt != nil {
		end := now
		if status.EndedAt != nil {
			end = *status.EndedAt
		}
		duration = int64(end.Sub(*status.StartedAt).Seconds())
		if duration < 0 {
			duration = 0
		}
	}

	// Publish the session.ended event — payment-service and notification-service
	// will consume this from Kafka to process billing and send confirmation emails.
	_ = h.Publisher.Emit(ctx, cid, appointmentID, "telemedicine.session.ended", map[string]interface{}{
		"endedBy":            act.UserID,
		"endedByRole":        act.Role,
		"durationSeconds":    duration,
		"recordingStatus":    status.RecordingStatus,
		"recordingObjectKey": status.RecordingObjectKey,
	})

	writeJSON(w, map[string]interface{}{
		"appointmentId":   appointmentID,
		"status":          "ended",
		"durationSeconds": duration,
	})
}
