package model

// EventEnvelope is the standard wrapper for all events published to Kafka
// and persisted in the session_events table. Every event in the platform follows
// this structure so consumers can parse them uniformly.
//
// The correlationId field links this event back to the original HTTP request
// that caused it — this is how you trace a single user action across all services
// in Grafana/Loki logs.
type EventEnvelope struct {
	EventID       string      `json:"eventId"`       // UUID — unique per event
	EventType     string      `json:"eventType"`     // e.g. "telemedicine.session.started"
	Timestamp     string      `json:"timestamp"`     // RFC3339 formatted time
	CorrelationID string      `json:"correlationId"` // ties to the original request
	AppointmentID string      `json:"appointmentId"` // which appointment this relates to
	Producer      string      `json:"producer"`      // always "telemedicine-service"
	Version       string      `json:"version"`       // schema version, currently "v1"
	Payload       interface{} `json:"payload"`       // event-specific data (free-form map)
}
