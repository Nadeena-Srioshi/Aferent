package handler

import (
	"context"
	"database/sql"
	"net/http"
	"strconv"
	"strings"
	"time"

	"github.com/jackc/pgx/v5/pgxpool"
)

// HandleHistory returns a paginated list of past video sessions.
// Can be filtered by patientId and/or doctorId query parameters.
// Results are ordered by most recent first.
//
// Endpoint: GET /sessions/history?patientId=...&doctorId=...&limit=20&offset=0
//
// Query parameters (all optional):
//   - patientId: filter by patient
//   - doctorId:  filter by doctor
//   - limit:     max results (default 20, max 200)
//   - offset:    pagination offset
//
// Response:
//
//	{
//	  "items": [
//	    {
//	      "appointmentId": "appt-001",
//	      "channelName": "appt_appt-001",
//	      "status": "ended",
//	      "patientId": "patient-001",
//	      "doctorId": "doctor-001",
//	      "startedAt": "2026-04-13T14:30:00Z",
//	      "endedAt": "2026-04-13T15:00:00Z",
//	      "durationSeconds": 1800,
//	      "recordingStatus": "none"
//	    }
//	  ],
//	  "limit": 20,
//	  "offset": 0
//	}
func (h *Handler) HandleHistory(w http.ResponseWriter, r *http.Request) {
	ctx := r.Context()
	patientID := strings.TrimSpace(r.URL.Query().Get("patientId"))
	doctorID := strings.TrimSpace(r.URL.Query().Get("doctorId"))

	limit := 20
	if v := strings.TrimSpace(r.URL.Query().Get("limit")); v != "" {
		if n, err := strconv.Atoi(v); err == nil {
			limit = n
		}
	}
	if limit <= 0 || limit > 200 {
		limit = 20
	}

	offset := 0
	if v := strings.TrimSpace(r.URL.Query().Get("offset")); v != "" {
		if n, err := strconv.Atoi(v); err == nil {
			offset = n
		}
	}

	var items []map[string]interface{}
	var err error
	if h.Config.AppointmentMode == "stub" {
		items, err = h.historyStub(ctx, patientID, doctorID, limit, offset)
	} else {
		items, err = h.historyRemote(ctx, limit, offset)
	}
	if err != nil {
		http.Error(w, "failed to query history", http.StatusInternalServerError)
		return
	}

	writeJSON(w, map[string]interface{}{
		"items":  items,
		"limit":  limit,
		"offset": offset,
	})
}

// historyStub queries sessions joined with appointments_stub (development mode).
func (h *Handler) historyStub(ctx context.Context, patientID, doctorID string, limit, offset int) ([]map[string]interface{}, error) {
	rows, err := h.Sessions.DB.Query(ctx, `
		SELECT s.appointment_id, s.channel_name, s.status, s.started_at, s.ended_at,
		       s.scheduled_duration_seconds, s.duration_override_seconds,
		       s.recording_status, s.recording_object_key,
		       ap.patient_id, ap.doctor_id
		FROM sessions s
		JOIN appointments_stub ap ON ap.appointment_id = s.appointment_id
		WHERE ($1 = '' OR ap.patient_id = $1)
		  AND ($2 = '' OR ap.doctor_id = $2)
		ORDER BY COALESCE(s.started_at, s.created_at) DESC
		LIMIT $3 OFFSET $4
	`, patientID, doctorID, limit, offset)
	if err != nil {
		return nil, err
	}
	defer rows.Close()

	items := make([]map[string]interface{}, 0)
	for rows.Next() {
		var appointmentID, channel, status, recordingStatus, pID, dID string
		var startedAt, endedAt sql.NullTime
		var scheduledDur int64
		var override sql.NullInt64
		var recKey sql.NullString

		if err := rows.Scan(
			&appointmentID, &channel, &status, &startedAt, &endedAt,
			&scheduledDur, &override, &recordingStatus, &recKey, &pID, &dID,
		); err != nil {
			return nil, err
		}

		duration := calcDuration(startedAt, endedAt)
		item := map[string]interface{}{
			"appointmentId":      appointmentID,
			"channelName":        channel,
			"status":             status,
			"patientId":          pID,
			"doctorId":           dID,
			"startedAt":          nullableTime(startedAt),
			"endedAt":            nullableTime(endedAt),
			"durationSeconds":    duration,
			"recordingStatus":    recordingStatus,
			"recordingObjectKey": nullableString(recKey),
		}
		if override.Valid {
			item["durationOverrideSeconds"] = override.Int64
		}
		items = append(items, item)
	}
	return items, nil
}

// historyRemote queries sessions directly without appointments_stub (remote mode).
func (h *Handler) historyRemote(ctx context.Context, limit, offset int) ([]map[string]interface{}, error) {
	rows, err := h.Sessions.DB.Query(ctx, `
		SELECT appointment_id, channel_name, status, started_at, ended_at,
		       scheduled_duration_seconds, duration_override_seconds,
		       recording_status, recording_object_key
		FROM sessions
		ORDER BY COALESCE(started_at, created_at) DESC
		LIMIT $1 OFFSET $2
	`, limit, offset)
	if err != nil {
		return nil, err
	}
	defer rows.Close()

	items := make([]map[string]interface{}, 0)
	for rows.Next() {
		var appointmentID, channel, status, recordingStatus string
		var startedAt, endedAt sql.NullTime
		var scheduledDur int64
		var override sql.NullInt64
		var recKey sql.NullString

		if err := rows.Scan(
			&appointmentID, &channel, &status, &startedAt, &endedAt,
			&scheduledDur, &override, &recordingStatus, &recKey,
		); err != nil {
			return nil, err
		}

		duration := calcDuration(startedAt, endedAt)
		item := map[string]interface{}{
			"appointmentId":      appointmentID,
			"channelName":        channel,
			"status":             status,
			"startedAt":          nullableTime(startedAt),
			"endedAt":            nullableTime(endedAt),
			"durationSeconds":    duration,
			"recordingStatus":    recordingStatus,
			"recordingObjectKey": nullableString(recKey),
		}
		if override.Valid {
			item["durationOverrideSeconds"] = override.Int64
		}
		items = append(items, item)
	}
	return items, nil
}

func calcDuration(startedAt, endedAt sql.NullTime) int64 {
	if !startedAt.Valid {
		return 0
	}
	endTime := time.Now().UTC()
	if endedAt.Valid {
		endTime = endedAt.Time
	}
	d := int64(endTime.Sub(startedAt.Time).Seconds())
	if d < 0 {
		return 0
	}
	return d
}

// HistoryDB returns the DB pool from the handler, used for the history query.
// This is a helper to keep the handler's DB accessible without exporting more than needed.
func (h *Handler) HistoryDB() *pgxpool.Pool {
	return h.Sessions.DB
}

// nullableTime converts a sql.NullTime to an RFC3339 string or nil.
func nullableTime(v sql.NullTime) interface{} {
	if !v.Valid {
		return nil
	}
	return v.Time.Format(time.RFC3339)
}

// nullableString converts a sql.NullString to its value or nil.
func nullableString(v sql.NullString) interface{} {
	if !v.Valid {
		return nil
	}
	return v.String
}
