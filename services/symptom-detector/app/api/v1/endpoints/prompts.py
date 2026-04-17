"""
app/api/v1/endpoints/prompts.py
───────────────────────────────
Prompt management API for specialization prompts.

All routes are ADMIN-only and expect the gateway to forward:
  - X-User-Role: ADMIN
"""

from __future__ import annotations

from fastapi import APIRouter, Depends, Header, Query, status
from motor.motor_asyncio import AsyncIOMotorDatabase

from app.core.security import require_admin_role
from app.db.mongo import get_database
from app.schemas.common import PaginatedResponse
from app.schemas.prompt import (
    PromptActivateRequest,
    PromptCreateRequest,
    PromptResponse,
    PromptUpdateRequest,
    PromptVersionSummary,
)
from app.services.prompt.prompt_service import PromptService

router = APIRouter(
    prefix="/prompts",
    tags=["Prompts"],
    dependencies=[Depends(require_admin_role)],
)


def _get_service(db: AsyncIOMotorDatabase = Depends(get_database)) -> PromptService:
    return PromptService(db)


@router.get("", response_model=PaginatedResponse[PromptVersionSummary], summary="List specialization prompts")
async def list_prompts(
    specialization: str | None = Query(default=None, description="Filter by specialization name."),
    page: int = Query(default=1, ge=1),
    page_size: int = Query(default=20, ge=1, le=100),
    service: PromptService = Depends(_get_service),
) -> PaginatedResponse[PromptVersionSummary]:
    return await service.list_prompts(specialization, page=page, page_size=page_size)


@router.post("", response_model=PromptResponse, status_code=status.HTTP_201_CREATED, summary="Create a specialization prompt version")
async def create_prompt(
    body: PromptCreateRequest,
    x_user_id: str = Header(..., alias="X-User-ID"),
    service: PromptService = Depends(_get_service),
) -> PromptResponse:
    return await service.create_prompt(body, actor_id=x_user_id)


@router.post("/seed", response_model=list[PromptResponse], status_code=status.HTTP_201_CREATED, summary="Seed default specialization prompts")
async def seed_prompts(
    x_user_id: str = Header(..., alias="X-User-ID"),
    service: PromptService = Depends(_get_service),
) -> list[PromptResponse]:
    return await service.seed_defaults(actor_id=x_user_id)


@router.get("/{specialization}/active", response_model=PromptResponse, summary="Get the active prompt for a specialization")
async def get_active_prompt(
    specialization: str,
    service: PromptService = Depends(_get_service),
) -> PromptResponse:
    return await service.get_active_prompt(specialization)


@router.get("/{specialization}/versions", response_model=PaginatedResponse[PromptVersionSummary], summary="List versions for a specialization")
async def list_versions(
    specialization: str,
    page: int = Query(default=1, ge=1),
    page_size: int = Query(default=20, ge=1, le=100),
    service: PromptService = Depends(_get_service),
) -> PaginatedResponse[PromptVersionSummary]:
    return await service.list_prompts(specialization, page=page, page_size=page_size)


@router.get("/{specialization}/versions/{version}", response_model=PromptResponse, summary="Get a specific specialization prompt version")
async def get_prompt_version(
    specialization: str,
    version: str,
    service: PromptService = Depends(_get_service),
) -> PromptResponse:
    return await service.get_prompt(specialization, version)


@router.patch("/{specialization}/versions/{version}", response_model=PromptResponse, summary="Update a specialization prompt version")
async def update_prompt(
    specialization: str,
    version: str,
    body: PromptUpdateRequest,
    x_user_id: str = Header(..., alias="X-User-ID"),
    service: PromptService = Depends(_get_service),
) -> PromptResponse:
    return await service.update_prompt(specialization, version, updated_by=x_user_id, req=body)


@router.post("/{specialization}/versions/{version}/activate", response_model=PromptResponse, summary="Activate or deactivate a specialization prompt version")
async def activate_prompt(
    specialization: str,
    version: str,
    body: PromptActivateRequest,
    x_user_id: str = Header(..., alias="X-User-ID"),
    service: PromptService = Depends(_get_service),
) -> PromptResponse:
    return await service.activate_prompt(
        specialization,
        version,
        active=body.is_active,
        updated_by=x_user_id,
    )


@router.delete("/{specialization}/versions/{version}", status_code=status.HTTP_204_NO_CONTENT, summary="Delete a specialization prompt version")
async def delete_prompt(
    specialization: str,
    version: str,
    service: PromptService = Depends(_get_service),
) -> None:
    await service.delete_prompt(specialization, version)
