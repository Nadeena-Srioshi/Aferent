package store

import (
	"context"
	"database/sql"
	"errors"
	"fmt"

	"github.com/aferent/telemedicine-service/internal/model"
	"github.com/jackc/pgx/v5/pgxpool"
)

// AppointmentStore handles the appointments_stub table.
// This is a temporary local table used while appointment-service is not yet integrated.
// Once appointment-service is live, these queries will be replaced with HTTP calls to
// GET /appointments/{id} through the gateway at http://appointment-service:3004.
type AppointmentStore struct {
	DB                        *pgxpool.Pool
	DefaultSessionDurationSec int64  // from config — used when creating a stub appointment
	Mode                      string // "stub" or "remote"
}

// EnsureAppointment verifies that the appointment exists and that the actor is allowed
// to join it. In "stub" mode, it auto-creates appointments on first use — this lets
// the telemedicine service work without appointment-service being deployed.
func (a *AppointmentStore) EnsureAppointment(ctx context.Context, appointmentID string, actor model.Actor) error {
	if a.Mode != "stub" {
		return fmt.Errorf("appointment remote mode not implemented yet")
	}

	// Try to find existing appointment
	var patientID, doctorID string
	err := a.DB.QueryRow(ctx,
		`SELECT patient_id, doctor_id FROM appointments_stub WHERE appointment_id=$1`,
		appointmentID,
	).Scan(&patientID, &doctorID)

	if err != nil {
		if !errors.Is(err, sql.ErrNoRows) {
			return err
		}
		// Appointment doesn't exist — create a stub.
		// The first person to join defines their role in the appointment.
		p := ""
		d := ""
		if actor.Role == "PATIENT" {
			p = actor.UserID
		}
		if actor.Role == "DOCTOR" {
			d = actor.UserID
		}
		_, err = a.DB.Exec(ctx, `
			INSERT INTO appointments_stub (appointment_id, patient_id, doctor_id, scheduled_start, scheduled_duration_seconds)
			VALUES ($1,$2,$3,NOW(),$4)
		`, appointmentID, p, d, a.DefaultSessionDurationSec)
		return err
	}

	// Appointment exists — verify the actor is allowed to join it.
	// A patient can only join their own appointment, same for doctor.
	if actor.Role == "PATIENT" && patientID != "" && patientID != actor.UserID {
		return fmt.Errorf("patient is not owner of appointment")
	}
	if actor.Role == "DOCTOR" && doctorID != "" && doctorID != actor.UserID {
		return fmt.Errorf("doctor is not owner of appointment")
	}

	// If this role's slot is empty, fill it in (second person joining)
	if actor.Role == "PATIENT" && patientID == "" {
		_, _ = a.DB.Exec(ctx,
			`UPDATE appointments_stub SET patient_id=$2, updated_at=NOW() WHERE appointment_id=$1`,
			appointmentID, actor.UserID,
		)
	}
	if actor.Role == "DOCTOR" && doctorID == "" {
		_, _ = a.DB.Exec(ctx,
			`UPDATE appointments_stub SET doctor_id=$2, updated_at=NOW() WHERE appointment_id=$1`,
			appointmentID, actor.UserID,
		)
	}
	return nil
}
