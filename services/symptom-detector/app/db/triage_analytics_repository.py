"""
app/db/triage_analytics_repository.py
────────────────────────────────────
Repository for maintaining per-specialization/per-version triage analytics.
"""

from __future__ import annotations

from datetime import datetime

from motor.motor_asyncio import AsyncIOMotorDatabase
from pymongo import ReturnDocument

from app.core.exceptions import DatabaseError
from app.core.logging import get_logger
from app.db.indexes import TRIAGE_ANALYTICS_COLLECTION
from app.models.enums import ConfidenceTier
from app.models.triage_analytics import TriageAnalyticsDocument

logger = get_logger(__name__)


class TriageAnalyticsRepository:
    def __init__(self, db: AsyncIOMotorDatabase) -> None:
        self._collection = db[TRIAGE_ANALYTICS_COLLECTION]

    async def record_hit(
        self,
        *,
        specialization: str,
        version: str,
        confidence_score: float,
        confidence_tier: ConfidenceTier,
    ) -> TriageAnalyticsDocument:
        """
        Upsert analytics for a specialization/version pair:
        - increment total hits
        - update running average confidence score
        - increment one tier bucket
        """
        now = datetime.utcnow()
        high_inc = 1 if confidence_tier == ConfidenceTier.HIGH else 0
        medium_inc = 1 if confidence_tier == ConfidenceTier.MEDIUM else 0
        low_inc = 1 if confidence_tier == ConfidenceTier.LOW else 0

        update_pipeline = [
            {
                "$set": {
                    "specialization": specialization,
                    "version": version,
                    "created_at": {"$ifNull": ["$created_at", now]},
                    "updated_at": now,
                    "total_hits": {"$add": [{"$ifNull": ["$total_hits", 0]}, 1]},
                    "high_count": {"$add": [{"$ifNull": ["$high_count", 0]}, high_inc]},
                    "medium_count": {"$add": [{"$ifNull": ["$medium_count", 0]}, medium_inc]},
                    "low_count": {"$add": [{"$ifNull": ["$low_count", 0]}, low_inc]},
                    "avg_confidence_score": {
                        "$let": {
                            "vars": {
                                "prev_hits": {"$ifNull": ["$total_hits", 0]},
                                "prev_avg": {"$ifNull": ["$avg_confidence_score", 0.0]},
                            },
                            "in": {
                                "$cond": [
                                    {"$eq": ["$$prev_hits", 0]},
                                    confidence_score,
                                    {
                                        "$divide": [
                                            {
                                                "$add": [
                                                    {"$multiply": ["$$prev_avg", "$$prev_hits"]},
                                                    confidence_score,
                                                ]
                                            },
                                            {"$add": ["$$prev_hits", 1]},
                                        ]
                                    },
                                ]
                            },
                        }
                    },
                }
            }
        ]

        try:
            updated = await self._collection.find_one_and_update(
                {"specialization": specialization, "version": version},
                update_pipeline,
                upsert=True,
                return_document=ReturnDocument.AFTER,
                projection={"_id": 0},
            )
        except Exception as exc:
            raise DatabaseError(f"Failed to update triage analytics: {exc}") from exc

        logger.info(
            "triage_analytics.recorded specialization=%s version=%s confidence_tier=%s confidence_score=%.4f",
            specialization,
            version,
            confidence_tier.value,
            confidence_score,
        )
        return TriageAnalyticsDocument.model_validate(updated)
