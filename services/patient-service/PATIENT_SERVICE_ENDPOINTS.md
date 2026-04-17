# Patient Service API Endpoints

This document lists **all current `patient-service` REST endpoints** from:
- `PatientController`
- `MedicalHistoryController`

---

## Authentication & Headers

`patient-service` expects identity headers injected by API Gateway:

- `X-User-ID`: Auth user ID (auth-service user id)
- `X-User-Role`: `PATIENT` | `DOCTOR` | `ADMIN`

---

## ID Semantics (Important)

- `authId` → value in `X-User-ID` (identity from auth-service)
- `patientId` → human-readable patient id, e.g. `PAT_001` (used in path `/patients/{patientId}`)
- `documentId` → id of a document in `patient_documents`
- `appointmentId` → id of appointment used in share grants
- `doctorId` → domain doctor id (e.g. `DOC_001`)
- `doctorAuthId` → doctor auth identity (what doctor sends as `X-User-ID`)

---

## Role Access Matrix (Quick View)

| Endpoint Pattern | PATIENT | DOCTOR | ADMIN |
|---|---:|---:|---:|
| `GET /patients/{patientId}` | Own only | ✅ | ✅ |
| `GET /patients/me` | ✅ | ❌ | ❌ |
| `PUT /patients/{patientId}` | Own only | ❌ | ❌ |
| `POST /patients/{patientId}/documents/upload-url` | Own only | ❌ | ❌ |
| `POST /patients/me/documents/upload-url` | ✅ (patient profile required) | ⚠️ (not role-checked in controller) | ⚠️ |
| `POST /patients/{patientId}/documents` | Own only | ❌ | ❌ |
| `POST /patients/me/documents` | ✅ (patient profile required) | ⚠️ | ⚠️ |
| `GET /patients/{patientId}/documents` | Own only | ✅ (filtered by `patient_doctor_access`) | ✅ |
| `GET /patients/me/documents` | ✅ | ❌ | ❌ |
| `GET /patients/{patientId}/medical-reports` | Own only | ❌ | ❌ |
| `GET /patients/me/medical-reports` | ✅ | ❌ | ❌ |
| `GET /patients/{patientId}/documents/{documentId}/download-url` | Own only | ✅ (must have explicit doc permission) | ✅ |
| `GET /patients/me/documents/{documentId}/download-url` | ✅ | ❌ | ❌ |
| `POST /patients/{patientId}/documents/share` | Own only | ❌ | ❌ |
| `DELETE /patients/{patientId}/documents/{documentId}` | Own only | ❌ | ❌ |
| `GET /patients/{patientId}/medical-history` | Own only | ✅ | ✅ |
| `GET /patients/me/medical-history` | ✅ | ❌ | ❌ |
| `GET /patients/{patientId}/medical-history/records` | Own only | ✅ | ✅ |
| `GET /patients/me/medical-history/records` | ✅ | ❌ | ❌ |
| `GET /patients/{patientId}/medical-history/count` | Own only | ✅ | ✅ |
| `GET /patients/me/medical-history/count` | ✅ | ❌ | ❌ |

> ⚠️ `me` upload/save routes currently derive patient from `X-User-ID` and don’t explicitly check role in controller.

---

## Common Error Response (Example)

```json
{
  "timestamp": "2026-04-17T03:10:00.123",
  "status": 403,
  "error": "Forbidden",
  "message": "Access denied",
  "path": "/patients/PAT_001/documents",
  "correlationId": "3e2e5c0d-..."
}
```

Validation errors include `validationErrors` object.

---

## Endpoints

## 1) Profile Endpoints

### GET `/patients/{patientId}`
Get patient profile by patient id.

**Access:**
- PATIENT: only own profile
- DOCTOR/ADMIN: allowed

**Example request:**
- Headers: `X-User-ID: auth_patient_123`, `X-User-Role: PATIENT`
- Path: `/patients/PAT_001`

**Example response (200):**
```json
{
  "id": "67f4...",
  "authId": "auth_patient_123",
  "patientId": "PAT_001",
  "email": "patient@example.com",
  "firstName": "Asha",
  "lastName": "Perera",
  "phone": "+94770000000",
  "dateOfBirth": "1998-02-21",
  "gender": "FEMALE",
  "bloodGroup": "O+",
  "address": {
    "street": "12 Main St",
    "city": "Colombo",
    "country": "Sri Lanka"
  },
  "createdAt": "2026-04-10T09:30:00",
  "updatedAt": "2026-04-15T13:22:00"
}
```

