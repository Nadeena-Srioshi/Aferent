# Doctor Service API Endpoints

This document lists the main doctor-service endpoints, the expected IDs/headers, whether the route is public or private, and the typical request/response payloads.

## ID conventions

- `authId`: the authenticated user ID from the auth system.
  - Used in request body for initial registration profile submission.
  - Also injected by the gateway as `X-User-ID` for authenticated requests.
- `doctorId`: the doctor-service identifier, e.g. `DOC_001`.
  - Used in path parameters for profile, prescription, and signed URL endpoints.
- `X-User-ID`: header injected by the gateway from the access token subject.
  - For doctor routes, this is the doctor’s `authId`.
- `X-User-Role`: header injected by the gateway from the access token role.
  - Used for admin/private access checks.
- `X-Patient-ID`: optional header used on the prescription QR signed-url endpoint when the caller is a patient.

## Error response format

When an error occurs, the service returns a JSON payload like this:

```json
{
  "status": 403,
  "error": "Forbidden",
  "message": "You can only access your own license document",
  "timestamp": "2026-04-16T15:00:00"
}
```

Common status codes:
- `400 Bad Request` — invalid payload or invalid query parameter
- `403 Forbidden` — authenticated, but not allowed
- `404 Not Found` — resource not found
- `500 Internal Server Error` — unexpected error

---

## 1) Register doctor profile

### `POST /doctors/register/profile`

**Access:** Public through the gateway

**Expected ID:** `authId` in the JSON body

**Request body:**

```json
{
  "authId": "auth-123",
  "firstName": "Asha",
  "lastName": "Rao",
  "phone": "+91-9876543210",
  "specialization": "Cardiology",
  "licenseNumber": "LIC-2026-001",
  "yearsOfExperience": 8,
  "qualifications": ["MBBS", "MD"]
}
```

**Success response:** `200 OK`

Returns the created/updated `Doctor` object.

Example:

```json
{
  "id": "...",
  "authId": "auth-123",
  "doctorId": "DOC_001",
  "email": "doctor@example.com",
  "firstName": "Asha",
  "lastName": "Rao",
  "phone": "+91-9876543210",
  "specialization": "Cardiology",
  "licenseNumber": "LIC-2026-001",
  "licenseDocKey": null,
  "yearsOfExperience": 8,
  "qualifications": ["MBBS", "MD"],
  "bio": null,
  "profilePicKey": null,
  "profilePicUrl": null,
  "hospitals": null,
  "consultationFee": null,
  "languages": null,
  "status": "PENDING_VERIFICATION",
  "createdAt": "2026-04-16T15:00:00",
  "updatedAt": "2026-04-16T15:00:00"
}
```

---

## 2) Get license upload URL

### `POST /doctors/license-upload-url`

**Access:** Public

**Expected ID:** `authId` in the JSON body

**Request body:**

```json
{
  "authId": "auth-123",
  "fileName": "license.pdf"
}
```

**Headers:**
- No JWT required for this endpoint

**Example request:**

`POST /doctors/license-upload-url`

**Success response:** `200 OK`

```json
{
  "uploadUrl": "http://...presigned-put-url...",
  "objectKey": "doctor-license/DOC_001/license.pdf"
}
```

**Notes:**
- The route does **not** take `doctorId`.
- The route does **not** take `authId` in query/header.
- The backend resolves the doctor from body `authId`.

**Possible errors:**
- `404 Not Found` if doctor profile for `authId` does not exist
- `400 Bad Request` if `authId` or `fileName` is missing

---

## 3) Get doctor profile

### `GET /doctors/{doctorId}`

**Access:** Public

**Expected ID:** `doctorId` in the path

**Success response:** `200 OK`

Returns a `Doctor` object.

Example response shape:

```json
{
  "doctorId": "DOC_001",
  "firstName": "Asha",
  "lastName": "Rao",
  "specialization": "Cardiology",
  "status": "ACTIVE"
}
```

---

## 4) Update doctor profile

### `PUT /doctors/{doctorId}/profile`

