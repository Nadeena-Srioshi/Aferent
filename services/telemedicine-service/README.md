# Telemedicine Service

Real-time video consultation service for the Aferent healthcare platform. Built with Go, it handles Agora video sessions, participant tracking, session lifecycle events, and an append-only audit log — all persisted in PostgreSQL and streamed to Kafka.

---

## Table of Contents

1. [Architecture Overview](#architecture-overview)
2. [Project Structure](#project-structure)
3. [Build & Run](#build--run)
4. [Configuration (Environment Variables)](#configuration-environment-variables)
5. [API Reference](#api-reference)
6. [Kafka Events](#kafka-events)
7. [Database Schema](#database-schema)
8. [Integration with Other Services](#integration-with-other-services)
9. [Testing Workflows](#testing-workflows)
10. [Verifying Data in PostgreSQL (DBeaver)](#verifying-data-in-postgresql-dbeaver)
11. [Compiled Binary Location](#compiled-binary-location)
12. [Docker](#docker)

---

## Architecture Overview

```
Browser / Mobile
       │
       ▼
  API Gateway (Spring Boot, port 8080)
       │  routes /sessions/** → telemedicine-service:3006
       │  JWT-exempt: /sessions/webhooks
       ▼
  Telemedicine Service (Go, port 3006)
       │         │
       ▼         ▼
  PostgreSQL    Kafka
  (port 5434)   (port 9092)
```

The API Gateway forwards all `/sessions/**` requests to this service. JWT validation happens at the gateway; headers `X-User-ID`, `X-User-Role`, and `X-User-Email` are set by the gateway after token validation. The webhook endpoint (`POST /sessions/webhooks`) is exempt from JWT checks because Agora calls it directly.

---

## Project Structure

```
services/telemedicine-service/
├── cmd/server/main.go           ← entry point: wires dependencies, starts HTTP server
├── internal/
│   ├── config/config.go         ← environment-based configuration loader
│   ├── model/
│   │   ├── session.go           ← DTOs: JoinResponse, SessionStatusResponse, Actor, etc.
│   │   └── event.go             ← EventEnvelope struct (Kafka/Postgres event format)
│   ├── store/
│   │   ├── schema.go            ← DDL: creates tables + indexes on startup
│   │   ├── session.go           ← SessionStore: CRUD for sessions table
│   │   ├── participant.go       ← ParticipantStore: join/leave tracking
│   │   ├── appointment.go       ← AppointmentStore: stub & remote appointment validation
│   │   └── event.go             ← EventStore: append-only audit log writes
│   ├── event/publisher.go       ← dual-write: Postgres + Kafka event emission
│   ├── agora/token.go           ← Agora RTC token builder (dev fallback)
│   ├── handler/
│   │   ├── handler.go           ← Handler struct, shared helpers (writeJSON, actorFromHeaders)
│   │   ├── health.go            ← GET /actuator/health, /actuator/health/liveness
│   │   ├── join.go              ← POST /sessions/join/{appointmentId}
│   │   ├── status.go            ← GET /sessions/{id}/status, /remaining-duration
│   │   ├── end.go               ← POST /sessions/{id}/end
│   │   ├── history.go           ← GET /sessions/history (paginated)
│   │   ├── duration.go          ← PATCH /sessions/{id}/duration
│   │   ├── webhook.go           ← POST /sessions/webhooks (Agora callbacks)
│   │   └── frontend.go          ← GET /test (serves web/index.html)
│   └── middleware/
│       ├── correlation.go       ← X-Correlation-ID propagation
│       └── metrics.go           ← Prometheus request counter + histogram
├── web/index.html               ← browser test console
├── Dockerfile                   ← multi-stage Docker build
├── go.mod / go.sum              ← Go module files
└── README.md                    ← this file
```

---

## Build & Run

### Prerequisites

- Go 1.26+
- PostgreSQL 16 (or the Docker Compose stack)
- Kafka (or the Docker Compose stack)

### Local (no Docker)

```bash
cd services/telemedicine-service

# download dependencies
go mod download

# build the binary
go build -o server ./cmd/server

# set the minimum required env vars (Postgres must be reachable)
export TELEMEDICINE_DB_DSN="postgres://telemedicine:telemedicine@localhost:5434/telemedicine_db?sslmode=disable"
export KAFKA_BOOTSTRAP_SERVERS="localhost:9092"

# run
./server
```

The service starts on `http://localhost:3006`. Open `http://localhost:3006/test` for the browser test console.

### With Docker Compose (recommended)

From the **project root** (`/Aferent`):

```bash
# build and start only telemedicine + its dependencies
docker compose up --build telemedicine-service

# or everything
docker compose up --build
```

---

## Configuration (Environment Variables)

| Variable | Default | Description |
|----------|---------|-------------|
| `PORT` | `3006` | HTTP listen port |
| `TELEMEDICINE_DB_DSN` | `postgres://telemedicine:telemedicine@localhost:5432/telemedicine_db?sslmode=disable` | PostgreSQL connection string |
| `KAFKA_BOOTSTRAP_SERVERS` | `localhost:9092` | Comma-separated Kafka broker addresses |
| `KAFKA_TOPIC_SESSION_EVENTS` | `telemedicine.events` | Kafka topic for session events |
| `KAFKA_TOPIC_SESSION_STARTED` | `telemedicine.session.started` | Dedicated topic consumed by appointment-service to save video link |
| `APPOINTMENT_MODE` | `stub` | `stub` = local Postgres appointments; `remote` = validate via real appointment-service |
| `APPOINTMENT_SERVICE_URL` | `http://appointment-service:3004` | Base URL of appointment-service (used in remote mode) |
| `SESSION_LINK_BASE_URL` | `http://localhost:8080` | Base URL for building video session links sent to appointment-service |
| `DEFAULT_SESSION_DURATION_SEC` | `1800` | Default call duration in seconds (30 min) |
| `WARNING_THRESHOLD_SEC` | `300` | Seconds remaining when a warning event fires |
| `NO_SHOW_THRESHOLD_SEC` | `60` | Seconds past scheduled start before no-show detection |
| `RECONNECT_GRACE_SEC` | `90` | Grace period for reconnections |
| `AGORA_APP_ID` | _(empty)_ | Agora application ID. If empty, dev tokens are issued |
| `AGORA_APP_CERTIFICATE` | _(empty)_ | Agora certificate. Required for real tokens |
| `AGORA_WEBHOOK_SECRET` | _(empty)_ | HMAC secret for webhook signature verification. If empty, verification is skipped |
| `AGORA_TOKEN_TTL_SEC` | `3600` | Agora token time-to-live in seconds |

---

## API Reference

All session endpoints are routed through the API Gateway at `http://localhost:8080/sessions/...`. For direct access during development, use `http://localhost:3006/sessions/...`.

### Common Headers

These are set by the API Gateway after JWT validation:

| Header | Required | Description |
|--------|----------|-------------|
| `X-User-ID` | Yes | Authenticated user's ID |
| `X-User-Role` | Yes | `DOCTOR`, `PATIENT`, or `ADMIN` |
| `X-User-Email` | Yes | User's email address |

---

### POST /sessions/join/{appointmentId}

Join (or create) a video session for the given appointment.

**Request:** No body required. Identity comes from headers.

**Response (200):**

```json
{
  "appointmentId": "appt-001",
  "channelName": "appt_appt-001",
  "appId": "47ba4aef...",
  "token": "007eJxT...",
  "uid": "doctor-001",
  "role": "DOCTOR",
  "status": "in_progress",
  "otherParticipantJoined": false,
  "expiresAt": "2025-04-13T15:00:00Z"
}
```

| Field | Type | Description |
|-------|------|-------------|
| `appointmentId` | string | Appointment identifier |
| `channelName` | string | Agora channel name (`appt_` + appointmentId) |
| `appId` | string | Agora app ID (empty if credentials not configured) |
| `token` | string | Agora RTC token. Starts with `dev-` if credentials not set |
| `uid` | string | User ID used in the Agora channel |
| `role` | string | Caller's role |
| `status` | string | `in_progress` after join |
| `otherParticipantJoined` | boolean | Whether the counterpart (doctor/patient) is already in |
| `expiresAt` | string | Token expiration (RFC 3339) |

**Events emitted:** `telemedicine.session.started` (first join), `telemedicine.participant.joined`, `telemedicine.no_show_detected` (conditional)

---

### GET /sessions/{appointmentId}/status

Get current state of a session.

**Response (200):**

```json
{
  "appointmentId": "appt-001",
  "channelName": "appt_appt-001",
  "status": "in_progress",
  "startedAt": "2025-04-13T14:30:00Z",
  "endedAt": null,
  "doctorJoined": true,
  "patientJoined": false,
  "remainingDurationSec": 1200,
  "durationOverrideSec": null,
  "noShowNotified": false,
  "recordingStatus": "none",
  "recordingObjectKey": null
}
```

---

### GET /sessions/{appointmentId}/remaining-duration

Quick check on time left.

**Response (200):**

```json
{
  "appointmentId": "appt-001",
  "remainingDurationSec": 1200,
  "warningThresholdSec": 300
}
```

---

### POST /sessions/{appointmentId}/end

End the session. Only `DOCTOR`, `PATIENT`, or `ADMIN` can end.

**Response (200):**

```json
{
  "appointmentId": "appt-001",
  "status": "ended",
  "durationSeconds": 1234
}
```

**Events emitted:** `telemedicine.session.ended`

---

### GET /sessions/history

Paginated session history with optional filters.

| Query Param | Type | Default | Description |
|-------------|------|---------|-------------|
| `patientId` | string | — | Filter by patient |
| `doctorId` | string | — | Filter by doctor |
| `limit` | int | 20 | Max results (cap: 200) |
| `offset` | int | 0 | Pagination offset |

**Response (200):**

```json
{
  "items": [
    {
      "appointmentId": "appt-001",
      "channelName": "appt_appt-001",
      "status": "ended",
      "patientId": "patient-001",
      "doctorId": "doctor-001",
      "startedAt": "2025-04-13T14:30:00Z",
      "endedAt": "2025-04-13T15:00:00Z",
      "durationSeconds": 1800,
      "recordingStatus": "none",
      "recordingObjectKey": null,
      "durationOverrideSeconds": null
    }
  ],
  "limit": 20,
  "offset": 0
}
```

---

### PATCH /sessions/{appointmentId}/duration

Extend or change session duration. **Doctor or Admin only** — patients get 403.

**Request:**

```json
{
  "durationSeconds": 3600,
  "reason": "complex case, need more time"
}
```

`durationSeconds` must be >= 60.

**Response (200):**

```json
{
  "appointmentId": "appt-001",
  "durationSeconds": 3600,
  "updatedBy": "doctor-001"
}
```

**Events emitted:** `telemedicine.session.duration_changed`

---

### POST /sessions/webhooks

Receives callbacks from Agora (or simulated events from the test console). If `AGORA_WEBHOOK_SECRET` is set, the `X-Webhook-Signature` header is verified with HMAC-SHA256.

**Request:**

```json
{
  "type": "participant_joined",
  "appointmentId": "appt-001",
  "userId": "doctor-001",
  "role": "DOCTOR"
}
```

For recording events:

```json
{
  "type": "recording_ready",
  "appointmentId": "appt-001",
  "recordingUrl": "https://...",
  "objectKey": "recordings/appt-001.mp4"
}
```

**Accepted event types** (with aliases):

| Canonical | Aliases | Action |
|-----------|---------|--------|
| `participant_joined` | `user-joined`, `participant.joined` | Upsert participant |
| `participant_left` | `user-left`, `participant.left` | Mark participant left |
| `recording_ready` | `recording-ready`, `recording.ready` | Store recording metadata |

**Response (200):**

```json
{ "status": "ok" }
```

---

### Infrastructure Endpoints

| Endpoint | Description |
|----------|-------------|
| `GET /actuator/health` | Health check (pings Postgres). Returns `{"status":"UP","checks":{"db":"UP"}}` |
| `GET /actuator/health/liveness` | Lightweight liveness probe. Returns `{"status":"UP"}` |
| `GET /actuator/prometheus` | Prometheus metrics scrape endpoint |
| `GET /test` | Browser test console (serves `web/index.html`) |
| `GET /` | Simple text: `telemedicine-service up` |

---

## Kafka Events

All events are published to the `telemedicine.events` topic (configurable). The message key is the `appointmentId`, which ensures ordering per session.

### Event Envelope Format

Every event follows this structure:

```json
{
  "eventId": "550e8400-e29b-41d4-a716-446655440000",
  "eventType": "telemedicine.session.started",
  "timestamp": "2025-04-13T14:30:00Z",
  "correlationId": "req-abc123",
  "appointmentId": "appt-001",
  "producer": "telemedicine-service",
  "version": "v1",
  "payload": { ... }
}
```

### Event Types

| Event Type | Payload Fields | Trigger |
|-----------|----------------|---------|
| `telemedicine.session.started` | `startedAt`, `joinedBy`, `userId` | First participant joins |
| `telemedicine.participant.joined` | `role`, `userId`, `joined` | Any participant joins |
| `telemedicine.participant.left` | `userId`, `role`, `leftAt` | Participant leaves (webhook) |
| `telemedicine.no_show_detected` | `missingRole`, `thresholdTime` | No-show threshold exceeded |
| `telemedicine.session.ended` | `endedBy`, `endedByRole`, `durationSeconds`, `recordingStatus`, `recordingObjectKey` | Session ended |
| `telemedicine.session.duration_changed` | `newDurationSeconds`, `reason`, `actorRole`, `actorId` | Duration patched |
| `telemedicine.recording.ready` | `recordingUrl`, `objectKey` | Recording webhook received |

### Viewing Events in Kafka UI

Kafka UI is available at `http://localhost:8090`.

1. Open Kafka UI → Topics → `telemedicine.events`
2. Click **Messages** to see all published events
3. Use the **Key** column to filter by appointment ID

---

## Database Schema

The service auto-creates all tables on startup (no migration tool needed). Four tables are created in the `telemedicine_db` database:

### `appointments_stub`

Temporary local appointment records used when `APPOINTMENT_MODE=stub` (i.e., the appointment-service is not available yet).

| Column | Type | Description |
|--------|------|-------------|
| `appointment_id` | TEXT PK | Appointment identifier |
| `patient_id` | TEXT | Patient user ID |
| `doctor_id` | TEXT | Doctor user ID |
| `scheduled_start` | TIMESTAMPTZ | Defaults to NOW() |
| `scheduled_duration_seconds` | BIGINT | Defaults to 1800 |
| `created_at` | TIMESTAMPTZ | Row creation time |
| `updated_at` | TIMESTAMPTZ | Last modification |

### `sessions`

One row per video call. Core table.

| Column | Type | Description |
|--------|------|-------------|
| `appointment_id` | TEXT PK | Links to appointment |
| `channel_name` | TEXT | Agora channel name |
| `status` | TEXT | `scheduled` → `in_progress` → `ended` |
| `started_at` | TIMESTAMPTZ | When first participant joined |
| `ended_at` | TIMESTAMPTZ | When session was ended |
| `scheduled_start` | TIMESTAMPTZ | From appointment |
| `scheduled_duration_seconds` | BIGINT | Default duration |
| `duration_override_seconds` | BIGINT | Set by PATCH /duration |
| `no_show_notified` | BOOLEAN | No-show event fired? |
| `recording_status` | TEXT | `none` or `ready` |
| `recording_object_key` | TEXT | MinIO key (future) |
| `created_at` / `updated_at` | TIMESTAMPTZ | Timestamps |

### `session_participants`

Tracks each person in a call.

| Column | Type | Description |
|--------|------|-------------|
| `appointment_id` | TEXT | FK to sessions |
| `user_id` | TEXT | User identifier |
| `role` | TEXT | DOCTOR / PATIENT / ADMIN |
| `joined_at` | TIMESTAMPTZ | When they joined |
| `last_seen_at` | TIMESTAMPTZ | Updated on upsert |
| `left_at` | TIMESTAMPTZ | When they left (null = still in) |

**Primary Key:** `(appointment_id, user_id, role)`

### `session_events`

Append-only audit log. Every action that produces a Kafka event also gets a row here.

| Column | Type | Description |
|--------|------|-------------|
| `event_id` | UUID PK | Unique event ID |
| `appointment_id` | TEXT | Session identifier |
| `event_type` | TEXT | e.g. `telemedicine.session.started` |
| `payload` | JSONB | Full event data (queryable) |
| `correlation_id` | TEXT | Request trace ID |
| `created_at` | TIMESTAMPTZ | When event was stored |

### Indexes

```sql
idx_sessions_status              ON sessions(status)
idx_sessions_started_at          ON sessions(started_at)
idx_session_events_appointment   ON session_events(appointment_id, created_at DESC)
```

---

## Integration with Other Services

### API Gateway (Spring Boot)

The gateway routes all `/sessions/**` traffic to `telemedicine-service:3006`. JWT tokens are validated at the gateway, and the decoded user info is forwarded as headers:

- `X-User-ID` — user's unique ID
- `X-User-Role` — `DOCTOR`, `PATIENT`, or `ADMIN`
- `X-User-Email` — user's email

The webhook endpoint is **exempt from JWT**, since Agora calls it directly with its own HMAC signature.

### Auth Service (Spring Boot)

The auth service issues JWT tokens at `POST /auth/register` and `POST /auth/login`. The telemedicine service does **not** talk to auth directly — it trusts the headers set by the gateway.

### Appointment Service (Spring Boot, port 3004)

When `APPOINTMENT_MODE=remote`, the telemedicine service validates appointments by calling `GET /appointments/{id}` on the appointment-service (internal Docker network). It verifies:

- The appointment exists
- Type is `VIDEO`
- Status is `CONFIRMED` (appointment has been approved by doctor and paid for)
- The requesting user is the patient or doctor on the appointment

When a video session starts, the telemedicine service publishes to the dedicated `telemedicine.session.started` Kafka topic with a flat payload:

```json
{
  "appointmentId": "appt-123",
  "sessionLink": "http://localhost:8080/sessions/join/appt-123"
}
```

The appointment-service consumes this event and saves the `sessionLink` as the `videoSessionLink` field on the appointment record.

When `APPOINTMENT_MODE=stub` (default for local dev), appointments are auto-created in a local `appointments_stub` table — no dependency on appointment-service.

### Kafka Consumers (other services)

Any service can subscribe to `telemedicine.events` to react to session lifecycle:

- **notification-service**: could listen for `telemedicine.session.started` to send SMS/email
- **payment-service**: could listen for `telemedicine.session.ended` to trigger billing
- **patient-service**: could listen for `telemedicine.recording.ready` to link recordings to patient files

---

## Testing Workflows

### Workflow 1: Full Session Lifecycle (curl)

```bash
# 1. Doctor joins
curl -X POST http://localhost:3006/sessions/join/appt-test-001 \
  -H "X-User-ID: doctor-001" \
  -H "X-User-Role: DOCTOR" \
  -H "X-User-Email: doc@example.com"

# 2. Patient joins (same appointment)
curl -X POST http://localhost:3006/sessions/join/appt-test-001 \
  -H "X-User-ID: patient-001" \
  -H "X-User-Role: PATIENT" \
  -H "X-User-Email: pat@example.com"

# 3. Check session status
curl http://localhost:3006/sessions/appt-test-001/status \
  -H "X-User-ID: doctor-001" \
  -H "X-User-Role: DOCTOR" \
  -H "X-User-Email: doc@example.com"

# 4. Check remaining time
curl http://localhost:3006/sessions/appt-test-001/remaining-duration \
  -H "X-User-ID: doctor-001" \
  -H "X-User-Role: DOCTOR" \
  -H "X-User-Email: doc@example.com"

# 5. Extend duration (doctor only)
curl -X PATCH http://localhost:3006/sessions/appt-test-001/duration \
  -H "Content-Type: application/json" \
  -H "X-User-ID: doctor-001" \
  -H "X-User-Role: DOCTOR" \
  -H "X-User-Email: doc@example.com" \
  -d '{"durationSeconds": 3600, "reason": "complex case"}'

# 6. End session
curl -X POST http://localhost:3006/sessions/appt-test-001/end \
  -H "X-User-ID: doctor-001" \
  -H "X-User-Role: DOCTOR" \
  -H "X-User-Email: doc@example.com"

# 7. View history
curl "http://localhost:3006/sessions/history?doctorId=doctor-001" \
  -H "X-User-ID: doctor-001" \
  -H "X-User-Role: DOCTOR" \
  -H "X-User-Email: doc@example.com"
```

### Workflow 2: Webhook Simulation

```bash
# Simulate a participant leaving
curl -X POST http://localhost:3006/sessions/webhooks \
  -H "Content-Type: application/json" \
  -d '{
    "type": "participant_left",
    "appointmentId": "appt-test-001",
    "userId": "patient-001",
    "role": "PATIENT"
  }'

# Simulate recording ready
curl -X POST http://localhost:3006/sessions/webhooks \
  -H "Content-Type: application/json" \
  -d '{
    "type": "recording_ready",
    "appointmentId": "appt-test-001",
    "recordingUrl": "https://example.com/recording.mp4",
    "objectKey": "recordings/appt-test-001.mp4"
  }'
```

### Workflow 3: Through the API Gateway

If the full stack is running (`docker compose up`):

```bash
# 1. Register and login to get a JWT
TOKEN=$(curl -s -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"doc@example.com","password":"password123"}' | jq -r '.token')

# 2. Use the JWT for session calls
curl -X POST http://localhost:8080/sessions/join/appt-gw-001 \
  -H "Authorization: Bearer $TOKEN"
```

### Workflow 4: Browser Test Console

1. Start the stack: `docker compose up --build`
2. Open `http://localhost:3006/test`
3. Fill in appointment ID, user ID, role, email
4. Click **Join Session** → see the response in the debug log
5. Open a second browser tab with a different user ID and `PATIENT` role
6. Both tabs will show `otherParticipantJoined: true`
7. Use the **Duration Controls** to extend the session
8. Click **End Session** to terminate
9. Switch to the **Session History** tab to see completed sessions

### Workflow 5: Health Checks

```bash
# Full health (checks DB connectivity)
curl http://localhost:3006/actuator/health

# Liveness (lightweight, no DB check)
curl http://localhost:3006/actuator/health/liveness

# Prometheus metrics
curl http://localhost:3006/actuator/prometheus
```

---

## Verifying Data in PostgreSQL (DBeaver)

### Connection Settings

| Field | Value |
|-------|-------|
| Host | `localhost` |
| Port | `5434` |
| Database | `telemedicine_db` |
| Username | `telemedicine` |
| Password | `telemedicine` |

### Useful Queries

**See all sessions:**

```sql
SELECT appointment_id, channel_name, status, started_at, ended_at,
       scheduled_duration_seconds, duration_override_seconds, recording_status
FROM sessions
ORDER BY created_at DESC;
```

**See who joined a specific session:**

```sql
SELECT user_id, role, joined_at, left_at, last_seen_at
FROM session_participants
WHERE appointment_id = 'appt-test-001';
```

**See the event audit trail for a session:**

```sql
SELECT event_id, event_type, correlation_id, created_at,
       payload->>'userId' AS user_id,
       payload->>'role' AS role
FROM session_events
WHERE appointment_id = 'appt-test-001'
ORDER BY created_at DESC;
```

**Check stub appointments:**

```sql
SELECT * FROM appointments_stub ORDER BY created_at DESC;
```

**Count active sessions:**

```sql
SELECT status, COUNT(*) FROM sessions GROUP BY status;
```

**Find sessions where duration was extended:**

```sql
SELECT appointment_id, scheduled_duration_seconds, duration_override_seconds
FROM sessions
WHERE duration_override_seconds IS NOT NULL;
```

**Search events by type (e.g. no-shows):**

```sql
SELECT * FROM session_events
WHERE event_type = 'telemedicine.no_show_detected'
ORDER BY created_at DESC;
```

---

## Compiled Binary Location

| Context | Path | Notes |
|---------|------|-------|
| Local build | `./server` | After running `go build -o server ./cmd/server` |
| Docker build stage | `/out/telemedicine-service` | Inside the `golang:1.26-alpine` build container |
| Docker runtime | `/app/telemedicine-service` | Inside the final `alpine:3.21` container |

To inspect the binary inside a running container:

```bash
docker exec -it telemedicine-service ls -la /app/telemedicine-service
```

---

## Docker

### Multi-stage Build

The [Dockerfile](Dockerfile) uses a two-stage build:

1. **Build stage** (`golang:1.26-alpine`): downloads dependencies, compiles to `/out/telemedicine-service`
2. **Runtime stage** (`alpine:3.21`): copies the binary + `web/` folder, runs as non-root `appuser`

### Build Manually

```bash
cd services/telemedicine-service
docker build -t telemedicine-service .
```

### Run Manually

```bash
docker run --rm -p 3006:3006 \
  -e TELEMEDICINE_DB_DSN="postgres://telemedicine:telemedicine@host.docker.internal:5434/telemedicine_db?sslmode=disable" \
  -e KAFKA_BOOTSTRAP_SERVERS="host.docker.internal:9092" \
  telemedicine-service
```

Use `docker compose up --build telemedicine-service` for the normal workflow — it handles networking and dependency readiness automatically.
