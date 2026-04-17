-- PostgreSQL Initialization Script for Telemedicine Service
-- Database: telemedicine_db
-- This script creates the necessary tables and indexes for the telemedicine-service.
-- Run this against the postgres-telemedicine container or a PostgreSQL instance.

-- appointments_stub holds minimal appointment data so telemedicine can work
-- independently. Once appointment-service is live, this table will be replaced
-- by HTTP calls to GET /appointments/{id}.
CREATE TABLE IF NOT EXISTS appointments_stub (
    appointment_id TEXT PRIMARY KEY,
    patient_id TEXT,
    doctor_id TEXT,
    scheduled_start TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    scheduled_duration_seconds BIGINT NOT NULL DEFAULT 1800,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- sessions is the core table — one row per video call.
-- status transitions: scheduled → in_progress → ended
CREATE TABLE IF NOT EXISTS sessions (
    appointment_id TEXT PRIMARY KEY,
    channel_name TEXT NOT NULL,
    status TEXT NOT NULL DEFAULT 'scheduled',
    started_at TIMESTAMPTZ,
    ended_at TIMESTAMPTZ,
    scheduled_start TIMESTAMPTZ NOT NULL,
    scheduled_duration_seconds BIGINT NOT NULL,
    duration_override_seconds BIGINT,
    no_show_notified BOOLEAN NOT NULL DEFAULT FALSE,
    recording_status TEXT NOT NULL DEFAULT 'none',
    recording_object_key TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- session_participants tracks each person in the call.
-- The composite primary key (appointment_id, user_id, role) means
-- a user can only have one active entry per session per role.
CREATE TABLE IF NOT EXISTS session_participants (
    appointment_id TEXT NOT NULL,
    user_id TEXT NOT NULL,
    role TEXT NOT NULL,
    joined_at TIMESTAMPTZ NOT NULL,
    last_seen_at TIMESTAMPTZ NOT NULL,
    left_at TIMESTAMPTZ,
    PRIMARY KEY (appointment_id, user_id, role)
);

-- session_events is an append-only audit log.
-- Every action (join, leave, end, webhook) writes a row here AND to Kafka.
-- The payload column is JSONB so you can query it with Postgres JSON operators.
CREATE TABLE IF NOT EXISTS session_events (
    event_id UUID PRIMARY KEY,
    appointment_id TEXT NOT NULL,
    event_type TEXT NOT NULL,
    payload JSONB NOT NULL,
    correlation_id TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Indexes for common query patterns
CREATE INDEX IF NOT EXISTS idx_sessions_status ON sessions(status);
CREATE INDEX IF NOT EXISTS idx_sessions_started_at ON sessions(started_at);
CREATE INDEX IF NOT EXISTS idx_session_events_appointment ON session_events(appointment_id, created_at DESC);