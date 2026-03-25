"""
app/api/v1/endpoints/admin.py
──────────────────────────────
Admin API for managing versioned prompts stored in MongoDB Atlas.

All routes require the X-Admin-API-Key header.

Endpoints:
  GET    /admin/prompts                               list all prompts (paginated)
  POST   /admin/prompts                               create a new prompt version
  GET    /admin/prompts/{category}/active             get the active version for a category
  GET    /admin/prompts/{category}/versions           list all versions for a category
  GET    /admin/prompts/{category}/versions/{version} get a specific version
  PATCH  /admin/prompts/{category}/versions/{version} update a specific version
  POST   /admin/prompts/{category}/versions/{version}/activate  activate/deactivate
  DELETE /admin/prompts/{category}/versions/{version} hard-delete a version
  POST   /admin/prompts/seed                          seed default prompts for all categories
"""

from __future__ import annotations

from fastapi import APIRouter, Depends, Query, status
from motor.motor_asyncio import AsyncIOMotorDatabase

from app.core.security import verify_admin_key
from app.db.mongo import get_database
from app.models.enums import MedicalCategory
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
    prefix="/admin/prompts",
    tags=["Admin — Prompts"],
    dependencies=[Depends(verify_admin_key)],   # every route in this router requires auth
)


def _get_service(db: AsyncIOMotorDatabase = Depends(get_database)) -> PromptService:
    return PromptService(db)


# ── List all prompts ───────────────────────────────────────────────────────────

@router.get(
    "",
    response_model=PaginatedResponse[PromptVersionSummary],
    summary="List all prompt versions",
)
async def list_prompts(
    category: MedicalCategory | None = Query(
        default=None, description="Filter by medical category."
    ),
    page: int = Query(default=1, ge=1),
    page_size: int = Query(default=20, ge=1, le=100),
    service: PromptService = Depends(_get_service),
) -> PaginatedResponse[PromptVersionSummary]:
    """
    Returns all prompt versions, optionally filtered by category.
    Results are sorted by category (asc) then version (desc).
    """
    return await service.list_prompts(category, page=page, page_size=page_size)


# ── Create new prompt version ──────────────────────────────────────────────────

@router.post(
    "",
    response_model=PromptResponse,
    status_code=status.HTTP_201_CREATED,
    summary="Create a new prompt version",
)
async def create_prompt(
    body: PromptCreateRequest,
    service: PromptService = Depends(_get_service),
) -> PromptResponse:
    """
    Creates a new prompt version for the given category.
    The version number is auto-incremented.
    The new prompt starts **inactive** — call the activate endpoint to go live.
    """
    return await service.create_prompt(body)


# ── Seed defaults ──────────────────────────────────────────────────────────────

@router.post(
    "/seed",
    response_model=list[PromptResponse],
    status_code=status.HTTP_201_CREATED,
    summary="Seed default prompts for all categories",
)
async def seed_prompts(
    service: PromptService = Depends(_get_service),
) -> list[PromptResponse]:
    """
    Inserts and activates a v1 prompt for every MedicalCategory that has no prompts yet.
    Idempotent — skips categories that already have at least one version.
    Useful for initial Atlas setup.
    """
    return await service.seed_defaults()


# ── Get active prompt for category ────────────────────────────────────────────

@router.get(
    "/{category}/active",
    response_model=PromptResponse,
    summary="Get the currently active prompt for a category",
)
async def get_active_prompt(
    category: MedicalCategory,
    service: PromptService = Depends(_get_service),
) -> PromptResponse:
    return await service.get_active_prompt(category)


# ── List versions for category ─────────────────────────────────────────────────

@router.get(
    "/{category}/versions",
    response_model=PaginatedResponse[PromptVersionSummary],
    summary="List all versions for a specific category",
)
async def list_versions(
    category: MedicalCategory,
    page: int = Query(default=1, ge=1),
    page_size: int = Query(default=20, ge=1, le=100),
    service: PromptService = Depends(_get_service),
) -> PaginatedResponse[PromptVersionSummary]:
    return await service.list_prompts(category, page=page, page_size=page_size)


# ── Get specific version ───────────────────────────────────────────────────────

@router.get(
    "/{category}/versions/{version}",
    response_model=PromptResponse,
    summary="Get a specific prompt version",
)
async def get_prompt_version(
    category: MedicalCategory,
    version: int,
    service: PromptService = Depends(_get_service),
) -> PromptResponse:
    return await service.get_prompt(category, version)


# ── Update specific version ────────────────────────────────────────────────────

@router.patch(
    "/{category}/versions/{version}",
    response_model=PromptResponse,
    summary="Update a prompt version (partial update)",
)
async def update_prompt(
    category: MedicalCategory,
    version: int,
    body: PromptUpdateRequest,
    service: PromptService = Depends(_get_service),
) -> PromptResponse:
    """
    Partially update a prompt version.
    Only fields included in the request body are changed.
    `updated_at` is always refreshed.
    """
    return await service.update_prompt(category, version, body)


# ── Activate / deactivate ──────────────────────────────────────────────────────

@router.post(
    "/{category}/versions/{version}/activate",
    response_model=PromptResponse,
    summary="Activate or deactivate a prompt version",
)
async def activate_prompt(
    category: MedicalCategory,
    version: int,
    body: PromptActivateRequest,
    service: PromptService = Depends(_get_service),
) -> PromptResponse:
    """
    Set `active: true` to make this the live prompt for the category.
    This atomically deactivates all other versions for the same category.

    Set `active: false` to deactivate without promoting another version.
    **Warning:** deactivating without promoting will cause analysis requests
    for this category to return 404 until another version is activated.
    """
    return await service.activate_prompt(category, version, active=body.active)


# ── Delete ─────────────────────────────────────────────────────────────────────

@router.delete(
    "/{category}/versions/{version}",
    status_code=status.HTTP_204_NO_CONTENT,
    summary="Hard-delete a prompt version",
)
async def delete_prompt(
    category: MedicalCategory,
    version: int,
    service: PromptService = Depends(_get_service),
) -> None:
    """
    Permanently deletes a prompt version.
    **The active version cannot be deleted** — activate a different version first.
    """
    await service.delete_prompt(category, version)
