"""
app/api/v1/endpoints/health.py
────────────────────────────────
GET /ai/api/v1/health - liveness + readiness probe.
Used by Docker healthcheck and load balancers.
"""

from __future__ import annotations

from fastapi import APIRouter, Depends, status
from motor.motor_asyncio import AsyncIOMotorDatabase

from app.core.config import get_settings
from app.db.mongo import get_database
from app.schemas.symptom import HealthResponse

router = APIRouter(tags=["Health"])


@router.get(
    "/health",
    response_model=HealthResponse,
    status_code=status.HTTP_200_OK,
    summary="Service liveness & readiness probe",
)
async def health_check(
    db: AsyncIOMotorDatabase = Depends(get_database),
) -> HealthResponse:
    """
    Verifies the service is alive and MongoDB is reachable.
    Returns 503 automatically if the DB dependency raises.
    """
    settings = get_settings()
    # Ping MongoDB to confirm readiness
    await db.command("ping")

    return HealthResponse(
        status="ok",
        version=settings.app_version,
        environment=settings.app_env,
    )
