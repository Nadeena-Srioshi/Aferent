"""
app/schemas/symptom.py
──────────────────────
API request/response models for symptom triage.
"""

from __future__ import annotations

from dataclasses import dataclass, field
from datetime import datetime
from typing import Any

from app.models.enums import ConfidenceTier, MedicalCategory


@dataclass(slots=True)
class SymptomRequest:
    symptoms: str
    patient_id: str | None = None


@dataclass(slots=True)
class ClassificationResult:
    category: MedicalCategory
    confidence: float = 0.0


@dataclass(slots=True)
class LLMAnalysisResult:
    confidence_score: float
    suggestions: list[str]
    reasoning: str


@dataclass(slots=True)
class SymptomResponse:
    request_id: str
    category: MedicalCategory
    confidence_score: float
    confidence_tier: ConfidenceTier
    suggestions: list[str]
    reasoning: str
    patient_id: str | None = None
    verification_required: bool = False
    escalate_to_human: bool = False
    workflow_metadata: dict[str, Any] = field(default_factory=dict)
    created_at: datetime = field(default_factory=datetime.utcnow)


@dataclass(slots=True)
class HealthResponse:
    status: str
    version: str
    environment: str