**Access:** Private

**Expected ID:**
- `doctorId` in the path
- `X-User-ID` must match the doctor’s `authId`

**Request body:**

```json
{
  "firstName": "Asha",
  "lastName": "Rao",
  "phone": "+91-9999999999",
  "bio": "Cardiologist with 8 years of experience",
  "hospitalIds": ["HOSP_001", "HOSP_002"],
  "languages": ["English", "Hindi"],
  "yearsOfExperience": 9,
  "qualifications": ["MBBS", "MD", "DM"]
}
```

**Success response:** `200 OK`

Returns the updated `Doctor` object.

**Possible errors:**
- `403 Forbidden` if `X-User-ID` does not match the doctor
- `403 Forbidden` if the doctor is not ACTIVE
- `400 Bad Request` if a hospital ID is invalid
- `404 Not Found` if the doctor does not exist

---

## 5) Get profile picture upload URL

### `POST /doctors/profile/pic-upload-url`

**Access:** Private

**Expected ID:** `X-User-ID` header must contain the authenticated doctor’s `authId`

**Query params:**
- `fileName` — required, for example `profile.png`

**Success response:** `200 OK`

```json
{
  "uploadUrl": "http://...presigned-put-url...",
  "permanentUrl": "http://...public-object-url..."
}
```

**Notes:**
- The route does **not** take `doctorId`.
- The backend stores the public `permanentUrl` for the doctor profile.

**Possible errors:**
- `404 Not Found` if the doctor does not exist
- `403 Forbidden` if the doctor is not ACTIVE
- `400 Bad Request` if `fileName` is not an image name

---

## 6) Get public profile picture URL

### `GET /doctors/{doctorId}/profile/pic-url`

**Access:** Public

**Expected ID:** `doctorId` in the path

**Success response:** `200 OK`

```json
{
  "url": "http://...public-profile-picture-url..."
}
```

**Possible errors:**
- `404 Not Found` if the doctor does not exist or no profile picture exists

---

## 7) Get signed URL for private license document

### `GET /doctors/{doctorId}/license/signed-url`

**Access:** Private

**Expected ID:**
- `doctorId` in the path
- `X-User-ID` must be the doctor’s `authId` unless the caller is admin
- `X-User-Role` must be `DOCTOR` or `ADMIN`

**Query params:**
- `expires` — optional, default `3600`

**Success response:** `200 OK`

```json
{
  "url": "http://...presigned-download-url..."
}
```

**Access rules:**
- `ADMIN` can access any doctor license
- `DOCTOR` can access only their own license
- Everyone else gets `403 Forbidden`

**Possible errors:**
- `404 Not Found` if the doctor or license document does not exist
- `403 Forbidden` if the caller is not allowed
- `400 Bad Request` if `expires` is invalid

---

## 8) Issue prescription

### `POST /doctors/{doctorId}/prescriptions`

**Access:** Private

**Expected ID:**
- `doctorId` in the path
- `X-User-ID` must be the issuing doctor’s `authId`

**Request body:**

```json
{
  "appointmentId": "APT_1001",
  "patientId": "PAT_2001",
  "patientName": "Rahul Sharma",
  "patientAge": 34,
  "patientPhone": "+91-9000000000",
  "patientEmail": "rahul@example.com",
  "consultationType": "IN_PERSON",
  "hospitalId": "HOSP_001",
  "diagnosis": "Hypertension",
  "symptoms": "Headache and dizziness",
  "medications": [
    {
      "name": "Amlodipine",
      "dosage": "5mg",
      "frequency": "once daily",
      "duration": "30 days",
      "instructions": "after breakfast"
    }
  ],
  "notes": "Monitor BP daily",
  "followUpDate": "2026-04-30"
}
```

**Success response:** `200 OK`

```json
{
  "message": "Prescription issued successfully. Patient notified via SMS and email.",
  "prescription": {
    "prescriptionId": "RX_0001",
    "doctorId": "DOC_001",
    "patientId": "PAT_2001",
    "qrCodeKey": "prescription-qr/RX_0001/RX_0001.png"
  }
}
```

