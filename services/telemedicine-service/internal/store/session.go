package store

import (
	"context"
	"database/sql"
	"errors"
	"fmt"
	"strings"
	"time"

	"github.com/aferent/telemedicine-service/internal/model"
	"github.com/google/uuid"
	"github.com/jackc/pgx/v5/pgxpool"
)

// SessionStore provides all database operations for the sessions table.
type SessionStore struct {
	DB *pgxpool.Pool
}

// EnsureSession fetches an existing session or creates a new one for the appointment.
// It uses the provided AppointmentInfo for scheduling data when creating a new session.
func (s *SessionStore) EnsureSession(ctx context.Context, appointmentID string, info *model.AppointmentInfo) (*model.SessionRow, error) {
	row := &model.SessionRow{}
	err := s.DB.QueryRow(ctx, `
		SELECT appointment_id, channel_name, status, started_at, ended_at, scheduled_start,
		       scheduled_duration_seconds, duration_override_seconds, no_show_notified,
		       recording_status, recording_object_key
		FROM sessions WHERE appointment_id=$1
	`, appointmentID).Scan(
		&row.AppointmentID, &row.ChannelName, &row.Status, &row.StartedAt, &row.EndedAt,
		&row.ScheduledStart, &row.ScheduledDurationSeconds, &row.DurationOverrideSeconds,
		&row.NoShowNotified, &row.RecordingStatus, &row.RecordingObjectKey,
	)
	if err == nil {
		return row, nil
	}
	if !errors.Is(err, sql.ErrNoRows) {
		return nil, err
	}

	// Session doesn't exist yet — create one from the appointment info
	scheduledStart := info.ScheduledStart
	scheduledDuration := info.ScheduledDuration

	// Channel name is derived from the appointment ID.
	// Agora uses this as the "room name" for the video call.
	channelName := fmt.Sprintf("appt_%s", sanitizeForChannel(appointmentID))
	_, err = s.DB.Exec(ctx, `
		INSERT INTO sessions (appointment_id, channel_name, status, scheduled_start, scheduled_duration_seconds)
		VALUES ($1,$2,'scheduled',$3,$4)
	`, appointmentID, channelName, scheduledStart, scheduledDuration)
	if err != nil {
		return nil, err
	}

	// Re-read to get the full row with defaults applied by Postgres
	return s.EnsureSession(ctx, appointmentID, info)
}

// StartSession transitions the session to "in_progress" and records the start time.
// Called when the first participant joins.
func (s *SessionStore) StartSession(ctx context.Context, appointmentID string, now time.Time) error {
	_, err := s.DB.Exec(ctx,
		`UPDATE sessions SET status='in_progress', started_at=$2, updated_at=NOW() WHERE appointment_id=$1`,
		appointmentID, now,
	)
	return err
}

// EndSession marks the session as "ended". Uses COALESCE so it won't overwrite
// an already-set ended_at (idempotent — safe to call twice).
func (s *SessionStore) EndSession(ctx context.Context, appointmentID string, now time.Time) error {
	_, err := s.DB.Exec(ctx, `
		UPDATE sessions
		SET status='ended', ended_at=COALESCE(ended_at, $2), updated_at=NOW()
		WHERE appointment_id=$1
	`, appointmentID, now)
	return err
}

// UpdateDuration sets a new duration override on an active session.
// Only applies to sessions that haven't ended yet (ended_at IS NULL).
func (s *SessionStore) UpdateDuration(ctx context.Context, appointmentID string, durationSeconds int64) error {
	_, err := s.DB.Exec(ctx, `
		UPDATE sessions
		SET duration_override_seconds=$2, updated_at=NOW()
		WHERE appointment_id=$1 AND ended_at IS NULL
	`, appointmentID, durationSeconds)
	return err
}

// SetNoShowNotified marks that the no-show event has been emitted for this session.
func (s *SessionStore) SetNoShowNotified(ctx context.Context, appointmentID string) error {
	_, err := s.DB.Exec(ctx,
		`UPDATE sessions SET no_show_notified=TRUE, updated_at=NOW() WHERE appointment_id=$1`,
		appointmentID,
	)
	return err
}

