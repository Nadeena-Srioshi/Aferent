package store

import (
	"context"
	"encoding/json"

	"github.com/aferent/telemedicine-service/internal/model"
	"github.com/jackc/pgx/v5/pgxpool"
)

// EventStore persists events to the session_events table in PostgreSQL.
// This gives us a durable, queryable audit log — even if Kafka is temporarily down,
// the event is safely stored in Postgres.
type EventStore struct {
	DB *pgxpool.Pool
}

// Insert writes a single event to the session_events table.
// The envelope is marshalled to JSON for the JSONB payload column.
func (e *EventStore) Insert(ctx context.Context, envelope model.EventEnvelope) error {
	buf, err := json.Marshal(envelope)
	if err != nil {
		return err
	}
	_, err = e.DB.Exec(ctx, `
		INSERT INTO session_events (event_id, appointment_id, event_type, payload, correlation_id)
		VALUES ($1,$2,$3,$4,$5)
	`, envelope.EventID, envelope.AppointmentID, envelope.EventType, buf, envelope.CorrelationID)
	return err
}