### GET `/patients/me`
Get current patient profile from `X-User-ID`.

**Access:** PATIENT only

**Example response (200):** same shape as above.

### PUT `/patients/{patientId}`
Update own profile.

**Access:** PATIENT owning that `patientId`

**Request body (`UpdateProfileRequest`):**
```json
{
  "firstName": "Asha",
  "lastName": "Perera",
  "phone": "+94770000000",
  "dateOfBirth": "1998-02-21",
  "gender": "FEMALE",
  "bloodGroup": "O+",
  "address": {
    "street": "12 Main St",
    "city": "Colombo",
    "country": "Sri Lanka"
  }
}
```

**Response (200):** updated `Patient` object.

---

## 2) Document Upload & Metadata Endpoints

### POST `/patients/{patientId}/documents/upload-url`
Generate upload URL for a document.

**Access:** PATIENT owning that `patientId`

**Query params:**
- `fileName` (required)
- `contentType` (required)
- `documentType` (optional; defaults via enum parser)

**Example response (`PresignedUrlResponse`):**
```json
{
  "uploadUrl": "http://localhost:9000/app-storage/...signed...",
  "permanentUrl": null
}
```

### POST `/patients/me/documents/upload-url`
Generate upload URL for current user.

**Access:** Intended for PATIENT with existing patient profile

**Query params:**
- `fileName` (required)
- `contentType` (required)
- `documentType` (optional)
- `displayName` (optional)

**Response:** `PresignedUrlResponse`

### POST `/patients/{patientId}/documents`
Save document metadata after upload.

**Access:** PATIENT owning that `patientId`

**Request body (`DocumentMetadataRequest`):**
```json
{
  "documentId": "2f4b10a4-5bb1-4dc7-a30e-cc8d3f4f6eb8",
  "minioKey": "patient-service/private/medical-reports/PAT_001/report-1.pdf",
  "permanentUrl": null,
  "visibility": "private",
  "category": "medical-reports/PAT_001",
  "originalFileName": "blood-report-apr-2026.pdf",
  "displayName": "Blood test - April 2026",
  "contentType": "application/pdf",
  "fileSize": 245678,
  "documentType": "MEDICAL_REPORT"
}
```

**Response (200):** saved `PatientDocument`

### POST `/patients/me/documents`
Save metadata for current user.

**Access:** Intended for PATIENT with existing profile

**Body/response:** same as above.

---

## 3) Document Read Endpoints

### GET `/patients/{patientId}/documents`
List patient documents.

**Access:**
- PATIENT: own docs only
- DOCTOR: only docs granted via `patient_doctor_access.allowedDocumentIds`
- ADMIN: all patient docs

**Query params:**
- `documentType` (optional)

**Example response (200):** `PatientDocument[]`
```json
[
  {
    "id": "doc_001",
    "patientId": "PAT_001",
    "fileName": "blood-report-apr-2026.pdf",
    "originalFileName": "blood-report-apr-2026.pdf",
    "displayName": "Blood test - April 2026",
    "contentType": "application/pdf",
    "fileSize": 245678,
    "minioKey": "patient-service/private/medical-reports/PAT_001/blood-report-apr-2026.pdf",
    "permanentUrl": null,
    "visibility": "private",
    "category": "medical-reports/PAT_001",
    "documentType": "MEDICAL_REPORT",
    "uploadStatus": "UPLOADED",
    "requestedAt": "2026-04-15T08:00:00",
    "expiresAt": "2026-04-16T08:00:00",
    "deleted": false,
    "uploadedAt": "2026-04-15T08:01:30"
  }
]
```

> `expiresAt` above is upload-url lifecycle metadata, **not** doctor access control.

### GET `/patients/me/documents`
List current patient documents.

**Access:** PATIENT

**Query params:** `documentType` optional

### GET `/patients/{patientId}/medical-reports`
Lightweight list for medical reports only.

**Access:** PATIENT owning that `patientId`

**Response (`MedicalReportSummaryResponse[]`):**
```json
[
  {
    "documentId": "doc_001",
    "originalFileName": "blood-report-apr-2026.pdf",
    "displayName": "Blood test - April 2026",
    "uploadedAt": "2026-04-15T08:01:30",
    "contentType": "application/pdf",
    "documentType": "MEDICAL_REPORT"
  }
]
```

### GET `/patients/me/medical-reports`
Lightweight medical report list for current patient.

