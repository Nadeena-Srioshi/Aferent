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
        specialization=doc.specialization,
        version=doc.version,
        is_active=doc.is_active,
        system_instruction=doc.system_instruction,
        author=doc.author,
        updated_by=doc.updated_by,
        created_at=doc.created_at,
        updated_at=doc.updated_at,
    )


def _to_summary(doc: PromptDocument) -> PromptVersionSummary:
    return PromptVersionSummary(
        specialization=doc.specialization,
        version=doc.version,
        is_active=doc.is_active,
        author=doc.author,
        updated_by=doc.updated_by,
        created_at=doc.created_at,
        updated_at=doc.updated_at,
    )


class PromptService:
    def __init__(self, db: AsyncIOMotorDatabase) -> None:
        self._repo = PromptRepository(db)

    async def list_prompts(
        self,
        specialization: str | None,
        page: int,
        page_size: int,
    ) -> PaginatedResponse[PromptVersionSummary]:
        docs, total = await self._repo.list_all(specialization, page=page, page_size=page_size)
        return PaginatedResponse(
            items=[_to_summary(d) for d in docs],
            total=total,
            page=page,
            page_size=page_size,
        )

    async def get_prompt(
        self, specialization: str, version: str
    ) -> PromptResponse:
        doc = await self._repo.get_by_version(specialization, version)
        return _to_response(doc)

    async def get_active_prompt(self, specialization: str) -> PromptResponse:
        doc = await self._repo.get_active_prompt(specialization)
        return _to_response(doc)

    async def create_prompt(self, req: PromptCreateRequest) -> PromptResponse:
        doc = await self._repo.create(req)
        return _to_response(doc)

    async def update_prompt(
        self,
        specialization: str,
        version: str,
        req: PromptUpdateRequest,
    ) -> PromptResponse:
        doc = await self._repo.update(specialization, version, req)
        return _to_response(doc)

    async def activate_prompt(
        self, specialization: str, version: str, *, active: bool, updated_by: str
    ) -> PromptResponse:
        doc = await self._repo.set_active(specialization, version, active=active, updated_by=updated_by)
        return _to_response(doc)

    async def delete_prompt(
        self, specialization: str, version: str
    ) -> None:
        await self._repo.delete(specialization, version)

    async def seed_defaults(self) -> list[PromptResponse]:
        """
        Insert v1 prompts for any category that has zero prompts.
        Idempotent — skips categories that already have at least one version.
        Useful for first-time Atlas setup or resetting a test database.
        """
        created: list[PromptResponse] = []

        from app.models.enums import MedicalCategory

        for category in MedicalCategory:
            specialization = " ".join(category.value.replace("_", " ").split()).title()
            versions = await self._repo.list_versions(specialization)
            if versions:
                logger.debug("prompt_service.seed_skip", specialization=specialization)
                continue

            req = PromptCreateRequest(
                specialization=specialization,
                version="1.0.0",
                system_instruction=(
                    f"You are a specialized {specialization} triage assistant.\n"
                    "Analyse the patient's symptoms and return a JSON object with these exact fields:\n"
                    "- confidence_score: float 0.0–1.0\n"
                    "- suggestions: array of 3–5 actionable clinical suggestions\n"
                    "- reasoning: brief explanation\n"
                    "Return ONLY valid JSON. No markdown, no preamble."
                ),
                author="system_seed",
                updated_by="system_seed",
            )
            doc = await self._repo.create(req)
            # Auto-activate the very first version for each category
            doc = await self._repo.set_active(specialization, doc.version, active=True, updated_by="system_seed")
            created.append(_to_response(doc))
            logger.info("prompt_service.seeded", specialization=specialization, version=doc.version)

        return created
