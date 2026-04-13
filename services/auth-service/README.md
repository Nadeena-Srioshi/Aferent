# Auth Service

Authentication and account-lifecycle service for Aferent.

This service handles:
- User registration and login
- JWT access token issuance
- Refresh token issuance via HTTP-only cookie
- Role-based activation/deactivation flows
- Global session revocation via refresh token version (`rtv`)

---

## Tech Stack

- Java 21
- Spring Boot
- Spring Data MongoDB
- Spring Kafka
- JJWT (JSON Web Tokens)

---

## Service Configuration

`src/main/resources/application.yaml`

- `server.port`: `3001`
- MongoDB URI: `${MONGO_URI}`
- Kafka bootstrap: `${KAFKA_BOOTSTRAP_SERVERS:kafka:29092}`
- JWT secret: `${JWT_SECRET}`
- Access token TTL: `900000 ms` (15 minutes)
- Refresh token TTL: `604800000 ms` (7 days)

Error response verbosity is reduced:
- no stack traces in API responses
- no detailed binding errors in default server error output

---

## Data Model (`users` collection)

`User` fields:
- `id`
- `email` (unique)
- `passwordHash` (BCrypt)
- `role` (`PATIENT | DOCTOR | ADMIN`)
- `active` (default true)
- `refreshTokenVersion` (default `1`)
- audit fields: `activatedAt`, `activatedBy`, `deactivatedAt`, `deactivatedBy`, `deactivationReason`

---

## JWT Design

### Access Token
Claims:
- `sub` = user id
- `email`
- `role`
- `active`
- `rtv` = refresh token version snapshot at issuance time
- `iat`, `exp`

### Refresh Token
Claims:
- `sub` = user id
- `rtv`
- `iat`, `exp`

### Why `rtv` exists
`rtv` is a version number used for revocation checks.
- If DB `refreshTokenVersion` changes, tokens carrying an older `rtv` are treated as revoked where version checks are performed.

---

## Cookie Behavior

Refresh token is stored in cookie:
- Name: `refreshToken`
- `HttpOnly: true`
- `Path: /auth`
- `Max-Age: 7 days`
- `Secure: false` (current code; suitable only for non-HTTPS local/dev)

On logout endpoints, cookie is cleared by setting:
- value empty
- same path `/auth`
- `Max-Age: 0`

---

## Route Security Model

All requests pass through API Gateway (`JwtAuthFilter`).

### Gateway public paths (no JWT required)
- `/auth/login`
- `/auth/register`
- `/auth/refresh`
- `/auth/logout`
- `/payments/webhook`
- `/actuator`

All other paths are protected by default.

### Gateway protected-path behavior
- Requires `Authorization: Bearer <token>`
- Verifies JWT signature and expiry
- Adds forwarded identity headers:
  - `X-User-ID`
  - `X-User-Role`
  - `X-User-Email`
- Preserves `Authorization` header only for `/auth/**` routes
- Strips `Authorization` when forwarding to non-auth services

---

## Auth Endpoints

Base path: `/auth`

## 1) Register
`POST /auth/register`

Creates user account with role policy:
- `PATIENT` -> active immediately
- `DOCTOR` -> created inactive (pending admin activation)
- `ADMIN` -> blocked via public registration

### Request
```json
{
  "email": "doctor@aferent.com",
  "password": "Password@123",
  "role": "DOCTOR"
}
```

### Response (200)
```json
{
  "accessToken": "...",
  "tokenType": "Bearer",
  "role": "DOCTOR",
  "authId": "..."
}
```

Possible errors:
- `409` Email already registered
- `400` Invalid role
- `403` Admin registration is not allowed

---

## 2) Login
`POST /auth/login`

Validates credentials, checks account active state, returns access token and sets refresh cookie.

### Request
```json
{
  "email": "patient@aferent.com",
  "password": "Password@123"
}
```

### Response (200)
```json
{
  "accessToken": "...",
  "tokenType": "Bearer",
  "role": "PATIENT",
  "authId": "..."
}
```

Possible errors:
- `401` Invalid email or password
- `403` Account pending admin verification (doctor)
- `403` Account is inactive

---

## 3) Refresh Access Token
`POST /auth/refresh`

Reads refresh token from cookie and issues a new access token.

Validation steps:
1. Refresh token signature/expiry valid
2. User exists
3. User is active
4. Refresh token `rtv` equals DB `refreshTokenVersion`

### Response (200)
```json
{
  "accessToken": "..."
}
```

Possible errors:
- `401` No refresh token
- `401` Invalid or expired refresh token
- `401` Refresh token has been revoked
- `403` Account is inactive

---

