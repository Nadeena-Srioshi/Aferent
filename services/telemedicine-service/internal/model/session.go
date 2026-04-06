// Package model defines the data structures used across the telemedicine service.
// These structs map directly to PostgreSQL table rows and JSON API responses.
package model

import "time"

// SessionRow maps to a row in the "sessions" table.
// It tracks the state of a single video call tied to an appointment.
type SessionRow struct {
	AppointmentID            string     // PK — matches an appointment from appointment-service (or appointments_stub)
	ChannelName              string     // Agora channel name, derived from appointment ID
	Status                   string     // "scheduled" | "in_progress" | "ended"
	StartedAt                *time.Time // set when the first participant joins
	EndedAt                  *time.Time // set when session is explicitly ended
	ScheduledStart           time.Time  // the time the appointment was supposed to start
	ScheduledDurationSeconds int64      // default duration from config (e.g. 1800 = 30 min)
	DurationOverrideSeconds  *int64     // if doctor/admin extends the session
	NoShowNotified           bool       // true after no-show event has been emitted
	RecordingStatus          string     // "none" | "ready"
	RecordingObjectKey       *string    // MinIO object key (future use)
}

// SessionStatusResponse is the JSON payload returned by GET /sessions/{id}/status.
type SessionStatusResponse struct {
	AppointmentID        string     `json:"appointmentId"`
	ChannelName          string     `json:"channelName"`
	Status               string     `json:"status"`
	StartedAt            *time.Time `json:"startedAt,omitempty"`
	EndedAt              *time.Time `json:"endedAt,omitempty"`
	DoctorJoined         bool       `json:"doctorJoined"`
	PatientJoined        bool       `json:"patientJoined"`
	RemainingDurationSec int64      `json:"remainingDurationSec"`
	DurationOverrideSec  *int64     `json:"durationOverrideSec,omitempty"`
	NoShowNotified       bool       `json:"noShowNotified"`
	RecordingStatus      string     `json:"recordingStatus"`
	RecordingObjectKey   *string    `json:"recordingObjectKey,omitempty"`
}

// JoinResponse is returned by POST /sessions/join/{appointmentId}.
// It gives the client everything it needs to connect to the Agora video channel.
type JoinResponse struct {
	AppointmentID          string `json:"appointmentId"`
	ChannelName            string `json:"channelName"`
	AppID                  string `json:"appId"`
	Token                  string `json:"token"`
	UID                    string `json:"uid"`
	Role                   string `json:"role"`
	Status                 string `json:"status"`
	OtherParticipantJoined bool   `json:"otherParticipantJoined"`
	ExpiresAt              string `json:"expiresAt"`
}

// DurationPatchRequest is the body of PATCH /sessions/{id}/duration.
// Only doctors and admins can extend or shorten an active session.
type DurationPatchRequest struct {
	DurationSeconds int64  `json:"durationSeconds"` // new total duration in seconds (must be >= 60)
	Reason          string `json:"reason"`          // why the duration was changed
}

// WebhookPayload is the body sent by Agora (or a test client) to POST /sessions/webhooks.
// It notifies the service about participant events and recording readiness.
type WebhookPayload struct {
	Type          string `json:"type"`          // e.g. "participant_joined", "recording_ready"
	AppointmentID string `json:"appointmentId"` // which session this event belongs to
	UserID        string `json:"userId"`
	Role          string `json:"role"`
	RecordingURL  string `json:"recordingUrl"` // set for recording_ready events
	ObjectKey     string `json:"objectKey"`    // MinIO object key for recording
}

// Actor represents the authenticated user making the request.
// Values come from headers injected by the API gateway after JWT validation:
//   - X-User-ID, X-User-Role, X-User-Email
type Actor struct {
	UserID string
	Role   string
	Email  string
}
