"""
app/models/prompt.py
────────────────────
MongoDB document model for specialization prompt storage.
"""

from __future__ import annotations

from datetime import datetime
import re

from pydantic import BaseModel, Field, field_validator


_SEMVER_PATTERN = re.compile(r"^v?(\d+)\.(\d+)\.(\d+)(?:[-+][0-9A-Za-z.-]+)?$")


class PromptDocument(BaseModel):
    specialization: str
    version: str
    is_active: bool = False
    system_instruction: str
    author: str
    updated_by: str
    created_at: datetime = Field(default_factory=datetime.utcnow)
    updated_at: datetime = Field(default_factory=datetime.utcnow)

    model_config = {"populate_by_name": True, "extra": "ignore"}

    @field_validator("specialization")
    @classmethod
    def _normalize_specialization(cls, value: str) -> str:
        normalized = " ".join(value.strip().replace("_", " ").split())
        if not normalized:
            raise ValueError("specialization cannot be empty.")
        return normalized.title()

    @field_validator("version")
    @classmethod
    def _validate_version(cls, value: str) -> str:
        version = value.strip()
        match = _SEMVER_PATTERN.fullmatch(version)
        if not match:
            raise ValueError("version must be a semantic version string like 'v1.0.4'.")

        if version.lower().startswith("v"):
            return version

        return f"v{match.group(1)}.{match.group(2)}.{match.group(3)}"

    @field_validator("system_instruction")
    @classmethod
    def _validate_system_instruction(cls, value: str) -> str:
        instruction = value.strip()
        if len(instruction) < 20:
            raise ValueError("system_instruction must be at least 20 characters long.")
        return instruction
