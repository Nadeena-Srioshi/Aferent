"""
app/db/prompt_repository.py
────────────────────────────
Data-access object for the `prompts` collection.

Public surface:
  Read  — get_active_prompt(category)         used by the analysis pipeline
  Admin — create / update / activate / delete  used by the admin API

Versioning rules (enforced here, not in the service layer):
  * Each (category, version) pair is unique (enforced by DB index).
  * Only ONE document per category may have active=True at a time.
  * `create` auto-increments the version for the category.
  * `activate` deactivates all other versions for the same category atomically.
"""

from __future__ import annotations

from datetime import datetime

from motor.motor_asyncio import AsyncIOMotorDatabase
from pymongo import ReturnDocument

from app.core.exceptions import DatabaseError, PromptNotFoundError
from app.core.logging import get_logger
from app.db.indexes import PROMPTS_COLLECTION
from app.models.enums import MedicalCategory
from app.models.prompt import PromptDocument
from app.schemas.prompt import PromptCreateRequest, PromptUpdateRequest

logger = get_logger(__name__)


class PromptRepository:
    def __init__(self, db: AsyncIOMotorDatabase) -> None:
        self._col = db[PROMPTS_COLLECTION]

    # ── Pipeline read ──────────────────────────────────────────────────────────

    async def get_active_prompt(self, category: MedicalCategory) -> PromptDocument:
        """
        Return the active prompt with the highest version for *category*.

        Raises:
            PromptNotFoundError: No active prompt exists.
            DatabaseError:       Unexpected Atlas failure.
        """
        logger.debug("prompt_repository.fetch_active", category=category.value)
        try:
            doc = await self._col.find_one(
                {"category": category.value, "active": True},
                sort=[("version", -1)],
            )
        except Exception as exc:
            raise DatabaseError(f"Failed to query prompts: {exc}") from exc

        if doc is None:
            raise PromptNotFoundError(
                f"No active prompt for category '{category.value}'"
            )
        doc.pop("_id", None)
        prompt = PromptDocument.model_validate(doc)
        logger.info(
            "prompt_repository.active_found",
            category=category.value,
            version=prompt.version,
        )
        return prompt

    # ── Admin reads ────────────────────────────────────────────────────────────

    async def list_all(
        self,
        category: MedicalCategory | None = None,
        *,
        page: int = 1,
        page_size: int = 20,
    ) -> tuple[list[PromptDocument], int]:
        """List all prompts, optionally filtered by category. Sorted newest version first."""
        query: dict = {}
        if category:
            query["category"] = category.value

        skip = (page - 1) * page_size
        try:
            total = await self._col.count_documents(query)
            cursor = (
                self._col.find(query, {"_id": 0})
                .sort([("category", 1), ("version", -1)])
                .skip(skip)
                .limit(page_size)
            )
            docs = await cursor.to_list(length=page_size)
        except Exception as exc:
            raise DatabaseError(f"Failed to list prompts: {exc}") from exc

        return [PromptDocument.model_validate(d) for d in docs], total

    async def get_by_version(
        self, category: MedicalCategory, version: int
    ) -> PromptDocument:
        """Fetch a specific version of a prompt."""
        try:
            doc = await self._col.find_one(
                {"category": category.value, "version": version},
                {"_id": 0},
            )
        except Exception as exc:
            raise DatabaseError(f"Failed to fetch prompt version: {exc}") from exc

        if doc is None:
            raise PromptNotFoundError(
                f"Prompt '{category.value}' version {version} not found."
            )
        return PromptDocument.model_validate(doc)

    async def list_versions(self, category: MedicalCategory) -> list[int]:
        """Return all version numbers for a category (ascending)."""
        try:
            cursor = self._col.find(
                {"category": category.value},
                {"version": 1, "_id": 0},
                sort=[("version", 1)],
            )
            docs = await cursor.to_list(length=200)
        except Exception as exc:
            raise DatabaseError(f"Failed to list versions: {exc}") from exc
        return [d["version"] for d in docs]

    # ── Admin writes ───────────────────────────────────────────────────────────

    async def create(self, req: PromptCreateRequest) -> PromptDocument:
        """
        Insert a new prompt version for *category*.
        Version is auto-incremented (max existing + 1).
        New prompts start INACTIVE — call set_active() to go live.
        """
        versions = await self.list_versions(req.category)
        next_version = (max(versions) + 1) if versions else 1

        now = datetime.utcnow()
        doc = PromptDocument(
            category=req.category,
            version=next_version,
            active=False,
            system_prompt=req.system_prompt,
            user_template=req.user_template,
            metadata=req.metadata,
            created_at=now,
            updated_at=now,
        )

        try:
            await self._col.insert_one(doc.model_dump())
        except Exception as exc:
            raise DatabaseError(f"Failed to create prompt: {exc}") from exc

        logger.info(
            "prompt_repository.created",
            category=req.category.value,
            version=next_version,
        )
        return doc

    async def update(
        self,
        category: MedicalCategory,
        version: int,
        req: PromptUpdateRequest,
    ) -> PromptDocument:
        """Patch mutable fields of an existing prompt version."""
        updates: dict = {"updated_at": datetime.utcnow()}
        if req.system_prompt is not None:
            updates["system_prompt"] = req.system_prompt
        if req.user_template is not None:
            updates["user_template"] = req.user_template
        if req.metadata is not None:
            updates["metadata"] = req.metadata

        try:
            updated = await self._col.find_one_and_update(
                {"category": category.value, "version": version},
                {"$set": updates},
                projection={"_id": 0},
                return_document=ReturnDocument.AFTER,
            )
        except Exception as exc:
            raise DatabaseError(f"Failed to update prompt: {exc}") from exc

        if updated is None:
            raise PromptNotFoundError(
                f"Prompt '{category.value}' version {version} not found."
            )
        logger.info("prompt_repository.updated", category=category.value, version=version)
        return PromptDocument.model_validate(updated)

    async def set_active(
        self, category: MedicalCategory, version: int, *, active: bool
    ) -> PromptDocument:
        """
        Activate or deactivate a specific version.
        Activation atomically deactivates all other versions for the same category.
        """
        await self.get_by_version(category, version)   # existence check

        now = datetime.utcnow()
        try:
            if active:
                # Deactivate all, then activate the target — atomic per-category
                await self._col.update_many(
                    {"category": category.value},
                    {"$set": {"active": False, "updated_at": now}},
                )
            updated = await self._col.find_one_and_update(
                {"category": category.value, "version": version},
                {"$set": {"active": active, "updated_at": now}},
                projection={"_id": 0},
                return_document=ReturnDocument.AFTER,
            )
        except Exception as exc:
            raise DatabaseError(f"Failed to set active state: {exc}") from exc

        logger.info(
            "prompt_repository.activation_changed",
            category=category.value,
            version=version,
            active=active,
        )
        return PromptDocument.model_validate(updated)

    async def delete(self, category: MedicalCategory, version: int) -> None:
        """
        Hard-delete a prompt version.
        Raises DatabaseError if attempting to delete the currently active version.
        """
        doc = await self.get_by_version(category, version)

        if doc.active:
            raise DatabaseError(
                f"Cannot delete the active prompt for '{category.value}'. "
                "Activate a different version first."
            )

        try:
            result = await self._col.delete_one(
                {"category": category.value, "version": version}
            )
        except Exception as exc:
            raise DatabaseError(f"Failed to delete prompt: {exc}") from exc

        if result.deleted_count == 0:
            raise PromptNotFoundError(
                f"Prompt '{category.value}' version {version} not found."
            )
        logger.info("prompt_repository.deleted", category=category.value, version=version)