// GetScheduleAndNoShow returns the scheduled start time and no_show_notified flag for a session.
func (s *SessionStore) GetScheduleAndNoShow(ctx context.Context, appointmentID string) (time.Time, bool, error) {
	var scheduledStart time.Time
	var notified bool
	err := s.DB.QueryRow(ctx,
		`SELECT scheduled_start, no_show_notified FROM sessions WHERE appointment_id=$1`,
		appointmentID,
	).Scan(&scheduledStart, &notified)
	return scheduledStart, notified, err
}

// UpdateRecording stores the recording status and object key when Agora sends a recording_ready webhook.
func (s *SessionStore) UpdateRecording(ctx context.Context, appointmentID, objectKey string) error {
	_, err := s.DB.Exec(ctx, `
		UPDATE sessions
		SET recording_status='ready', recording_object_key=$2, updated_at=NOW()
		WHERE appointment_id=$1
	`, appointmentID, objectKey)
	return err
}

// FetchStatus builds a full SessionStatusResponse by joining session data with participant presence.
// This is used by both the status endpoint and internally after ending a session.
func (s *SessionStore) FetchStatus(ctx context.Context, appointmentID string) (*model.SessionStatusResponse, error) {
	session := &model.SessionRow{}
	err := s.DB.QueryRow(ctx, `
		SELECT appointment_id, channel_name, status, started_at, ended_at, scheduled_start,
		       scheduled_duration_seconds, duration_override_seconds, no_show_notified,
		       recording_status, recording_object_key
		FROM sessions WHERE appointment_id=$1
	`, appointmentID).Scan(
		&session.AppointmentID, &session.ChannelName, &session.Status, &session.StartedAt, &session.EndedAt,
		&session.ScheduledStart, &session.ScheduledDurationSeconds, &session.DurationOverrideSeconds,
		&session.NoShowNotified, &session.RecordingStatus, &session.RecordingObjectKey,
	)
	if err != nil {
		return nil, err
	}

	// Check if doctor and patient are currently in the call (left_at IS NULL = still active)
	var doctorJoined, patientJoined bool
	if err := s.DB.QueryRow(ctx, `
		SELECT
			EXISTS(SELECT 1 FROM session_participants WHERE appointment_id=$1 AND role='DOCTOR' AND left_at IS NULL),
			EXISTS(SELECT 1 FROM session_participants WHERE appointment_id=$1 AND role='PATIENT' AND left_at IS NULL)
	`, appointmentID).Scan(&doctorJoined, &patientJoined); err != nil {
		return nil, err
	}

	// Calculate remaining duration
	dur := session.ScheduledDurationSeconds
	if session.DurationOverrideSeconds != nil {
		dur = *session.DurationOverrideSeconds
	}
	remaining := dur
	if session.StartedAt != nil {
		end := time.Now().UTC()
		if session.EndedAt != nil {
			end = *session.EndedAt
		}
		elapsed := int64(end.Sub(*session.StartedAt).Seconds())
		remaining = dur - elapsed
		if remaining < 0 {
			remaining = 0
		}
	}

	return &model.SessionStatusResponse{
		AppointmentID:        appointmentID,
		ChannelName:          session.ChannelName,
		Status:               session.Status,
		StartedAt:            session.StartedAt,
		EndedAt:              session.EndedAt,
		DoctorJoined:         doctorJoined,
		PatientJoined:        patientJoined,
		RemainingDurationSec: remaining,
		DurationOverrideSec:  session.DurationOverrideSeconds,
		NoShowNotified:       session.NoShowNotified,
		RecordingStatus:      session.RecordingStatus,
		RecordingObjectKey:   session.RecordingObjectKey,
	}, nil
}

// sanitizeForChannel cleans an appointment ID so it can be used as an Agora channel name.
// Agora channel names can't contain spaces or slashes.
func sanitizeForChannel(in string) string {
	in = strings.TrimSpace(in)
	in = strings.ReplaceAll(in, " ", "_")
	in = strings.ReplaceAll(in, "/", "_")
	if in == "" {
		return uuid.NewString()
	}
	return in
}
