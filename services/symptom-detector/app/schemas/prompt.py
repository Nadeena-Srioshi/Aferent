"""
app/schemas/prompt.py
──────────────────────
Pydantic schemas for the admin prompt management API.
Decoupled from the internal PromptDocument model so the API surface
can evolve independently of the DB schema.
"""

from __future__ import annotations

from datetime import datetime
from typing import Any

from pydantic import BaseModel, Field, field_validator

from app.models.enums import MedicalCategory


# ── Create ─────────────────────────────────────────────────────────────────────

class PromptCreateRequest(BaseModel):
    """Body for POST /api/v1/admin/prompts"""

    category: MedicalCategory
    system_prompt: str = Field(
        ...,
        min_length=20,
        description="System-level instruction sent to the LLM.",
        examples=["You are an expert cardiology triage assistant. Return ONLY valid JSON."],
    )
    user_template: str = Field(
        ...,
        min_length=10,
        description="User message template. Must contain the literal string {symptoms}.",
        examples=["Patient symptoms: {symptoms}\n\nRespond with confidence_score, suggestions, reasoning."],
    )
    metadata: dict[str, Any] = Field(
        default_factory=dict,
        description="Arbitrary key-value metadata (author, reviewer, ticket ref, etc.)",
    )

    @field_validator("user_template")
    @classmethod
    def must_contain_symptoms_placeholder(cls, v: str) -> str:
        if "{symptoms}" not in v:
            raise ValueError("user_template must contain the '{symptoms}' placeholder.")
        return v


# ── Update ─────────────────────────────────────────────────────────────────────

class PromptUpdateRequest(BaseModel):
    """Body for PATCH /api/v1/admin/prompts/{category}/versions/{version}"""

    system_prompt: str | None = Field(default=None, min_length=20)
    user_template: str | None = Field(default=None, min_length=10)
    metadata: dict[str, Any] | None = None

    @field_validator("user_template")
    @classmethod
    def must_contain_symptoms_placeholder(cls, v: str | None) -> str | None:
        if v is not None and "{symptoms}" not in v:
            raise ValueError("user_template must contain the '{symptoms}' placeholder.")
        return v


# ── Activate / Deactivate ──────────────────────────────────────────────────────

class PromptActivateRequest(BaseModel):
    """Body for POST /api/v1/admin/prompts/{category}/versions/{version}/activate"""
    active: bool = Field(..., description="Set to true to activate, false to deactivate.")


# ── Response ───────────────────────────────────────────────────────────────────

class PromptResponse(BaseModel):
    """Returned for all prompt read/write operations."""

    category: MedicalCategory
    version: int
    active: bool
    system_prompt: str
    user_template: str
    metadata: dict[str, Any]
    created_at: datetime
    updated_at: datetime


class PromptVersionSummary(BaseModel):
    """Lightweight item for list endpoints."""

    category: MedicalCategory
    version: int
    active: bool
    updated_at: datetime
    metadata: dict[str, Any]
