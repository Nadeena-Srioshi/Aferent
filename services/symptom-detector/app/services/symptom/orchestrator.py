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

import uuid

from motor.motor_asyncio import AsyncIOMotorDatabase

from app.core.logging import get_logger
from app.db.prompt_repository import PromptRepository
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
        self._router = CategoryRouter()
        self._executor = LLMExecutor()

    async def process(self, request: SymptomRequest) -> SymptomResponse:
        request_id = str(uuid.uuid4())

        logger.info(
            "orchestrator.start",
            request_id=request_id,
            patient_id=request.patient_id,
            symptoms_length=len(request.symptoms),
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
        prompt = await self._prompt_repo.get_active_prompt(classification.category)

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
            patient_id=request.patient_id,
            request_id=request_id,
        )

        # NOTE (Phase 2 – Persistence):
        # await result_repo.save(response)

        logger.info(
            "orchestrator.complete",
            request_id=request_id,
            tier=response.confidence_tier.value,
        )
        return response
