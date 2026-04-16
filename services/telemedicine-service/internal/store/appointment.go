package store

import (
	"context"
	"encoding/json"
	"errors"
	"fmt"
	"io"
	"net/http"
	"strings"
	"time"

	"github.com/aferent/telemedicine-service/internal/model"
	"github.com/jackc/pgx/v5"
	"github.com/jackc/pgx/v5/pgxpool"
)

// AppointmentStore handles appointment validation.
// In "stub" mode it uses a local appointments_stub table.
// In "remote" mode it calls the real appointment-service HTTP API.
type AppointmentStore struct {
	DB                        *pgxpool.Pool
	DefaultSessionDurationSec int64  // from config — used when creating a stub appointment
	Mode                      string // "stub" or "remote"
	AppointmentServiceURL     string // base URL of appointment-service (remote mode)
}

// appointmentAPIResponse mirrors the JSON returned by GET /appointments/{id}.
// VideoSlotStart/End are RawMessage because Jackson 3.x serializes LocalTime as
// an array [14,0,0] by default, but may also produce a string "14:00:00".
type appointmentAPIResponse struct {
	ID              string          `json:"id"`
	PatientID       string          `json:"patientId"`
	DoctorID        string          `json:"doctorId"`
	DoctorAuthID    string          `json:"doctorAuthId"`
	Type            string          `json:"type"`
	Status          string          `json:"status"`
	AppointmentDate string          `json:"appointmentDate"` // "2025-04-07"
	VideoSlotStart  json.RawMessage `json:"videoSlotStart"`  // "14:30:00" or [14,30,0]
	VideoSlotEnd    json.RawMessage `json:"videoSlotEnd"`    // "15:00:00" or [15,0,0]
	ConsultationFee float64         `json:"consultationFee"`
}

// parseTimeField parses a LocalTime value from Jackson which may be either:
//   - a JSON string: "14:30:00" or "14:30"
//   - a JSON array:  [14, 30, 0] or [14, 30]
func parseTimeField(raw json.RawMessage) (time.Time, error) {
	if len(raw) == 0 {
		return time.Time{}, fmt.Errorf("empty time field")
	}
	// String form
	if raw[0] == '"' {
		var s string
		if err := json.Unmarshal(raw, &s); err != nil {
			return time.Time{}, err
		}
		for _, layout := range []string{"15:04:05", "15:04"} {
			if t, err := time.Parse(layout, s); err == nil {
				return t, nil
			}
		}
		return time.Time{}, fmt.Errorf("unrecognised time string: %s", s)
	}
	// Array form [h, m, s?, nano?]
	if raw[0] == '[' {
		var parts []int
		if err := json.Unmarshal(raw, &parts); err != nil {
			return time.Time{}, err
		}
		if len(parts) < 2 {
			return time.Time{}, fmt.Errorf("time array too short: %v", parts)
		}
		h, m := parts[0], parts[1]
		s := 0
		if len(parts) >= 3 {
			s = parts[2]
		}
		return time.Date(0, 1, 1, h, m, s, 0, time.UTC), nil
	}
	return time.Time{}, fmt.Errorf("unrecognised time format: %s", string(raw))
}

// EnsureAppointment verifies the appointment exists and the actor is allowed to join.
// Returns AppointmentInfo with scheduling data needed by EnsureSession.
func (a *AppointmentStore) EnsureAppointment(ctx context.Context, appointmentID string, actor model.Actor) (*model.AppointmentInfo, error) {
	if a.Mode == "remote" {
		return a.ensureRemote(ctx, appointmentID, actor)
	}
	return a.ensureStub(ctx, appointmentID, actor)
}

// ensureRemote validates the appointment against the real appointment-service.
func (a *AppointmentStore) ensureRemote(ctx context.Context, appointmentID string, actor model.Actor) (*model.AppointmentInfo, error) {
	url := strings.TrimRight(a.AppointmentServiceURL, "/") + "/appointments/" + appointmentID

	req, err := http.NewRequestWithContext(ctx, http.MethodGet, url, nil)
	if err != nil {
		return nil, fmt.Errorf("failed to build appointment request: %w", err)
	}
	// Forward identity headers — appointment-service uses these for authorization
	req.Header.Set("X-User-ID", actor.UserID)
	req.Header.Set("X-User-Role", actor.Role)

	resp, err := http.DefaultClient.Do(req)
	if err != nil {
		return nil, fmt.Errorf("failed to reach appointment-service: %w", err)
	}
	defer resp.Body.Close()

	if resp.StatusCode == http.StatusNotFound {
		return nil, fmt.Errorf("appointment %s not found", appointmentID)
	}
	if resp.StatusCode != http.StatusOK {
		body, _ := io.ReadAll(resp.Body)
		return nil, fmt.Errorf("appointment-service returned %d: %s", resp.StatusCode, string(body))
	}

	var appt appointmentAPIResponse
	if err := json.NewDecoder(resp.Body).Decode(&appt); err != nil {
		return nil, fmt.Errorf("failed to decode appointment response: %w", err)
	}

	// Validate appointment type
	if !strings.EqualFold(appt.Type, "VIDEO") {
		return nil, fmt.Errorf("appointment %s is not a VIDEO appointment (type=%s)", appointmentID, appt.Type)
	}

	// Validate appointment status — only CONFIRMED appointments can start a video call
	if !strings.EqualFold(appt.Status, "CONFIRMED") {
		return nil, fmt.Errorf("appointment %s is not CONFIRMED (status=%s)", appointmentID, appt.Status)
	}

	// Validate the actor is part of this appointment
	if actor.Role == "PATIENT" && appt.PatientID != actor.UserID {
		return nil, fmt.Errorf("patient is not owner of appointment")
	}
	if actor.Role == "DOCTOR" && appt.DoctorID != actor.UserID && appt.DoctorAuthID != actor.UserID {
		return nil, fmt.Errorf("doctor is not owner of appointment")
	}

	// Parse scheduling info
	scheduledStart, scheduledDuration := a.parseSchedule(appt)

	return &model.AppointmentInfo{
		AppointmentID:     appointmentID,
		PatientID:         appt.PatientID,
		DoctorID:          appt.DoctorID,
		ScheduledStart:    scheduledStart,
		ScheduledDuration: scheduledDuration,
		Status:            appt.Status,
		Type:              appt.Type,
	}, nil
}

