"""
app/models/triage_analytics.py
──────────────────────────────
MongoDB document model for specialization prompt analytics.
"""

from __future__ import annotations

from datetime import datetime

from pydantic import BaseModel, Field


class TriageAnalyticsDocument(BaseModel):
    specialization: str
    version: str
    total_hits: int = 0
    avg_confidence_score: float = 0.0
    high_count: int = 0
    medium_count: int = 0
    low_count: int = 0
    created_at: datetime = Field(default_factory=datetime.utcnow)
    updated_at: datetime = Field(default_factory=datetime.utcnow)

    model_config = {"populate_by_name": True}
