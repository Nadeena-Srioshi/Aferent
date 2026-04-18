# Appointment Service API Documentation

This document describes the `appointment-service` APIs for frontend integration.

- **Service (direct)**: `http://localhost:3004`
- **Via API Gateway (recommended for frontend)**: `http://localhost:8080`
- **Gateway route prefix**: `/appointments/**`

---

## Authentication & Access Policy

### How auth is enforced

- `appointment-service` itself currently permits all requests at Spring Security level.
- **Actual auth enforcement is done at API Gateway** for `/appointments/**`.
- Gateway validates JWT and forwards identity headers:
  - `X-User-ID`
  - `X-User-Role`
  - `X-User-Email`

> For frontend: always call through gateway (`:8080`) with `Authorization: Bearer <token>`.

### Role-based access matrix

| Endpoint | Role(s) allowed | Notes |
|---|---|---|
| `GET /appointments/slots` | Authenticated user | Public browsing use-case, but currently behind gateway auth |
| `GET /appointments/slots/doctor/{doctorId}` | Public | Doctor required; optional `type` and `date` filters |
| `POST /appointments` | `PATIENT` only | Returns `403` if role is not PATIENT |
| `GET /appointments` | `PATIENT`, `DOCTOR`, `ADMIN` | Returns own patient list / doctor list / all for admin |
| `GET /appointments/{id}` | Owner patient, owner doctor, or `ADMIN` | Returns `403` when unauthorized |
| `GET /appointments/pending-video` | `DOCTOR` only | Video requests waiting doctor action |
| `PATCH /appointments/{id}/status` | `DOCTOR`, `ADMIN` | Used to accept/reject (video flow) |
| `PATCH /appointments/{id}/cancel` | Appointment patient or `ADMIN` | Cancels and may trigger refund flow |
| `PATCH /appointments/{id}/complete` | `DOCTOR` only | Doctor must match appointment doctor |
| `/debug/**` endpoints | Intended internal/admin/debug | Do not expose in production UI |

---

## Common Headers

### Required for authenticated endpoints

- `Authorization: Bearer <jwt>`

### Identity headers (injected by gateway)

The service reads:

- `X-User-ID`
- `X-User-Role`
- `X-User-Email`

You do **not** need to send these manually from browser when using gateway with JWT.

---

## Enums Used

### `AppointmentType`

- `PHYSICAL`
- `VIDEO`

### `AppointmentStatus`

- `PENDING_PAYMENT`
- `PENDING_DOCTOR_APPROVAL`
- `REJECTED`
- `ACCEPTED_PENDING_PAYMENT`
- `CONFIRMED`
- `COMPLETED`
- `CANCELLED`
- `CANCELLED_NO_REFUND`
- `PAYMENT_FAILED`

---

## Data Contracts

## `BookAppointmentRequest`

```json
{
  "doctorId": "DOC_12345",
  "scheduleId": "SCHED_abc123",
  "type": "PHYSICAL",
  "appointmentDate": "2026-04-20",
  "patientName": "John Doe",
  "doctorName": "Dr. Silva",
  "videoSlotId": "video_slot_09_30"
}
```

Notes:
- `videoSlotId` is required for `VIDEO` bookings.
- For `PHYSICAL`, `videoSlotId` should be omitted/null.
- `appointmentDate` format is `YYYY-MM-DD`.

## `AppointmentResponse` (shape)

```json
{
  "id": "67f0abc123",
  "patientId": "USER_PAT_01",
  "patientName": "John Doe",
  "doctorId": "DOC_12345",
  "doctorAuthId": "AUTH_DOC_99",
  "doctorName": "Dr. Silva",
  "type": "PHYSICAL",
  "status": "PENDING_PAYMENT",
  "appointmentDate": "2026-04-20",
  "appointmentNumber": 14,
  "calculatedTime": "10:45:00",
  "hospitalName": "Nawaloka Hospital",
  "hospitalLocation": "Colombo",
  "videoSlotId": null,
  "videoSlotStart": null,
  "videoSlotEnd": null,
  "videoSessionLink": null,
  "consultationFee": 3500.0,
  "paymentId": null,
  "createdAt": "2026-04-17T10:15:30.221",
  "updatedAt": "2026-04-17T10:15:30.221"
}
```

## `SlotResponse` (shape)

```json
{
  "slotId": "slot_67f0aaa",
  "scheduleId": "SCHED_abc123",
  "doctorId": "DOC_12345",
  "type": "VIDEO",
  "date": "2026-04-20",
  "startTime": "09:30:00",
  "endTime": "10:00:00",
  "booked": false,
  "consultationFee": 3500.0,
  "appointmentNumber": null,
  "hospitalName": null,
  "hospitalLocation": null,
  "videoSlotId": "video_slot_09_30"
}
```

---

## Endpoints

## 1) Get available slots by schedule + date

`GET /appointments/slots?scheduleId={scheduleId}&date={YYYY-MM-DD}`

### Example request

`GET /appointments/slots?scheduleId=SCHED_abc123&date=2026-04-20`

### Success `200`

Returns `SlotResponse[]`.

```json
[
  {
    "slotId": "slot_1",
    "scheduleId": "SCHED_abc123",
    "doctorId": "DOC_12345",
    "type": "PHYSICAL",
    "date": "2026-04-20",
    "startTime": "09:00:00",
    "endTime": "09:15:00",
    "booked": false,
    "consultationFee": 3500.0,
    "appointmentNumber": 1,
    "hospitalName": "Nawaloka Hospital",
    "hospitalLocation": "Colombo",
    "videoSlotId": null
  }
]
```

---

## 2) Get available slots by doctor (optional filters)