**Possible errors:**
- `403 Forbidden` if the caller is not the issuing doctor
- `400 Bad Request` if consultation type, hospital, or medication data is invalid
- `404 Not Found` if the doctor or appointment references are invalid

---

## 9) List prescriptions issued by a doctor

### `GET /doctors/{doctorId}/prescriptions`

**Access:** Private

**Expected ID:**
- `doctorId` in the path
- `X-User-ID` must match the issuing doctor’s `authId`

**Success response:** `200 OK`

Returns a list of `Prescription` objects.

---

## 10) List patients for a doctor

### `GET /doctors/{doctorId}/patients`

**Access:** Private

**Expected ID:**
- `doctorId` in the path
- `X-User-ID` must match the doctor’s `authId`

**Success response:** `200 OK`

Returns a list of `PatientVisitSummary` objects.

Example:

```json
[
  {
    "patientId": "PAT_2001",
    "patientName": "Rahul Sharma",
    "patientAge": 34,
    "visitCount": 3,
    "lastVisitDate": "2026-04-16T15:00:00"
  }
]
```

---

## 11) List prescriptions for a patient from a doctor

### `GET /doctors/{doctorId}/patients/{patientId}/prescriptions`

**Access:** Private

**Expected ID:**
- `doctorId` in the path
- `patientId` in the path
- `X-User-ID` must match the doctor’s `authId`

**Success response:** `200 OK`

Returns a list of `Prescription` objects.

---

## 12) Public prescription view

### `GET /prescriptions/{prescriptionId}/view`

**Access:** Public

**Expected ID:** `prescriptionId` in the path

**Success response:** `200 OK`

Returns a `PrescriptionPublicView` object.

Example:

```json
{
  "prescriptionId": "RX_0001",
  "patientName": "Rahul Sharma",
  "patientAge": 34,
  "doctorName": "Dr. Asha Rao",
  "doctorSpecialization": "Cardiology",
  "hospitalName": "Apollo Hospital",
  "diagnosis": "Hypertension",
  "symptoms": "Headache and dizziness",
  "medications": [],
  "notes": "Monitor BP daily",
  "followUpDate": "2026-04-30",
  "issuedAt": "2026-04-16T15:00:00"
}
```

---

## 13) Get prescription QR signed URL

### `GET /prescriptions/{prescriptionId}/qr/signed-url`

**Access:** Private

**Expected ID:**
- `prescriptionId` in the path
- `X-User-ID` must be either:
  - the issuing doctor’s `authId`, or
  - the receiving patient’s ID
- `X-User-Role` must be `DOCTOR` or `PATIENT`
- `X-Patient-ID` is optional and can help confirm patient identity

**Query params:**
- `expires` — optional, default `3600`

**Success response:** `200 OK`

```json
{
  "url": "http://...presigned-download-url..."
}
```

**Access rules:**
- `DOCTOR` can access only if they issued the prescription
- `PATIENT` can access only if they are the receiving patient
- Any other role gets `403 Forbidden`

**Possible errors:**
- `404 Not Found` if the prescription or QR document does not exist
- `403 Forbidden` if the caller is not allowed
- `400 Bad Request` if `expires` is invalid

---

## Quick access summary

### Public
- `POST /doctors/register/profile`
- `POST /doctors/license-upload-url`
- `GET /doctors/{doctorId}`
- `GET /doctors/{doctorId}/profile/pic-url`
- `GET /prescriptions/{prescriptionId}/view`

### Private
- `PUT /doctors/{doctorId}/profile`
- `POST /doctors/profile/pic-upload-url`
- `GET /doctors/{doctorId}/license/signed-url`
- `POST /doctors/{doctorId}/prescriptions`
- `GET /doctors/{doctorId}/prescriptions`
- `GET /doctors/{doctorId}/patients`
- `GET /doctors/{doctorId}/patients/{patientId}/prescriptions`
- `GET /prescriptions/{prescriptionId}/qr/signed-url`
