package handler

import (
	"errors"
	"net/http"
	"strings"

	"github.com/go-chi/chi/v5"
	"github.com/jackc/pgx/v5"
)

// HandleStatus returns the current state of a video session.
// The frontend polls this to show whether the doctor/patient has joined,
// how much time is remaining, and the recording status.
//
// Endpoint: GET /sessions/{appointmentId}/status
//
// Response body (SessionStatusResponse):
//
//	{
//	  "appointmentId": "appt-001",
//	  "channelName": "appt_appt-001",
//	  "status": "in_progress",
//	  "startedAt": "2026-04-13T14:30:00Z",
//	  "doctorJoined": true,
//	  "patientJoined": false,
//	  "remainingDurationSec": 1200,
//	  "noShowNotified": false,
//	  "recordingStatus": "none"
//	}
func (h *Handler) HandleStatus(w http.ResponseWriter, r *http.Request) {
	appointmentID := strings.TrimSpace(chi.URLParam(r, "appointmentId"))
	if appointmentID == "" {
		http.Error(w, "appointmentId required", http.StatusBadRequest)
		return
	}

	status, err := h.Sessions.FetchStatus(r.Context(), appointmentID)
	if err != nil {
		if errors.Is(err, pgx.ErrNoRows) {
			http.Error(w, "session not found", http.StatusNotFound)
			return
		}
		http.Error(w, "failed to fetch status", http.StatusInternalServerError)
		return
	}
	writeJSON(w, status)
}

// HandleRemainingDuration returns just the time left in the session.
// This is a lightweight alternative to the full status endpoint,
// useful for the frontend countdown timer.
//
// Endpoint: GET /sessions/{appointmentId}/remaining-duration
//
// Response:
//
//	{
//	  "appointmentId": "appt-001",
//	  "remainingDurationSec": 1200,
//	  "warningThresholdSec": 300
//	}
func (h *Handler) HandleRemainingDuration(w http.ResponseWriter, r *http.Request) {
	appointmentID := strings.TrimSpace(chi.URLParam(r, "appointmentId"))
	if appointmentID == "" {
		http.Error(w, "appointmentId required", http.StatusBadRequest)
		return
	}

	status, err := h.Sessions.FetchStatus(r.Context(), appointmentID)
	if err != nil {
		http.Error(w, "failed to fetch duration", http.StatusInternalServerError)
		return
	}
	writeJSON(w, map[string]interface{}{
		"appointmentId":        appointmentID,
		"remainingDurationSec": status.RemainingDurationSec,
		"warningThresholdSec":  h.Config.WarningThresholdSec,
	})
}
