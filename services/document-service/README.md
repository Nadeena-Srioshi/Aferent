# document-service

A stateless microservice that acts as a **secure path constructor and signing authority** for file storage. It manages a single MinIO bucket (`app-storage`) and enforces per-service isolation by namespacing every object path under the calling service's ID.

No database. No file storage. Pure signing logic.

---

## Table of contents

- [How it works](#how-it-works)
- [Architecture](#architecture)
- [Running locally](#running-locally)
- [Environment variables](#environment-variables)
- [API reference](#api-reference)
  - [Health check](#health-check)
  - [Request a presigned upload URL](#request-a-presigned-upload-url)
  - [Request a signed view URL](#request-a-signed-view-url)
- [Integration guide for other services](#integration-guide-for-other-services)
  - [Public file upload flow](#public-file-upload-flow)
  - [Private file upload flow](#private-file-upload-flow)
  - [Requesting a signed view URL](#requesting-a-signed-view-url)
- [Object path structure](#object-path-structure)
- [Bucket policy](#bucket-policy)
- [Error responses](#error-responses)
- [Production and Kubernetes notes](#production-and-kubernetes-notes)

---

## How it works

Other services never talk to MinIO directly. Instead they ask `document-service` for a **presigned URL**, then the client (browser, mobile app, or backend) uploads directly to MinIO using that URL.

```
Your service  ──POST /upload/presign──►  document-service  ──signs──►  MinIO
                ◄── upload_url ──────────
Client        ──PUT upload_url ──────────────────────────────────────►  MinIO
```

This means:
- MinIO credentials never leave the `document-service`.
- Uploads go directly from the client to storage — no proxying through your service.
- Every object is namespaced under the calling service's ID, so services cannot read or overwrite each other's files.

---

## Architecture

```
┌─────────────────────────────────────────┐
│              Docker network              │
│                                          │
│  ┌──────────────┐    ┌────────────────┐  │
│  │ document-    │    │     Nginx      │  │
│  │ service      │───►│  localhost:    │  │
│  │ :8080        │    │  9000 / 9001   │  │
│  └──────────────┘    └───────┬────────┘  │
│                              │           │
│                      ┌───────▼────────┐  │
│                      │     MinIO      │  │
│                      │  minio:9000    │  │
│                      └────────────────┘  │
└─────────────────────────────────────────┘
        ▲                     ▲
        │ POST /upload/presign │ PUT upload_url
        │                     │
   Your service            Browser / client
```

Nginx sits in front of MinIO so that both `document-service` (internal) and the browser (external) use the same hostname (`localhost:9000` in dev). This is required for presigned URL signatures to remain valid — the `Host` header is part of the AWS SigV4 signature, and it must match between signing time and upload time.

---

## Running locally

**Prerequisites:** Docker and Docker Compose.

```bash
# Clone and enter the project
git clone <repo-url>
cd document-service

# Start all services (MinIO + Nginx + document-service)
docker compose up --build

# Verify everything is up
curl http://localhost:8080/health
# → {"status":"ok"}

# MinIO console is available at http://localhost:9001
# Login: minioadmin / minioadmin
```

---

## Environment variables

| Variable | Required | Default | Description |
|---|---|---|---|
| `MINIO_ENDPOINT` | yes | — | Internal MinIO address used by the SDK. In Docker this is `localhost:9000` (via Nginx). |
| `MINIO_ACCESS_KEY` | yes | — | MinIO access key (root user in dev). |
| `MINIO_SECRET_KEY` | yes | — | MinIO secret key. |
| `MINIO_BUCKET` | no | `app-storage` | Bucket name. Created automatically on startup if it does not exist. |
| `MINIO_PUBLIC_HOST` | yes | — | Public-facing base URL embedded in returned URLs. Must be reachable by clients. |
| `MINIO_USE_SSL` | no | `false` | Set to `true` in production when MinIO is behind HTTPS. |
| `SERVER_PORT` | no | `8080` | Port the HTTP server listens on. |

In development these are set in `docker-compose.yml`. In production, inject them from Kubernetes `Secrets`.

---

## API reference

All endpoints require the `X-Internal-Service-ID` header. Requests missing this header are rejected with `400`. The value becomes the first segment of every object path, isolating each service's files.

### Health check

```
GET /health
```

No authentication required. Used by load balancers and Docker health checks.

**Response `200`**

```json
{"status": "ok"}
```

---

### Request a presigned upload URL

```
POST /upload/presign
```

Returns a short-lived presigned PUT URL. The client uses this URL to upload a file directly to MinIO — no data passes through `document-service`.

**Headers**

| Header | Required | Description |
|---|---|---|
| `Content-Type` | yes | `application/json` |
| `X-Internal-Service-ID` | yes | Your service name, e.g. `patient-service`. Becomes the path prefix. |

**Request body**

```json
{
  "visibility": "public",
  "category": "avatars",
  "filename": "profile.png",
  "append_uuid": false
}
```

| Field | Type | Required | Description |
|---|---|---|---|
| `visibility` | string | yes | `"public"` or `"private"`. Controls path and access policy. |
| `category` | string | yes | Logical grouping for the file, e.g. `"avatars"`, `"invoices"`, `"reports"`. |
| `filename` | string | yes | Original filename including extension, e.g. `"profile.png"`. |
| `append_uuid` | boolean | no | When `true`, a UUID is appended before the file extension to prevent collisions. Useful when multiple users upload files with the same name. |

**Response `200` — public file**

```json
{
  "upload_url": "http://localhost:9000/app-storage/patient-service/public/avatars/profile.png?X-Amz-Algorithm=...&X-Amz-Expires=900&...",
  "permanent_url": "http://localhost:9000/app-storage/patient-service/public/avatars/profile.png"
}
```

| Field | Description |
|---|---|
| `upload_url` | Presigned PUT URL. Valid for **15 minutes**. The client must upload using HTTP `PUT`, not `POST`. |
| `permanent_url` | Stable, non-expiring URL. Save this in your database. Works immediately after the upload completes, because the bucket policy grants anonymous read on all `*/public/*` paths. |

**Response `200` — private file**

```json
{
  "upload_url": "http://localhost:9000/app-storage/patient-service/private/invoices/invoice-001.pdf?X-Amz-Algorithm=...&X-Amz-Expires=900&...",
  "object_key": "patient-service/private/invoices/invoice-001.pdf"
}
```

| Field | Description |
|---|---|
| `upload_url` | Presigned PUT URL. Valid for **15 minutes**. |
| `object_key` | The full object path in MinIO. **Store this in your database.** You will need it to request a signed view URL later. There is no permanent URL for private files — they are not publicly accessible. |

---

### Request a signed view URL

```
GET /sign?key=<object_key>&expires=<seconds>
```

Returns a time-limited presigned GET URL for a private file. Call this whenever a user needs to view or download a private file. The URL expires and cannot be shared permanently.

**Headers**

| Header | Required | Description |
|---|---|---|
| `X-Internal-Service-ID` | yes | Your service name. |

**Query parameters**

| Parameter | Required | Default | Description |
|---|---|---|---|
| `key` | yes | — | The `object_key` returned when the file was uploaded. |
| `expires` | no | `3600` | URL lifetime in seconds. Maximum is `604800` (7 days). |

**Example request**

```
GET /sign?key=patient-service/private/invoices/invoice-001.pdf&expires=3600
X-Internal-Service-ID: patient-service
```

**Response `200`**

```json
{
  "url": "http://localhost:9000/app-storage/patient-service/private/invoices/invoice-001.pdf?X-Amz-Algorithm=...&X-Amz-Expires=3600&..."
}
```

The returned URL can be given directly to the browser for download or embedded in an `<img>` / `<a>` tag.

---

## Integration guide for other services

### Public file upload flow

Use this for files that anyone should be able to view without authentication — profile pictures, public documents, banner images.

**Step 1 — Your backend requests a presigned URL**

```http
POST http://document-service:8080/upload/presign
Content-Type: application/json
X-Internal-Service-ID: patient-service

{
  "visibility": "public",
  "category": "avatars",
  "filename": "profile.png",
  "append_uuid": true
}
```

**Step 2 — Return both URLs to your frontend**

```json
{
  "upload_url": "http://localhost:9000/app-storage/patient-service/public/avatars/profile_3f2a1b....png?...",
  "permanent_url": "http://localhost:9000/app-storage/patient-service/public/avatars/profile_3f2a1b....png"
}
```

**Step 3 — Frontend uploads directly to MinIO**

```javascript
// The client PUTs to the upload_url — your backend is not in this path at all
await fetch(uploadUrl, {
  method: 'PUT',
  body: file,                          // the raw File object from an <input>
  headers: {
    'Content-Type': file.type,         // must match what MinIO expects
  },
});
```

**Step 4 — Your backend saves the permanent URL**

```sql
UPDATE patients SET avatar_url = $1 WHERE id = $2;
-- store: http://localhost:9000/app-storage/patient-service/public/avatars/profile_3f2a1b....png
```

The `permanent_url` never expires. Render it directly in your HTML — no further calls to `document-service` needed.

---

### Private file upload flow

Use this for files that require authentication to view — medical records, invoices, personal documents.

**Step 1 — Your backend requests a presigned URL**

```http
POST http://document-service:8080/upload/presign
Content-Type: application/json
X-Internal-Service-ID: patient-service

{
  "visibility": "private",
  "category": "medical-records",
  "filename": "blood-test-2024.pdf",
  "append_uuid": true
}
```

**Step 2 — Return the upload URL to your frontend**

```json
{
  "upload_url": "http://localhost:9000/app-storage/patient-service/private/medical-records/blood-test-2024_3f2a1b....pdf?...",
  "object_key": "patient-service/private/medical-records/blood-test-2024_3f2a1b....pdf"
}
```

**Step 3 — Frontend uploads directly to MinIO**

```javascript
await fetch(uploadUrl, {
  method: 'PUT',
  body: file,
  headers: { 'Content-Type': file.type },
});
```

**Step 4 — Your backend saves the object key** (not a URL)

```sql
INSERT INTO medical_records (patient_id, object_key, filename)
VALUES ($1, $2, $3);
-- object_key: patient-service/private/medical-records/blood-test-2024_3f2a1b....pdf
```

> **Important:** Store `object_key`, not the `upload_url`. The upload URL expires in 15 minutes and is useless afterwards. The object key is permanent and is what you use to request view access later.

---

### Requesting a signed view URL

When an authenticated user wants to view a private file, your backend fetches a short-lived URL on demand and returns it to the client.

**Your backend (on each view request)**

```http
GET http://document-service:8080/sign?key=patient-service/private/medical-records/blood-test-2024_3f2a1b....pdf&expires=3600
X-Internal-Service-ID: patient-service
```

**Response**

```json
{
  "url": "http://localhost:9000/app-storage/patient-service/private/medical-records/blood-test-2024_3f2a1b....pdf?X-Amz-Expires=3600&..."
}
```

**Return this to your frontend**

```javascript
// Render in an <img>, open in a new tab, or trigger a download
window.open(signedUrl, '_blank');
```

The URL expires after the requested TTL. Do not cache it longer than that.

---

## Object path structure

Every object stored in MinIO follows this path structure:

```
{service-id}/{visibility}/{category}/{filename}
```

| Segment | Example | Description |
|---|---|---|
| `service-id` | `patient-service` | Value of the `X-Internal-Service-ID` header. Enforces isolation between services. |
| `visibility` | `public` or `private` | Determines bucket policy access. |
| `category` | `avatars`, `invoices` | Logical grouping chosen by the caller. |
| `filename` | `profile.png` | Original filename, optionally suffixed with a UUID. |

**Example paths**

```
patient-service/public/avatars/profile.png
patient-service/private/medical-records/blood-test-2024_3f2a1b4c.pdf
order-service/private/invoices/order-882_9d4e2f1a.pdf
report-service/public/exports/q3-summary.pdf
```

Services can only generate presigned URLs for paths prefixed with their own service ID. A service cannot request a URL for another service's path.

---

## Bucket policy

On startup, `document-service` automatically applies a bucket policy that grants **anonymous read** on all objects matching `*/public/*`:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Principal": "*",
      "Action": ["s3:GetObject"],
      "Resource": ["arn:aws:s3:::app-storage/*/public/*"]
    }
  ]
}
```

This policy is idempotent — it is applied every time the service starts, so it is safe to restart the container. Private objects (`*/private/*`) are not covered by this policy and require a presigned GET URL.

---

## Error responses

All errors return JSON with a single `error` field.

| Status | Cause |
|---|---|
| `400` | Missing `X-Internal-Service-ID` header, invalid JSON body, missing required fields, invalid `visibility` value, invalid `expires` parameter. |
| `405` | Wrong HTTP method for the endpoint. |
| `500` | MinIO is unreachable or failed to generate a presigned URL. Check service logs. |

**Example error**

```json
{
  "error": "X-Internal-Service-ID header is required"
}
```

---

## Production and Kubernetes notes

### Using a real domain

In production, replace `localhost:9000` with your actual domain in both env vars:

```env
MINIO_ENDPOINT=storage.myapp.com:443
MINIO_PUBLIC_HOST=https://storage.myapp.com
MINIO_USE_SSL=true
```

The Nginx config is essentially the same — point it at your MinIO pod instead of the Docker service name.

### Kubernetes Secret

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: document-service-secret
type: Opaque
stringData:
  MINIO_ACCESS_KEY: <your-access-key>
  MINIO_SECRET_KEY: <your-secret-key>
```

Reference it in your Deployment:

```yaml
envFrom:
  - secretRef:
      name: document-service-secret
env:
  - name: MINIO_ENDPOINT
    value: "minio.default.svc.cluster.local:9000"
  - name: MINIO_PUBLIC_HOST
    value: "https://storage.myapp.com"
  - name: MINIO_BUCKET
    value: "app-storage"
  - name: MINIO_USE_SSL
    value: "true"
```

### Why Nginx is required

The `Host` header is part of the AWS SigV4 signature that MinIO validates. If `document-service` signs using `minio:9000` (the internal Docker hostname) but the browser tries to PUT to that URL, the browser cannot resolve `minio:9000` and the request fails.

Nginx gives both the internal service and the browser the same hostname. The signing host and the upload host are identical, so the signature always validates.

In production this role is played by your cloud load balancer, CDN, or Ingress controller — Nginx is not needed as a separate component.