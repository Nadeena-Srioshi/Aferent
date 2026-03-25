# Symptom Detector API Endpoints

This file is meant for quick Postman testing **through the API Gateway**.

## Base URL

Use your gateway URL, then append the API prefix:

```text
{{GATEWAY_BASE_URL}}/ai/api/v1
```

Examples below use:

```text
{{GATEWAY_BASE_URL}}/ai/api/v1/prompts
```

## Required Headers

```http
Content-Type: application/json
X-User-Role: ADMIN
```

If your gateway also forwards a user identity header, send it as well:

```http
X-User-Id: admin_user_01
```

---

## 1) List prompt versions

**GET** `/ai/api/v1/prompts`

Optional query params:

- `specialization` = `Cardiology`
- `page` = `1`
- `page_size` = `20`

**Example:**

```http
GET {{GATEWAY_BASE_URL}}/ai/api/v1/prompts?specialization=Cardiology&page=1&page_size=20
```

---

## 2) Create a new specialization prompt

**POST** `/ai/api/v1/prompts`

**Body:**

```json
{
  "specialization": "Cardiology",
  "version": "1.0.4",
  "system_instruction": "You are a specialized Cardiologist. Evaluate symptoms with urgency and return only valid JSON.",
  "author": "admin_auth_id",
  "updated_by": "admin_user_01"
}
```

**Example:**

```http
POST {{GATEWAY_BASE_URL}}/ai/api/v1/prompts
```

---

## 3) Seed default specialization prompts

**POST** `/ai/api/v1/prompts/seed`

**Body:** none

**Example:**

```http
POST {{GATEWAY_BASE_URL}}/ai/api/v1/prompts/seed
```

---

## 4) Get the active prompt for a specialization

**GET** `/ai/api/v1/prompts/{specialization}/active`

**Example:**

```http
GET {{GATEWAY_BASE_URL}}/ai/api/v1/prompts/Cardiology/active
```

---

## 5) List versions for a specialization

**GET** `/ai/api/v1/prompts/{specialization}/versions`

**Example:**

```http
GET {{GATEWAY_BASE_URL}}/ai/api/v1/prompts/Cardiology/versions?page=1&page_size=20
```

---

## 6) Get a specific version

**GET** `/ai/api/v1/prompts/{specialization}/versions/{version}`

**Example:**

```http
GET {{GATEWAY_BASE_URL}}/ai/api/v1/prompts/Cardiology/versions/1.0.4
```

---

## 7) Update a version

**PATCH** `/ai/api/v1/prompts/{specialization}/versions/{version}`

**Body:**

```json
{
  "system_instruction": "You are a specialized Cardiologist. Prioritize chest pain, left arm radiation, and diaphoresis.",
  "updated_by": "admin_user_01"
}
```

**Example:**

```http
PATCH {{GATEWAY_BASE_URL}}/ai/api/v1/prompts/Cardiology/versions/1.0.4
```

---

## 8) Activate a version

**POST** `/ai/api/v1/prompts/{specialization}/versions/{version}/activate`

**Body:**

```json
{
  "is_active": true,
  "updated_by": "admin_user_01"
}
```

To deactivate:

```json
{
  "is_active": false,
  "updated_by": "admin_user_01"
}
```

**Example:**

```http
POST {{GATEWAY_BASE_URL}}/ai/api/v1/prompts/Cardiology/versions/1.0.4/activate
```

---

## 9) Delete a version

**DELETE** `/ai/api/v1/prompts/{specialization}/versions/{version}`

**Example:**

```http
DELETE {{GATEWAY_BASE_URL}}/ai/api/v1/prompts/Cardiology/versions/1.0.4
```

---

## Suggested Postman variables

Set these in your Postman environment:

- `GATEWAY_BASE_URL` = `https://your-gateway-host.example.com`
- `USER_ROLE` = `ADMIN`
- `USER_ID` = `admin_user_01`

Then reuse them in request headers:

```http
X-User-Role: {{USER_ROLE}}
X-User-Id: {{USER_ID}}
```
