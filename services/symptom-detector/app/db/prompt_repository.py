"""
app/db/prompt_repository.py
────────────────────────────
Data-access object for the `specialization_prompts` collection.

Public surface:
  Read  — get_active_prompt(specialization) used by the analysis pipeline
  Admin — create / update / activate / delete used by the prompt API

Versioning rules:
  * Each (specialization, version) pair is unique (enforced by DB index).
  * Only ONE document per specialization may have is_active=True at a time.
    * Versions are semantic strings (e.g. v1.0.4).
  * `activate` deactivates all other versions for the same specialization atomically.
"""

from __future__ import annotations

from datetime import datetime
import re

from motor.motor_asyncio import AsyncIOMotorDatabase
from pymongo import ReturnDocument

from app.core.exceptions import DatabaseError, PromptNotFoundError
from app.core.logging import get_logger
from app.db.indexes import PROMPTS_COLLECTION
from app.models.prompt import PromptDocument
from app.schemas.prompt import PromptCreateRequest, PromptUpdateRequest

logger = get_logger(__name__)


class PromptRepository:
    def __init__(self, db: AsyncIOMotorDatabase) -> None:
        self._col = db[PROMPTS_COLLECTION]

    @staticmethod
    def _canonical_specialization(value: str) -> str:
        return " ".join(value.strip().replace("_", " ").split()).title()

    @staticmethod
    def _canonical_version(version: str) -> str:
        """Ensure version has v prefix for consistent DB queries."""
        version = version.strip()
        if not version.startswith("v"):
            return f"v{version}"
        return version

    @staticmethod
    def _semver_key(version: str) -> tuple[int, int, int, str]:
        match = re.fullmatch(r"v?(\d+)\.(\d+)\.(\d+)(?:[-+].*)?", version.strip())
        if match is None:
            return (0, 0, 0, version)
        return (int(match.group(1)), int(match.group(2)), int(match.group(3)), version)

    async def get_active_prompt(self, specialization: str) -> PromptDocument:
        """Return the active prompt with the highest semantic version."""
        specialization = self._canonical_specialization(specialization)
        logger.debug("prompt_repository.fetch_active specialization=%s", specialization)
        try:
            docs = await self._col.find(
                {"specialization": specialization, "is_active": True},
                {"_id": 0},
            ).to_list(length=50)
        except Exception as exc:
            raise DatabaseError(f"Failed to query prompts: {exc}") from exc

        if not docs:
            raise PromptNotFoundError(
                f"No active prompt for specialization '{specialization}'"
            )

        doc = max(docs, key=lambda item: self._semver_key(item["version"]))
        prompt = PromptDocument.model_validate(doc)
        logger.info(
            "prompt_repository.active_found specialization=%s version=%s",
            specialization,
            prompt.version,
        )
        return prompt

    async def list_all(
        self,
        specialization: str | None = None,
        *,
        page: int = 1,
        page_size: int = 20,
    ) -> tuple[list[PromptDocument], int]:
        """List all prompts, optionally filtered by specialization."""
        query: dict = {}
        if specialization:
            query["specialization"] = self._canonical_specialization(specialization)

        skip = (page - 1) * page_size
        try:
            total = await self._col.count_documents(query)
            docs = await self._col.find(query, {"_id": 0}).to_list(length=None)
        except Exception as exc:
            raise DatabaseError(f"Failed to list prompts: {exc}") from exc

        docs.sort(key=lambda item: self._semver_key(item["version"]), reverse=True)
        docs.sort(key=lambda item: item["specialization"])
        page_docs = docs[skip : skip + page_size]
        return [PromptDocument.model_validate(d) for d in page_docs], total

    async def get_by_version(self, specialization: str, version: str) -> PromptDocument:
        """Fetch a specific version of a prompt."""
        specialization = self._canonical_specialization(specialization)
        version = self._canonical_version(version)
        try:
            doc = await self._col.find_one(
                {"specialization": specialization, "version": version},
                {"_id": 0},
            )
        except Exception as exc:
            raise DatabaseError(f"Failed to fetch prompt version: {exc}") from exc

        if doc is None:
            raise PromptNotFoundError(
                f"Prompt '{specialization}' version {version} not found."
            )
        return PromptDocument.model_validate(doc)

    async def list_versions(self, specialization: str) -> list[str]:
        """Return all version strings for a specialization (semantic ascending)."""
        specialization = self._canonical_specialization(specialization)
        try:
            cursor = self._col.find(
                {"specialization": specialization},
                {"version": 1, "_id": 0},
            )
            docs = await cursor.to_list(length=200)
        except Exception as exc:
            raise DatabaseError(f"Failed to list versions: {exc}") from exc
        return [d["version"] for d in sorted(docs, key=lambda item: self._semver_key(item["version"]))]

    async def create(
        self,
        req: PromptCreateRequest,
        *,
        actor_id: str,
        version: str,
    ) -> PromptDocument:
        """Insert a new semantic version for a specialization."""
        specialization = self._canonical_specialization(req.specialization)
        actor_id = actor_id.strip()
        if not actor_id:
            raise DatabaseError("Caller identity is required to create prompt versions.")

        now = datetime.utcnow()
        doc = PromptDocument(
            specialization=specialization,
            version=version,
            is_active=False,
            system_instruction=req.system_instruction,
            author=actor_id,
            updated_by=actor_id,
            created_at=now,
            updated_at=now,
        )

        try:
            await self._col.insert_one(doc.model_dump())
        except Exception as exc:
            raise DatabaseError(f"Failed to create prompt: {exc}") from exc

        logger.info(
            "prompt_repository.created specialization=%s version=%s",
            specialization,
            version,
        )
        return doc

    async def update(
        self,
        specialization: str,
        version: str,
        *,
        updated_by: str,
        req: PromptUpdateRequest,
    ) -> PromptDocument:
        """Patch mutable fields of an existing prompt version."""
        specialization = self._canonical_specialization(specialization)
        version = self._canonical_version(version)
        updated_by = updated_by.strip()
        if not updated_by:
            raise DatabaseError("Caller identity is required to update prompt versions.")

        updates: dict = {"updated_at": datetime.utcnow(), "updated_by": updated_by}
        if req.system_instruction is not None:
            updates["system_instruction"] = req.system_instruction

        try:
            updated = await self._col.find_one_and_update(
                {"specialization": specialization, "version": version},
                {"$set": updates},
                projection={"_id": 0},
                return_document=ReturnDocument.AFTER,
            )
        except Exception as exc:
            raise DatabaseError(f"Failed to update prompt: {exc}") from exc

        if updated is None:
            raise PromptNotFoundError(
                f"Prompt '{specialization}' version {version} not found."
            )
        logger.info("prompt_repository.updated specialization=%s version=%s", specialization, version)
        return PromptDocument.model_validate(updated)

    async def set_active(
        self,
        specialization: str,
        version: str,
        *,
        active: bool,
        updated_by: str,
    ) -> PromptDocument:
        """Activate or deactivate a specific version."""
        specialization = self._canonical_specialization(specialization)
        version = self._canonical_version(version)
        await self.get_by_version(specialization, version)

        now = datetime.utcnow()
        try:
            if active:
                await self._col.update_many(
                    {"specialization": specialization},
                    {
                        "$set": {
                            "is_active": False,
                            "updated_at": now,
                            "updated_by": updated_by,
                        }
                    },
                )
            updated = await self._col.find_one_and_update(
                {"specialization": specialization, "version": version},
                {"$set": {"is_active": active, "updated_at": now, "updated_by": updated_by}},
                projection={"_id": 0},
                return_document=ReturnDocument.AFTER,
            )
        except Exception as exc:
            raise DatabaseError(f"Failed to set active state: {exc}") from exc

        logger.info(
            "prompt_repository.activation_changed specialization=%s version=%s is_active=%s",
            specialization,
            version,
            active,
        )
        return PromptDocument.model_validate(updated)

    async def delete(self, specialization: str, version: str) -> None:
        """Hard-delete a prompt version."""
        specialization = self._canonical_specialization(specialization)
        version = self._canonical_version(version)
        doc = await self.get_by_version(specialization, version)

        if doc.is_active:
            raise DatabaseError(
                f"Cannot delete the active prompt for '{specialization}'. Activate a different version first."
            )

        try:
            result = await self._col.delete_one(
                {"specialization": specialization, "version": version}
            )
        except Exception as exc:
            raise DatabaseError(f"Failed to delete prompt: {exc}") from exc

        if result.deleted_count == 0:
            raise PromptNotFoundError(
                f"Prompt '{specialization}' version {version} not found."
            )
        logger.info("prompt_repository.deleted specialization=%s version=%s", specialization, version)
