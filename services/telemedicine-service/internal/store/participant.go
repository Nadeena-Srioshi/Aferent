package store

import (
	"context"
	"time"

	"github.com/jackc/pgx/v5/pgxpool"
)

// ParticipantStore handles database operations for session_participants.
type ParticipantStore struct {
	DB *pgxpool.Pool
}

// Upsert adds a participant to the session or updates their status if they're re-joining.
// ON CONFLICT means: if this person already has a row, clear their left_at (they're back)
// and update last_seen_at. This handles the reconnect scenario.
func (p *ParticipantStore) Upsert(ctx context.Context, appointmentID, userID, role string, now time.Time) error {
	_, err := p.DB.Exec(ctx, `
		INSERT INTO session_participants (appointment_id, user_id, role, joined_at, last_seen_at, left_at)
		VALUES ($1, $2, $3, $4, $4, NULL)
		ON CONFLICT (appointment_id, user_id, role)
		DO UPDATE SET left_at = NULL, last_seen_at = EXCLUDED.last_seen_at;
	`, appointmentID, userID, role, now)
	return err
}

// MarkLeft sets the left_at timestamp for a participant.
// Called when a "participant_left" webhook comes from Agora.
func (p *ParticipantStore) MarkLeft(ctx context.Context, appointmentID, userID, role string, now time.Time) error {
	_, err := p.DB.Exec(ctx, `
		UPDATE session_participants
		SET left_at=$4, last_seen_at=$4
		WHERE appointment_id=$1 AND user_id=$2 AND role=$3
	`, appointmentID, userID, role, now)
	return err
}

// MarkAllLeft sets left_at for all participants in a session.
// Called when the session is explicitly ended.
func (p *ParticipantStore) MarkAllLeft(ctx context.Context, appointmentID string, now time.Time) error {
	_, err := p.DB.Exec(ctx, `
		UPDATE session_participants
		SET left_at=COALESCE(left_at, $2), last_seen_at=$2
		WHERE appointment_id=$1
	`, appointmentID, now)
	return err
}

// IsRoleActive checks if a participant with the given role is currently in the call
// (has joined but not left). Used to determine if the "other" person has joined.
func (p *ParticipantStore) IsRoleActive(ctx context.Context, appointmentID, role string) (bool, error) {
	var active bool
	err := p.DB.QueryRow(ctx, `
		SELECT EXISTS(
			SELECT 1 FROM session_participants
			WHERE appointment_id=$1 AND role=$2 AND left_at IS NULL
		)
	`, appointmentID, role).Scan(&active)
	return active, err
}
