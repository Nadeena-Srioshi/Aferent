# AI Symptom Service — Phase 1

> FastAPI · Motor (async MongoDB) · OpenAI-compatible LLM · Docker Compose

---

## Architecture

```
POST /ai/api/v1/symptoms/analyse
            │
            ▼
   ┌─────────────────────┐
   │  CategoryRouter      │  ← keyword/rule-based (Phase 2: embedding classifier)
   └────────┬────────────┘
            │  ClassificationResult
            ▼
   ┌─────────────────────┐
   │  PromptRepository    │  ← MongoDB: fetch active versioned prompt
   └────────┬────────────┘
            │  PromptDocument
            ▼
   ┌─────────────────────┐
   │  LLMExecutor         │  ← render template → call LLM → parse JSON
   └────────┬────────────┘
            │  LLMAnalysisResult { confidence_score, suggestions, reasoning }
            ▼
   ┌─────────────────────┐
   │  ResponseHandler     │  ← 3-tier confidence logic (pure function)
   └────────┬────────────┘
            │  SymptomResponse
            ▼
        HTTP 200 JSON
```

### Confidence Tiers

| Score     | Tier   | Flags set               |
|-----------|--------|-------------------------|
| > 0.8     | HIGH   | *(none)*                |
| 0.5 – 0.8 | MEDIUM | `verification_required` |
| < 0.5     | LOW    | `escalate_to_human`     |

---

## Project Structure

```
symptom-detector/
├── app/
│   ├── api/
│   │   └── v1/
│   │       ├── endpoints/
│   │       │   ├── health.py       # GET /api/v1/health
│   │       │   └── symptoms.py     # POST /api/v1/symptoms/analyse
│   │       └── router.py
│   ├── core/
│   │   ├── config.py               # Settings (pydantic-settings)
│   │   ├── exceptions.py           # Domain exception hierarchy
│   │   └── logging.py              # structlog structured logging
│   ├── db/
│   │   ├── indexes.py              # MongoDB index management
│   │   ├── mongo.py                # Motor connection lifecycle
│   │   └── prompt_repository.py   # Prompts data-access object
│   ├── models/
│   │   ├── enums.py                # MedicalCategory, ConfidenceTier
│   │   └── prompt.py               # PromptDocument (MongoDB model)
│   ├── schemas/
│   │   └── symptom.py              # API request/response schemas
│   ├── services/
│   │   ├── llm/
│   │   │   ├── client.py           # OpenAI-compatible async client
│   │   │   └── executor.py         # Prompt render → LLM → parse
│   │   └── symptom/
│   │       ├── category_router.py  # Symptom → MedicalCategory
│   │       ├── orchestrator.py     # Top-level pipeline coordinator
│   │       └── response_handler.py # 3-tier confidence logic
│   ├── utils/
│   │   ├── dependencies.py         # FastAPI Depends() factories
│   │   └── exception_handlers.py  # Domain exc → HTTP response
│   └── main.py                     # App factory + lifespan
├── scripts/
│   └── mongo-init.js               # MongoDB seed (8 prompt documents)
├── tests/
│   ├── unit/
│   │   ├── test_category_router.py
│   │   └── test_response_handler.py
│   └── integration/
│       └── test_symptom_endpoint.py
├── docker-compose.yml
├── Dockerfile
├── requirements.txt
├── pytest.ini
└── .env.example
```

For prompt management testing, see [`API_ENDPOINTS.md`](API_ENDPOINTS.md). It includes Postman-ready payloads and gateway headers for the `/ai/api/v1/prompts` routes.

---

## Quick Start

```bash
# 1. Clone and configure
cp .env.example .env
# Edit .env → set LLM_API_KEY

# 2. Start all services
docker compose up --build

# 3. (Optional) Start with Mongo Express UI
docker compose --profile dev up --build

# API docs available at:
#   http://localhost:8000/docs
# Mongo Express at:
#   http://localhost:8081  (admin / admin123)
```

---

## Example Request

```bash
curl -X POST http://localhost:8000/api/v1/symptoms/analyse \
  -H "Content-Type: application/json" \
  -d '{
    "symptoms": "I have chest pain radiating to my left arm and I am sweating.",
    "patient_id": "patient-001"
  }'
```

### Example Response (HIGH tier)

```json
{
  "request_id": "a3f1c2d4-...",
  "patient_id": "patient-001",
  "category": "cardiology",
  "confidence_score": 0.91,
  "confidence_tier": "high",
  "suggestions": [
    "Call emergency services immediately (potential cardiac event)",
    "Chew aspirin 300mg if not contraindicated",
    "Sit or lie down and rest"
  ],
  "reasoning": "Classic presentation of acute coronary syndrome.",
  "verification_required": false,
  "escalate_to_human": false,
  "workflow_metadata": {}
}
```

---

## Running Tests

```bash
# Inside the container
docker compose exec api pytest

# Or locally (with venv)
pip install -r requirements.txt
pytest
```

---

## Phase 2 Integration Points

All extension points are pre-marked with `NOTE (Phase 2 – ...)` comments.

| Component     | File                                    | What to do                                    |
|---------------|-----------------------------------------|-----------------------------------------------|
| **Kafka**     | `services/symptom/orchestrator.py`      | Publish event after classify; consume async   |
| **LangGraph** | `services/llm/executor.py`              | Replace `chat_complete` with `graph.invoke()` |
| **Kafka**     | `core/config.py`                        | Uncomment `kafka_*` + add producer/consumer   |
| **Caching**   | `db/prompt_repository.py`               | Add Redis layer before MongoDB fallback        |
| **Persistence**| `services/symptom/orchestrator.py`     | Add `result_repo.save(response)` after handle |

---

## MongoDB Prompt Schema

```json
{
  "specialization": "Cardiology",
  "version":        "1.0.4",
  "is_active":       true,
  "system_instruction": "You are a specialized Cardiologist...",
  "author":         "admin_user_01",
  "updated_by":     "admin_user_01",
  "created_at":     "2026-04-14T00:00:00Z",
  "updated_at":     "2026-04-14T00:00:00Z"
}
```

To roll out a new prompt version: insert a document with a new semantic `version` (for example `1.0.5`) and set `is_active: true` on the new one after deactivating the previous active version for that specialization.