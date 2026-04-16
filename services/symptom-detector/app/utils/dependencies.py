"""
app/utils/dependencies.py
──────────────────────────
FastAPI dependency functions.
All Depends() targets live here to keep route files thin.
"""

from __future__ import annotations

from fastapi import Depends
from motor.motor_asyncio import AsyncIOMotorDatabase

from app.db.mongo import get_database
from app.services.symptom.orchestrator import SymptomOrchestrator


async def get_orchestrator(
    db: AsyncIOMotorDatabase = Depends(get_database),
) -> SymptomOrchestrator:
    """Provide a fresh SymptomOrchestrator scoped to the current request."""
    return SymptomOrchestrator(db=db)
