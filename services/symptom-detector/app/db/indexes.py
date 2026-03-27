"""
app/db/indexes.py
─────────────────
MongoDB collection names and index bootstrap.
"""

from __future__ import annotations

from motor.motor_asyncio import AsyncIOMotorDatabase

PROMPTS_COLLECTION = "specialization_prompts"
RESULTS_COLLECTION = "symptom_results"
TRIAGE_ANALYTICS_COLLECTION = "triage_analytics"


async def ensure_indexes(db: AsyncIOMotorDatabase) -> None:
    prompts = db[PROMPTS_COLLECTION]
    await prompts.create_index(
        [("specialization", 1), ("version", 1)],
        name="uniq_specialization_version",
        unique=True,
    )
    await prompts.create_index(
        [("specialization", 1), ("is_active", 1)],
        name="idx_specialization_active",
    )
    await prompts.create_index(
        [("specialization", 1), ("created_at", -1)],
        name="idx_specialization_created_at",
    )

    results = db[RESULTS_COLLECTION]
    await results.create_index("request_id", name="uniq_request_id", unique=True)
    await results.create_index(
        [("patient_id", 1), ("created_at", -1)],
        name="idx_patient_created_at",
    )
    await results.create_index(
        "created_at",
        name="ttl_results_created_at",
        expireAfterSeconds=60 * 60 * 24 * 90,
    )

    analytics = db[TRIAGE_ANALYTICS_COLLECTION]
    await analytics.create_index(
        [("specialization", 1), ("version", 1)],
        name="uniq_analytics_specialization_version",
        unique=True,
    )
    await analytics.create_index(
        [("specialization", 1), ("updated_at", -1)],
        name="idx_analytics_specialization_updated_at",
    )
