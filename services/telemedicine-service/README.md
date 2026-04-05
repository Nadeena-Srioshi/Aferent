# Telemedicine Service (Go)

This service provides `/sessions/**` APIs, Agora token issuance, webhook handling, Postgres-backed session history, Prometheus metrics, and a built-in test page at `/test`.

## Local run

1. Ensure Postgres, Kafka, and MinIO are reachable.
2. Set environment variables (see `docker-compose.yml`).
3. Run:

```bash
go run ./cmd/server
```

## Key endpoints

- `POST /sessions/join/{appointmentId}`
- `GET /sessions/{appointmentId}/status`
- `POST /sessions/{appointmentId}/end`
- `GET /sessions/history`
- `GET /sessions/{appointmentId}/remaining-duration`
- `PATCH /sessions/{appointmentId}/duration`
- `POST /sessions/webhooks`
- `GET /actuator/health`
- `GET /actuator/health/liveness`
- `GET /actuator/prometheus`
- `GET /test`
