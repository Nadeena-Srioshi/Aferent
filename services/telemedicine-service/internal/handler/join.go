package handler

import (
	"context"
	"net/http"
	"strings"
	"time"

	"github.com/aferent/telemedicine-service/internal/middleware"
	"github.com/aferent/telemedicine-service/internal/model"
	"github.com/go-chi/chi/v5"
)

// HandleJoin is the main endpoint for entering a video call.
// Both doctor and patient call the same endpoint — the service figures out
// who they are from the X-User-ID and X-User-Role headers.
//
// What it does step by step:
//  1. Validates that the appointmentId is provided and the actor headers are present
//  2. Ensures the appointment exists (creates a stub in dev mode)
//  3. Ensures a session record exists in Postgres
//  4. Upserts the participant (adds or re-activates them)
//  5. If this is the first join, starts the session (status → in_progress)
//  6. Checks if the other participant (doctor/patient) is already in the call
//  7. If the other person hasn't shown up, checks for no-show threshold
//  8. Generates an Agora video token
//  9. Returns everything the client needs to join the Agora channel
//
// Endpoint: POST /sessions/join/{appointmentId}
//
// Required headers: X-User-ID, X-User-Role, X-User-Email
//
// Response body (JoinResponse):
//
//	{
//	  "appointmentId": "appt-001",
//	  "channelName": "appt_appt-001",
//	  "appId": "47ba4aef...",
//	  "token": "007eJxT...",
//	  "uid": "doctor-001",
//	  "role": "DOCTOR",
//	  "status": "in_progress",
//	  "otherParticipantJoined": false,
//	  "expiresAt": "2026-04-13T15:00:00Z"
//	}
func (h *Handler) HandleJoin(w http.ResponseWriter, r *http.Request) {
	appointmentID := strings.TrimSpace(chi.URLParam(r, "appointmentId"))
	if appointmentID == "" {
		http.Error(w, "appointmentId required", http.StatusBadRequest)
		return
	}

	actor, err := actorFromHeaders(r)
	if err != nil {
		http.Error(w, err.Error(), http.StatusUnauthorized)
		return
	}
	if actor.Role != "DOCTOR" && actor.Role != "PATIENT" && actor.Role != "ADMIN" {
		http.Error(w, "unsupported role", http.StatusForbidden)
		return
	}

	ctx := r.Context()
	cid := middleware.GetCorrelationID(ctx)

	// Step 2: ensure the appointment exists
	if err := h.Appointments.EnsureAppointment(ctx, appointmentID, actor); err != nil {
		http.Error(w, err.Error(), http.StatusBadRequest)
		return
	}

	// Step 3: ensure the session record exists
	session, err := h.Sessions.EnsureSession(ctx, appointmentID)
	if err != nil {
		h.logErr(r, err, "failed to ensure session")
		http.Error(w, "failed to create session", http.StatusInternalServerError)
		return
	}

	// Step 4: add or re-activate the participant
	now := time.Now().UTC()
	if err := h.Participants.Upsert(ctx, appointmentID, actor.UserID, actor.Role, now); err != nil {
		h.logErr(r, err, "failed to upsert participant")
		http.Error(w, "failed to update participant", http.StatusInternalServerError)
		return
	}

	// Step 5: if nobody has joined before, mark the session as started
	if session.StartedAt == nil {
		if err := h.Sessions.StartSession(ctx, appointmentID, now); err != nil {
			h.logErr(r, err, "failed to start session")
		}
		_ = h.Publisher.Emit(ctx, cid, appointmentID, "telemedicine.session.started", map[string]interface{}{
			"startedAt": now.Format(time.RFC3339),
			"joinedBy":  actor.Role,
			"userId":    actor.UserID,
		})
	}

	// Step 6: check if the counterpart is already in the call
	otherRole := "PATIENT"
	if actor.Role == "PATIENT" {
		otherRole = "DOCTOR"
	}
	otherJoined, err := h.Participants.IsRoleActive(ctx, appointmentID, otherRole)
	if err != nil {
		h.logErr(r, err, "failed to check counterpart")
	}

	// Emit participant.joined event
	_ = h.Publisher.Emit(ctx, cid, appointmentID, "telemedicine.participant.joined", map[string]interface{}{
		"role":   actor.Role,
		"userId": actor.UserID,
		"joined": now.Format(time.RFC3339),
	})

	// Step 7: no-show check — if the other person hasn't arrived,
	// check if we've passed the threshold to fire a no-show event
	if !otherJoined {
		if err := h.checkAndEmitNoShow(ctx, cid, appointmentID, otherRole); err != nil {
			h.logErr(r, err, "failed no-show evaluation")
		}
	}

	// Step 8: generate Agora video token
	token, expiresAt, err := h.AgoraTokenBuilder.BuildToken(session.ChannelName, actor.UserID)
	if err != nil {
		h.logErr(r, err, "failed to build agora token")
		http.Error(w, "token generation failed", http.StatusInternalServerError)
		return
	}

	// Step 9: return everything the frontend needs
	writeJSON(w, model.JoinResponse{
		AppointmentID:          appointmentID,
		ChannelName:            session.ChannelName,
		AppID:                  h.Config.AgoraAppID,
		Token:                  token,
		UID:                    actor.UserID,
		Role:                   actor.Role,
		Status:                 "in_progress",
		OtherParticipantJoined: otherJoined,
		ExpiresAt:              expiresAt.Format(time.RFC3339),
	})
}

// checkAndEmitNoShow fires a "telemedicine.no_show_detected" event if enough time
// has passed since the scheduled start and the other participant still hasn't joined.
func (h *Handler) checkAndEmitNoShow(ctx context.Context, correlationID, appointmentID, missingRole string) error {
	scheduledStart, notified, err := h.Sessions.GetScheduleAndNoShow(ctx, appointmentID)
	if err != nil {
		return err
	}
	if notified {
		return nil // already sent
	}

	threshold := scheduledStart.Add(time.Duration(h.Config.NoShowThresholdSec) * time.Second)
	if time.Now().UTC().Before(threshold) {
		return nil // too early to call it a no-show
	}

	if err := h.Sessions.SetNoShowNotified(ctx, appointmentID); err != nil {
		return err
	}
	return h.Publisher.Emit(ctx, correlationID, appointmentID, "telemedicine.no_show_detected", map[string]interface{}{
		"missingRole":   missingRole,
		"thresholdTime": threshold.Format(time.RFC3339),
	})
}

// logErr writes an error to the structured logger with correlation ID and path.
func (h *Handler) logErr(r *http.Request, err error, msg string) {
	cid := middleware.GetCorrelationID(r.Context())
	h.Log.Error().Err(err).Str("correlationId", cid).Str("path", r.URL.Path).Msg(msg)
}