`GET /appointments/slots/doctor/{doctorId}`

Optional query params:
- `type` = `PHYSICAL | VIDEO`
- `date` = `YYYY-MM-DD`

### Example requests

- `GET /appointments/slots/doctor/DOC_12345`
- `GET /appointments/slots/doctor/DOC_12345?date=2026-04-20`
- `GET /appointments/slots/doctor/DOC_12345?type=VIDEO&date=2026-04-20`

### Success `200`

Returns `SlotResponse[]`.

Notes:
- `doctorId` is always required in path.
- `type` and `date` are optional filters.
- Service returns only upcoming unbooked slots within a 3-week window from today (inclusive).
- If `date` is provided outside this 3-week window, response is an empty list.

---

## 3) Book appointment (patient only)

`POST /appointments`

### Required policy
- Role must be `PATIENT` (otherwise `403`).

### Example request body (PHYSICAL)

```json
{
  "doctorId": "DOC_12345",
  "scheduleId": "SCHED_abc123",
  "type": "PHYSICAL",
  "appointmentDate": "2026-04-20",
  "patientName": "John Doe",
  "doctorName": "Dr. Silva"
}
```

### Example request body (VIDEO)

```json
{
  "doctorId": "DOC_12345",
  "scheduleId": "SCHED_abc123",
  "type": "VIDEO",
  "appointmentDate": "2026-04-20",
  "patientName": "John Doe",
  "doctorName": "Dr. Silva",
  "videoSlotId": "video_slot_09_30"
}
```

### Success `201`

Returns `AppointmentResponse`.

### Typical errors
- `400` validation error (missing required fields)
- `400` for VIDEO without `videoSlotId`
- `403` if caller role is not `PATIENT`
- `404` video slot not found
- `409` no physical slot available / video slot already booked

---

## 4) Get my appointments

`GET /appointments`

### Behavior by role
- `PATIENT` → appointments where `patientId = X-User-ID`
- `DOCTOR` → appointments where `doctorAuthId = X-User-ID`
- `ADMIN` → all appointments ordered by latest created

### Success `200`

Returns `AppointmentResponse[]`.

---

## 5) Get appointment by ID

`GET /appointments/{id}`

### Access
Allowed for:
- appointment patient
- appointment doctor (`doctorId` or `doctorAuthId`)
- `ADMIN`

### Success `200`

Returns `AppointmentResponse`.

### Errors
- `403` unauthorized user
- `404` appointment not found

---

## 6) Get doctor pending video requests

`GET /appointments/pending-video`

### Required policy
- Role must be `DOCTOR`.

### Success `200`

Returns `AppointmentResponse[]` where status is `PENDING_DOCTOR_APPROVAL`.

### Errors
- `403` if non-doctor

---

## 7) Update appointment status (doctor/admin)

`PATCH /appointments/{id}/status`

### Required policy
- Role must be `DOCTOR` or `ADMIN`.

### Request body

```json
{
  "status": "ACCEPTED_PENDING_PAYMENT"
}
```

Common values used in flow:
- `ACCEPTED_PENDING_PAYMENT`
- `REJECTED`

### Success `200`

Returns updated `AppointmentResponse`.

### Errors
- `400` if `status` missing/blank
- `403` if role unauthorized
- `404` appointment not found
- `500` if invalid enum value passed

---

## 8) Cancel appointment

`PATCH /appointments/{id}/cancel`

### Access
- Appointment patient or `ADMIN`.

### Behavior
- Cancels appointment and releases slot.
- Final status becomes:
  - `CANCELLED` (refund-eligible), or
  - `CANCELLED_NO_REFUND`

### Success `200`

Returns updated `AppointmentResponse`.

### Errors
- `403` unauthorized
- `400` cannot cancel in terminal statuses (`COMPLETED`, already cancelled, etc.)
- `404` appointment not found

---

## 9) Complete appointment (doctor only)

`PATCH /appointments/{id}/complete`

### Required policy
- Role must be `DOCTOR` and doctor must match appointment doctor.
- Appointment must be in `CONFIRMED` state.

### Success `200`

Returns updated `AppointmentResponse` with status `COMPLETED`.

### Errors
- `403` role mismatch / doctor mismatch
- `400` if appointment is not `CONFIRMED`
- `404` appointment not found

---

## Error Response Formats

## App-level error (`AppException`)

```json
{
  "timestamp": "2026-04-17T11:02:13.401",
  "status": 403,
  "error": "Unauthorized"
}
```

## Validation error (`MethodArgumentNotValidException`)

```json
{
  "timestamp": "2026-04-17T11:02:13.401",
  "status": 400,
  "errors": {
    "doctorId": "Doctor ID is required",
    "appointmentDate": "Appointment date is required"
  }
}
```

## Unhandled server error

```json
{
  "timestamp": "2026-04-17T11:02:13.401",
  "status": 500,
  "error": "Internal server error: ..."
}
```

---

## Frontend Integration Notes

- Prefer calling via gateway: `http://localhost:8080/appointments/...`
- Send only JWT (`Authorization: Bearer ...`); gateway injects identity headers.
- For doctor-facing lists, ensure JWT `sub` matches doctor `authId` used in doctor-service.
- Date query/body uses `YYYY-MM-DD`.
- Time fields are ISO local time (`HH:mm:ss`).
- `consultationFee` is numeric (`double`).

---

## Debug Endpoints (Not for normal UI)

The service also exposes `/debug/slots/**` endpoints for diagnostics/manual slot generation.
These are routed by gateway and currently open under `/debug/**` public path.
Use only for admin/dev tooling; avoid exposing in production user flows.
