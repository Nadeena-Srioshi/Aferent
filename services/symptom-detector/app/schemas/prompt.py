"""
app/schemas/prompt.py
──────────────────────
Pydantic schemas for specialization prompt management.
"""

from __future__ import annotations

from datetime import datetime
import re

from pydantic import BaseModel, Field, field_validator


_SEMVER_PATTERN = re.compile(r"^\d+\.\d+\.\d+(?:[-+][0-9A-Za-z.-]+)?$")


# ── Create ─────────────────────────────────────────────────────────────────────

class PromptCreateRequest(BaseModel):
    """Body for POST /api/v1/prompts"""

    specialization: str = Field(..., min_length=2)
    version: str = Field(..., min_length=5)
    system_instruction: str = Field(
        ...,
        min_length=20,
        description="System-level instruction sent to the LLM.",
        examples=["You are an expert cardiology triage assistant. Return ONLY valid JSON."],
    )
    author: str = Field(..., min_length=1)
    updated_by: str | None = Field(default=None)

    @field_validator("specialization")
    @classmethod
    def normalize_specialization(cls, value: str) -> str:
        cleaned = " ".join(value.strip().replace("_", " ").split())
        if not cleaned:
            raise ValueError("specialization cannot be empty.")
        return cleaned.title()

    @field_validator("version")
    @classmethod
    def validate_version(cls, value: str) -> str:
        version = value.strip()
        if not _SEMVER_PATTERN.fullmatch(version):
            raise ValueError("version must be a semantic version like '1.0.4'.")
        return version

    @field_validator("system_instruction")
    @classmethod
    def validate_system_instruction(cls, value: str) -> str:
        instruction = value.strip()
        if len(instruction) < 20:
            raise ValueError("system_instruction must be at least 20 characters long.")
        return instruction


# ── Update ─────────────────────────────────────────────────────────────────────

class PromptUpdateRequest(BaseModel):
    """Body for PATCH /api/v1/prompts/{specialization}/versions/{version}"""

    system_instruction: str | None = Field(default=None, min_length=20)
    updated_by: str = Field(..., min_length=1)

    @field_validator("system_instruction")
    @classmethod
    def validate_system_instruction(cls, value: str | None) -> str | None:
        if value is not None and len(value.strip()) < 20:
            raise ValueError("system_instruction must be at least 20 characters long.")
        return value


# ── Activate / Deactivate ──────────────────────────────────────────────────────

class PromptActivateRequest(BaseModel):
    """Body for POST /api/v1/prompts/{specialization}/versions/{version}/activate"""
    is_active: bool = Field(..., description="Set to true to activate, false to deactivate.")
    updated_by: str = Field(..., min_length=1)


# ── Response ───────────────────────────────────────────────────────────────────

class PromptResponse(BaseModel):
    """Returned for all prompt read/write operations."""

    specialization: str
    version: str
    is_active: bool
    system_instruction: str
    author: str
    updated_by: str
    created_at: datetime
    updated_at: datetime


class PromptVersionSummary(BaseModel):
    """Lightweight item for list endpoints."""

    specialization: str
    version: str
    is_active: bool
    author: str
    updated_by: str
    created_at: datetime
    updated_at: datetime