// parseSchedule extracts the scheduled start time and duration from the appointment response.
func (a *AppointmentStore) parseSchedule(appt appointmentAPIResponse) (time.Time, int64) {
	now := time.Now().UTC()

	// Try to parse appointmentDate + videoSlotStart
	if appt.AppointmentDate != "" && len(appt.VideoSlotStart) > 0 {
		date, dateErr := time.Parse("2006-01-02", appt.AppointmentDate)
		slotStart, timeErr := parseTimeField(appt.VideoSlotStart)

		if dateErr == nil && timeErr == nil {
			scheduledStart := time.Date(
				date.Year(), date.Month(), date.Day(),
				slotStart.Hour(), slotStart.Minute(), slotStart.Second(),
				0, time.UTC,
			)

			// Calculate duration from slot times
			duration := a.DefaultSessionDurationSec
			if len(appt.VideoSlotEnd) > 0 {
				if slotEnd, err := parseTimeField(appt.VideoSlotEnd); err == nil {
					dur := time.Date(
						date.Year(), date.Month(), date.Day(),
						slotEnd.Hour(), slotEnd.Minute(), slotEnd.Second(),
						0, time.UTC,
					).Sub(scheduledStart)
					if dur.Seconds() > 0 {
						duration = int64(dur.Seconds())
					}
				}
			}
			return scheduledStart, duration
		}
	}

	// Fallback — use current time and default duration
	return now, a.DefaultSessionDurationSec
}

// ensureStub uses the local appointments_stub table (development/testing mode).
func (a *AppointmentStore) ensureStub(ctx context.Context, appointmentID string, actor model.Actor) (*model.AppointmentInfo, error) {
	var patientID, doctorID string
	var scheduledStart time.Time
	var scheduledDuration int64
	err := a.DB.QueryRow(ctx,
		`SELECT patient_id, doctor_id, scheduled_start, scheduled_duration_seconds FROM appointments_stub WHERE appointment_id=$1`,
		appointmentID,
	).Scan(&patientID, &doctorID, &scheduledStart, &scheduledDuration)

	if err != nil {
		if !errors.Is(err, pgx.ErrNoRows) {
			return nil, err
		}
		// Appointment doesn't exist — create a stub.
		p := ""
		d := ""
		if actor.Role == "PATIENT" {
			p = actor.UserID
		}
		if actor.Role == "DOCTOR" {
			d = actor.UserID
		}
		now := time.Now().UTC()
		_, err = a.DB.Exec(ctx, `
			INSERT INTO appointments_stub (appointment_id, patient_id, doctor_id, scheduled_start, scheduled_duration_seconds)
			VALUES ($1,$2,$3,NOW(),$4)
		`, appointmentID, p, d, a.DefaultSessionDurationSec)
		if err != nil {
			return nil, err
		}
		return &model.AppointmentInfo{
			AppointmentID:     appointmentID,
			PatientID:         p,
			DoctorID:          d,
			ScheduledStart:    now,
			ScheduledDuration: a.DefaultSessionDurationSec,
			Status:            "CONFIRMED",
			Type:              "VIDEO",
		}, nil
	}

	// Validate the actor is allowed to join
	if actor.Role == "PATIENT" && patientID != "" && patientID != actor.UserID {
		return nil, fmt.Errorf("patient is not owner of appointment")
	}
	if actor.Role == "DOCTOR" && doctorID != "" && doctorID != actor.UserID {
		return nil, fmt.Errorf("doctor is not owner of appointment")
	}

	// Fill in empty role slots
	if actor.Role == "PATIENT" && patientID == "" {
		_, _ = a.DB.Exec(ctx,
			`UPDATE appointments_stub SET patient_id=$2, updated_at=NOW() WHERE appointment_id=$1`,
			appointmentID, actor.UserID,
		)
		patientID = actor.UserID
	}
	if actor.Role == "DOCTOR" && doctorID == "" {
		_, _ = a.DB.Exec(ctx,
			`UPDATE appointments_stub SET doctor_id=$2, updated_at=NOW() WHERE appointment_id=$1`,
			appointmentID, actor.UserID,
		)
		doctorID = actor.UserID
	}

	return &model.AppointmentInfo{
		AppointmentID:     appointmentID,
		PatientID:         patientID,
		DoctorID:          doctorID,
		ScheduledStart:    scheduledStart,
		ScheduledDuration: scheduledDuration,
		Status:            "CONFIRMED",
		Type:              "VIDEO",
	}, nil
}
