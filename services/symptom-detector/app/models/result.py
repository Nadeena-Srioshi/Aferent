"""
app/models/result.py
─────────────────────
MongoDB document model for persisted symptom analysis results.
Stored in the `symptom_results` collection with a 90-day TTL index.

Used for:
  - audit trails per patient
  - Phase 2 Kafka event payloads (serialise with .model_dump())
  - LangGraph state hydration
"""

from __future__ import annotations

from datetime import datetime
from typing import Any

from pydantic import BaseModel, Field

from app.models.enums import ConfidenceTier, MedicalCategory


class ResultDocument(BaseModel):
    """
    Mirrors the `symptom_results` MongoDB document.

    Schema:
        {
            "request_id":           "uuid-...",
            "patient_id":           "pid-123" | null,
            "category":             "cardiology",
            "confidence_score":     0.91,
            "confidence_tier":      "high",
            "suggestions":          ["...", "..."],
            "reasoning":            "...",
            "verification_required": false,
            "escalate_to_human":    false,
            "prompt_version":       "1.0.4",
            "workflow_metadata":    {},
            "created_at":           ISODate("...")
        }
    """

    request_id: str
    patient_id: str | None = None
    category: MedicalCategory
    confidence_score: float
    confidence_tier: ConfidenceTier
    suggestions: list[str]
    reasoning: str
    verification_required: bool = False
    escalate_to_human: bool = False
    prompt_version: str = "1.0.0"
    workflow_metadata: dict[str, Any] = Field(default_factory=dict)
    created_at: datetime = Field(default_factory=datetime.utcnow)

    model_config = {"populate_by_name": True}
