// Package event handles publishing events to both PostgreSQL and Kafka.
// This "dual-write" pattern ensures events are never lost:
//   - PostgreSQL insert is the primary write (durable, queryable)
//   - Kafka publish is the secondary write (enables async consumers like notification-service)
//
// If Kafka is temporarily down, the event is still safely in Postgres.
// A future enhancement could add a retry/outbox pattern for failed Kafka writes.
package event

import (
	"context"
	"encoding/json"
	"time"

	"github.com/aferent/telemedicine-service/internal/model"
	"github.com/aferent/telemedicine-service/internal/store"
	"github.com/google/uuid"
	"github.com/rs/zerolog"
	"github.com/segmentio/kafka-go"
)

// Publisher emits structured events to Kafka and persists them in Postgres.
type Publisher struct {
	EventStore *store.EventStore
	Kafka      *kafka.Writer
	Log        zerolog.Logger
}

// Emit creates an EventEnvelope, writes it to the session_events table,
// then publishes it to the Kafka topic. The Kafka key is the appointmentID
// so all events for one session end up in the same partition (ordered).
func (p *Publisher) Emit(ctx context.Context, correlationID, appointmentID, eventType string, payload interface{}) error {
	envelope := model.EventEnvelope{
		EventID:       uuid.NewString(),
		EventType:     eventType,
		Timestamp:     time.Now().UTC().Format(time.RFC3339),
		CorrelationID: correlationID,
		AppointmentID: appointmentID,
		Producer:      "telemedicine-service",
		Version:       "v1",
		Payload:       payload,
	}

	// Step 1: persist to Postgres (the durable store)
	if err := p.EventStore.Insert(ctx, envelope); err != nil {
		return err
	}

	// Step 2: publish to Kafka (for other services to consume)
	buf, err := json.Marshal(envelope)
	if err != nil {
		return err
	}
	if err := p.Kafka.WriteMessages(ctx, kafka.Message{
		Key:   []byte(appointmentID),
		Value: buf,
	}); err != nil {
		// Kafka failure is non-fatal — the event is already in Postgres.
		// Log a warning so we can investigate, but don't fail the request.
		p.Log.Warn().Err(err).
			Str("eventType", eventType).
			Str("appointmentId", appointmentID).
			Msg("kafka publish failed — event is safe in postgres")
		return nil
	}

	p.Log.Info().
		Str("eventType", eventType).
		Str("appointmentId", appointmentID).
		Str("correlationId", correlationID).
		Msg("event published")
	return nil
}