## 4) Logout (session/browser logout)
`POST /auth/logout`

Current behavior:
- Clears refresh cookie from browser
- Does **not** increment `rtv`

Use case:
- Local session logout UX on current browser/device.

### Response (200)
```json
{
  "message": "Logged out"
}
```

---

## 5) Logout All Devices
`POST /auth/logout-all`

Requires access token in `Authorization` header.

Behavior:
- Validates actor access token (including access-token `rtv` vs DB)
- Increments actor `refreshTokenVersion`
- Clears local refresh cookie

Effect:
- Old refresh tokens are revoked globally
- Auth-service endpoints that enforce access-token `rtv` reject old access tokens as revoked

### Response (200)
```json
{
  "message": "Logged out from all devices"
}
```

Possible errors:
- `401` Missing/invalid/expired token
- `401` Access token has been revoked
- `403` Account is inactive

---

## 6) Admin Activate User
`POST /auth/admin/users/{userId}/activate`

Requires admin access token.

Behavior:
- Actor must be `ADMIN`
- Activates target user
- Writes activation audit fields
- Clears deactivation audit fields

### Response (200)
```json
{
  "message": "User activated",
  "userId": "..."
}
```

Possible errors:
- `403` Forbidden (non-admin)
- `404` User not found

---

## 7) Admin Deactivate User
`POST /auth/admin/users/{userId}/deactivate?reason=...`

Requires admin access token.

Behavior:
- Actor must be `ADMIN`
- Deactivates target user
- Writes deactivation audit fields
- If target was active, increments target `refreshTokenVersion`

### Response (200)
```json
{
  "message": "User deactivated",
  "userId": "..."
}
```

Possible errors:
- `403` Forbidden (non-admin)
- `404` User not found

---

## 8) Self Deactivate (Patient only)
`POST /auth/deactivate?reason=...`

Requires user access token.

Behavior:
- Only `PATIENT` role allowed
- Deactivates own account
- Increments own `refreshTokenVersion` if previously active
- Clears local refresh cookie

### Response (200)
```json
{
  "message": "Account deactivated"
}
```

Possible errors:
- `403` Only patients can self-deactivate

---

## Authorization and Revocation Flow

## A) Normal protected request via gateway
1. Client sends `Authorization: Bearer <access_token>`
2. Gateway validates JWT signature + expiry
3. Gateway forwards identity headers (`X-User-*`)
4. Target service authorizes action using role/user context

## B) Refresh flow
1. Client sends `/auth/refresh` with refresh cookie
2. Auth service validates refresh token and checks DB `rtv`
3. If valid, new access token issued with current `rtv`

## C) Logout-all flow
1. Client sends `/auth/logout-all` with access token
2. Auth service validates token and compares access-token `rtv` with DB
3. Auth service increments DB `refreshTokenVersion`
4. Old tokens become stale where `rtv` checks are enforced

---

## Error Response Format

All handled API errors return structured JSON:

```json
{
  "timestamp": "2026-04-12T11:52:40.123Z",
  "status": 401,
  "error": "Unauthorized",
  "message": "Invalid or expired access token",
  "path": "/auth/logout-all"
}
```

Global exception handling classes:
- `ApiException`
- `ApiErrorResponse`
- `GlobalExceptionHandler`

---

## Security Measures Implemented

- BCrypt password hashing (no plaintext password storage)
- Signed JWT tokens with shared secret
- Short-lived access tokens (15 min)
- HTTP-only refresh cookie (not accessible to JS)
- Account activation state checks (`active`)
- Role-based authorization for admin/self lifecycle endpoints
- Token revocation mechanism using `refreshTokenVersion` (`rtv`)
- Minimal error leakage in responses/log formatting
- Gateway strips raw JWT before forwarding to non-auth services

---

## Current Known Tradeoff

`/auth/logout` currently clears only the browser cookie and does not mutate `rtv`.
- Good for simple session UX
- Existing access token may remain usable until expiry
- Use `/auth/logout-all` for immediate global revocation semantics

---

## Environment Variables

Set at runtime:
- `MONGO_URI`
- `JWT_SECRET`
- `KAFKA_BOOTSTRAP_SERVERS` (optional; defaults to `kafka:29092`)

---

## Run (development)

From `services/auth-service`:
- `./mvnw spring-boot:run`

Or with Docker Compose from repo root:
- `docker compose up -d --build auth-service`

---

## Notes for Integrators

- Always send access token as `Authorization: Bearer <token>` for protected routes.
- Refresh requires browser cookie handling (`withCredentials` in frontend HTTP client).
- For critical account lockout across devices, call `/auth/logout-all`.
- Public admin registration is intentionally disabled.
