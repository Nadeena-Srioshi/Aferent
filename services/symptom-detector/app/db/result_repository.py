"""
app/db/result_repository.py
────────────────────────────
Data-access object for the `symptom_results` collection.

Responsibilities:
  - Persist a SymptomResponse after each analysis.
  - Query results by patient_id for audit / history.
  - All Motor interaction is isolated here.
"""

from __future__ import annotations

from datetime import datetime

from motor.motor_asyncio import AsyncIOMotorDatabase

from app.core.exceptions import DatabaseError
from app.core.logging import get_logger
from app.db.indexes import RESULTS_COLLECTION
from app.models.result import ResultDocument
from app.schemas.symptom import SymptomResponse

logger = get_logger(__name__)


class ResultRepository:
    def __init__(self, db: AsyncIOMotorDatabase) -> None:
        self._collection = db[RESULTS_COLLECTION]

    async def save(
        self,
        response: SymptomResponse,
        prompt_version: str = "1.0.0",
    ) -> ResultDocument:
        """
        Persist a completed SymptomResponse to Atlas.

        Args:
            response:       The final tiered response to store.
            prompt_version: Version of the PromptDocument used (for audit).

        Returns:
            The saved ResultDocument.

        Raises:
            DatabaseError: On Atlas write failure.
        """
        doc = ResultDocument(
            request_id=response.request_id,
            patient_id=response.patient_id,
            category=response.category,
            confidence_score=response.confidence_score,
            confidence_tier=response.confidence_tier,
            suggestions=response.suggestions,
            reasoning=response.reasoning,
            verification_required=response.verification_required,
            escalate_to_human=response.escalate_to_human,
            prompt_version=prompt_version,
            workflow_metadata=response.workflow_metadata,
        )

        try:
            await self._collection.insert_one(doc.model_dump())
        except Exception as exc:
            raise DatabaseError(f"Failed to save result: {exc}") from exc

        logger.info(
            "result_repository.saved request_id=%s tier=%s",
            doc.request_id,
            doc.confidence_tier.value,
        )
        return doc

    async def get_by_request_id(self, request_id: str) -> ResultDocument | None:
        """Fetch a single result by its UUID."""
        try:
            raw = await self._collection.find_one({"request_id": request_id})
        except Exception as exc:
            raise DatabaseError(f"Failed to fetch result: {exc}") from exc

        if raw is None:
            return None
        raw.pop("_id", None)
        return ResultDocument.model_validate(raw)

    async def list_by_patient(
        self,
        patient_id: str,
        *,
        page: int = 1,
        page_size: int = 20,
    ) -> tuple[list[ResultDocument], int]:
        """
        Return paginated results for a patient, newest first.

        Returns:
            (items, total_count)
        """
        skip = (page - 1) * page_size
        query = {"patient_id": patient_id}

        try:
            total = await self._collection.count_documents(query)
            cursor = (
                self._collection.find(query, {"_id": 0})
                .sort("created_at", -1)
                .skip(skip)
                .limit(page_size)
            )
            raw_docs = await cursor.to_list(length=page_size)
        except Exception as exc:
            raise DatabaseError(f"Failed to list results for patient: {exc}") from exc

        items = [ResultDocument.model_validate(d) for d in raw_docs]
        return items, total
