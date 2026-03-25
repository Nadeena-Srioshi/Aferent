"""
app/services/prompt/prompt_service.py
───────────────────────────────────────
Business logic for prompt administration.

Sits between the admin API endpoints and the PromptRepository.
Keeps orchestration (e.g. "seed all categories") out of both the
repository (DB concerns) and the routes (HTTP concerns).
"""

from __future__ import annotations

from motor.motor_asyncio import AsyncIOMotorDatabase

from app.core.logging import get_logger
from app.db.prompt_repository import PromptRepository
from app.models.enums import MedicalCategory
from app.models.prompt import PromptDocument
from app.schemas.common import PaginatedResponse
from app.schemas.prompt import (
    PromptCreateRequest,
    PromptResponse,
    PromptUpdateRequest,
    PromptVersionSummary,
)

logger = get_logger(__name__)


def _to_response(doc: PromptDocument) -> PromptResponse:
    return PromptResponse(
        category=doc.category,
        version=doc.version,
        active=doc.active,
        system_prompt=doc.system_prompt,
        user_template=doc.user_template,
        metadata=doc.metadata,
        created_at=doc.created_at,
        updated_at=doc.updated_at,
    )


def _to_summary(doc: PromptDocument) -> PromptVersionSummary:
    return PromptVersionSummary(
        category=doc.category,
        version=doc.version,
        active=doc.active,
        updated_at=doc.updated_at,
        metadata=doc.metadata,
    )


class PromptService:
    def __init__(self, db: AsyncIOMotorDatabase) -> None:
        self._repo = PromptRepository(db)

    async def list_prompts(
        self,
        category: MedicalCategory | None,
        page: int,
        page_size: int,
    ) -> PaginatedResponse[PromptVersionSummary]:
        docs, total = await self._repo.list_all(
            category, page=page, page_size=page_size
        )
        return PaginatedResponse(
            items=[_to_summary(d) for d in docs],
            total=total,
            page=page,
            page_size=page_size,
        )

    async def get_prompt(
        self, category: MedicalCategory, version: int
    ) -> PromptResponse:
        doc = await self._repo.get_by_version(category, version)
        return _to_response(doc)

    async def get_active_prompt(self, category: MedicalCategory) -> PromptResponse:
        doc = await self._repo.get_active_prompt(category)
        return _to_response(doc)

    async def create_prompt(self, req: PromptCreateRequest) -> PromptResponse:
        doc = await self._repo.create(req)
        return _to_response(doc)

    async def update_prompt(
        self,
        category: MedicalCategory,
        version: int,
        req: PromptUpdateRequest,
    ) -> PromptResponse:
        doc = await self._repo.update(category, version, req)
        return _to_response(doc)

    async def activate_prompt(
        self, category: MedicalCategory, version: int, *, active: bool
    ) -> PromptResponse:
        doc = await self._repo.set_active(category, version, active=active)
        return _to_response(doc)

    async def delete_prompt(
        self, category: MedicalCategory, version: int
    ) -> None:
        await self._repo.delete(category, version)

    async def seed_defaults(self) -> list[PromptResponse]:
        """
        Insert v1 prompts for any category that has zero prompts.
        Idempotent — skips categories that already have at least one version.
        Useful for first-time Atlas setup or resetting a test database.
        """
        created: list[PromptResponse] = []

        for category in MedicalCategory:
            versions = await self._repo.list_versions(category)
            if versions:
                logger.debug("prompt_service.seed_skip", category=category.value)
                continue

            req = PromptCreateRequest(
                category=category,
                system_prompt=(
                    f"You are an expert {category.value.replace('_', ' ')} triage assistant.\n"
                    "Analyse the patient's symptoms and return a JSON object with these exact fields:\n"
                    "- confidence_score: float 0.0–1.0\n"
                    "- suggestions: array of 3–5 actionable clinical suggestions\n"
                    "- reasoning: brief explanation\n"
                    "Return ONLY valid JSON. No markdown, no preamble."
                ),
                user_template=(
                    "Patient symptoms: {symptoms}\n\n"
                    "Respond with a JSON object: confidence_score, suggestions (array), reasoning."
                ),
                metadata={"author": "system_seed", "seeded": True},
            )
            doc = await self._repo.create(req)
            # Auto-activate the very first version for each category
            doc = await self._repo.set_active(category, doc.version, active=True)
            created.append(_to_response(doc))
            logger.info("prompt_service.seeded", category=category.value, version=doc.version)

        return created
