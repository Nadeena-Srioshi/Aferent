"""
app/services/symptom/orchestrator.py
──────────────────────────────────────
Top-level use-case orchestrator for Phase 1.

Pipeline:
    SymptomRequest
        │
        ▼
    CategoryRouter.classify()          →  ClassificationResult
        │
        ▼
    PromptRepository.get_active_prompt()  →  PromptDocument
        │
        ▼
    LLMExecutor.execute()              →  LLMAnalysisResult
        │
        ▼
    handle_response()                  →  SymptomResponse

Phase 2 integration points (marked with NOTE comments):
    - Kafka: publish SymptomRequest after classify; consume result async.
    - LangGraph: replace LLMExecutor.execute() with a graph.invoke() call.
    - Result persistence: add SymptomResultRepository.save() after handle_response.
"""

from __future__ import annotations

from datetime import datetime
import uuid

from motor.motor_asyncio import AsyncIOMotorDatabase

from app.core.exceptions import PromptNotFoundError
from app.core.logging import get_logger
from app.db.prompt_repository import PromptRepository
from app.db.result_repository import ResultRepository
from app.db.triage_analytics_repository import TriageAnalyticsRepository
from app.models.enums import MedicalCategory
from app.models.prompt import PromptDocument
from app.schemas.symptom import SymptomRequest, SymptomResponse
from app.services.llm.executor import LLMExecutor
from app.services.symptom.category_router import CategoryRouter
from app.services.symptom.response_handler import handle_response

logger = get_logger(__name__)


class SymptomOrchestrator:
    """
    Wires together all Phase 1 components.
    Injected with a database handle per-request (from FastAPI Depends).
    """

    def __init__(self, db: AsyncIOMotorDatabase) -> None:
        self._prompt_repo = PromptRepository(db)
        self._result_repo = ResultRepository(db)
        self._analytics_repo = TriageAnalyticsRepository(db)
        self._router = CategoryRouter()
        self._executor = LLMExecutor()

    async def process(self, request: SymptomRequest, *, patient_id: str | None = None) -> SymptomResponse:
        request_id = str(uuid.uuid4())

        logger.info(
            "orchestrator.start request_id=%s patient_id=%s symptoms_length=%s",
            request_id,
            patient_id,
            len(request.symptoms),
        )

        # ── Step 1: Classify ──────────────────────────────────────────────────
        classification = await self._router.classify(request.symptoms)

        # NOTE (Phase 2 – Kafka):
        # await kafka_producer.send("symptom-events", {
        #     "request_id": request_id,
        #     "category": classification.category.value,
        #     "symptoms": request.symptoms,
        # })

        # ── Step 2: Fetch versioned prompt ────────────────────────────────────
        specialization = " ".join(classification.category.value.replace("_", " ").split()).title()
        fallback_specialization = " ".join(
            MedicalCategory.GENERAL_MEDICINE.value.replace("_", " ").split()
        ).title()

        prompt_fallback_used = False
        try:
            prompt = await self._prompt_repo.get_active_prompt(specialization)
        except PromptNotFoundError as exc:
            logger.warning(
                "orchestrator.prompt_not_found_fallback request_id=%s code=PROMPT_NOT_FOUND detail=%s lookup_specialization=%s fallback_specialization=%s",
                request_id,
                str(exc),
                specialization,
                fallback_specialization,
            )
            try:
                prompt = await self._prompt_repo.get_active_prompt(fallback_specialization)
                prompt_fallback_used = True
                logger.info(
                    "orchestrator.prompt_fallback_applied request_id=%s requested_specialization=%s using_specialization=%s version=%s",
                    request_id,
                    specialization,
                    prompt.specialization,
                    prompt.version,
                )
            except PromptNotFoundError as fallback_exc:
                logger.warning(
                    "orchestrator.prompt_fallback_active_missing request_id=%s original_detail=%s fallback_detail=%s",
                    request_id,
                    str(exc),
                    str(fallback_exc),
                )
                fallback_docs, _ = await self._prompt_repo.list_all(
                    fallback_specialization, page=1, page_size=1
                )
                if fallback_docs:
                    prompt = fallback_docs[0]
                    prompt_fallback_used = True
                    logger.info(
                        "orchestrator.prompt_fallback_latest_applied request_id=%s requested_specialization=%s using_specialization=%s version=%s",
                        request_id,
                        specialization,
                        prompt.specialization,
                        prompt.version,
                    )
                else:
                    logger.error(
                        "orchestrator.prompt_fallback_default_applied request_id=%s requested_specialization=%s reason=no_general_prompt_available",
                        request_id,
                        specialization,
                    )
                    now = datetime.utcnow()
                    prompt = PromptDocument(
                        specialization=fallback_specialization,
                        version="v0.0.0",
                        is_active=False,
                        system_instruction=(
                            "You are a General Medicine clinical triage assistant. "
                            "Analyze symptoms conservatively, provide safe actionable guidance, "
                            "highlight red flags that need urgent care, and return strict JSON only."
                        ),
                        author="system_fallback",
                        updated_by="system_fallback",
                        created_at=now,
                        updated_at=now,
                    )
                    prompt_fallback_used = True

        # ── Step 3: Execute LLM ───────────────────────────────────────────────
        # NOTE (Phase 2 – LangGraph):
        # analysis = await langgraph_runner.invoke(prompt, request.symptoms)
        analysis = await self._executor.execute(
            prompt=prompt,
            symptoms=request.symptoms,
        )

        # ── Step 4: Build tiered response ─────────────────────────────────────
        response = handle_response(
            analysis=analysis,
            category=classification.category,
            patient_id=patient_id,
            request_id=request_id,
        )
        response.workflow_metadata.update(
            {
                "prompt_lookup_specialization": specialization,
                "prompt_specialization": prompt.specialization,
                "prompt_version": prompt.version,
                "prompt_fallback_used": prompt_fallback_used,
            }
        )

        # ── Step 5: Persist response + analytics ────────────────────────────
        await self._result_repo.save(response, prompt_version=prompt.version)
        await self._analytics_repo.record_hit(
            specialization=specialization,
            version=prompt.version,
            confidence_score=response.confidence_score,
            confidence_tier=response.confidence_tier,
        )

        logger.info(
            "orchestrator.complete request_id=%s tier=%s",
            request_id,
            response.confidence_tier.value,
        )
        return response