**Access:** PATIENT

**Response:** same as above.

### GET `/patients/{patientId}/documents/{documentId}/download-url`
Get presigned download URL.

**Access:**
- PATIENT: own doc only
- DOCTOR: must have explicit permission in `patient_doctor_access`
- ADMIN: allowed

**Response:**
```json
{
  "downloadUrl": "http://localhost:9000/app-storage/...signed..."
}
```

### GET `/patients/me/documents/{documentId}/download-url`
Get download URL for current patient document.

**Access:** PATIENT

**Response:** same shape as above.

---

## 4) Document Sharing (Patient -> Doctor)

### POST `/patients/{patientId}/documents/share`
Patient grants a doctor access to selected documents.

**Access:** PATIENT owning that `patientId`

**Request body (`ShareDocumentsRequest`):**
```json
{
  "appointmentId": "APT_12345",
  "doctorId": "DOC_001",
  "doctorAuthId": "auth_doctor_456",
  "documentIds": ["doc_001", "doc_003"],
  "expiresAt": "2026-04-30T23:59:59"
}
```

**Response (200):** `PatientDoctorAccess`
```json
{
  "id": "acc_001",
  "patientId": "PAT_001",
  "doctorId": "DOC_001",
  "doctorAuthId": "auth_doctor_456",
  "appointmentId": "APT_12345",
  "allowedDocumentIds": ["doc_001", "doc_003"],
  "active": true,
  "grantedAt": "2026-04-17T03:05:00",
  "expiresAt": "2026-04-30T23:59:59",
  "revokedAt": null,
  "createdAt": "2026-04-17T03:05:00",
  "updatedAt": "2026-04-17T03:05:00"
}
```

---

## 5) Delete Endpoint

### DELETE `/patients/{patientId}/documents/{documentId}`
Soft-delete a patient document.

**Access:** PATIENT owning that `patientId`

**Response:** `204 No Content`

---

## 6) Medical History Endpoints

### GET `/patients/{patientId}/medical-history`
Get full medical history document.

**Access:**
- PATIENT: own only
- DOCTOR/ADMIN: allowed

**Response (200):** `MedicalHistory`
```json
{
  "id": "mh_001",
  "patientId": "PAT_001",
  "records": [
    {
      "prescriptionId": "RX_1001",
      "appointmentId": "APT_12345",
      "consultationType": "ONLINE",
      "doctorId": "DOC_001",
      "doctorName": "Dr. Silva",
      "doctorSpecialization": "Cardiology",
      "hospitalId": "HSP_01",
      "hospitalName": "Aferent General",
      "patientId": "PAT_001",
      "patientName": "Asha Perera",
      "patientAge": 27,
      "patientPhone": "+94770000000",
      "patientEmail": "patient@example.com",
      "diagnosis": "Mild hypertension",
      "symptoms": ["headache", "fatigue"],
      "medications": [
        { "name": "Drug A", "dosage": "5mg", "duration": "7 days" }
      ],
      "notes": "Reduce salt intake",
      "followUpDate": "2026-05-01",
      "qrCodeKey": "prescriptions/PAT_001/RX_1001-qr.png",
      "issuedAt": "2026-04-16T10:00:00",
      "recordedAt": "2026-04-16T10:00:01"
    }
  ],
  "createdAt": "2026-04-16T10:00:01",
  "updatedAt": "2026-04-16T10:00:01"
}
```

### GET `/patients/me/medical-history`
Get current patient’s medical history.

**Access:** PATIENT only

### GET `/patients/{patientId}/medical-history/records`
Get only `records[]` from medical history.

**Access:** PATIENT own only, DOCTOR/ADMIN allowed

### GET `/patients/me/medical-history/records`
Get current patient records array.

**Access:** PATIENT only

### GET `/patients/{patientId}/medical-history/count`
Get count of medical records.

**Access:** PATIENT own only, DOCTOR/ADMIN allowed

**Response:**
```json
12
```

### GET `/patients/me/medical-history/count`
Get current patient medical record count.

**Access:** PATIENT only

**Response:**
```json
12
```

---

## Notes

1. **Doctor access to documents is grant-based** (`patient_doctor_access`) and independent from document upload URL expiry metadata.
2. For `me` routes, `patientId` is derived by `X-User-ID -> patient profile` lookup.
3. `documentType` enum currently includes: `PROFILE_IMAGE`, `MEDICAL_REPORT`, `PRESCRIPTION`, `QR_CODE`, `SCAN`.
