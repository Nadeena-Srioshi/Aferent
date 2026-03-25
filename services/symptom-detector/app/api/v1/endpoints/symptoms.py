"""
app/api/v1/endpoints/symptoms.py
──────────────────────────────────
POST /ai/api/v1/symptoms - analyse a symptom description.

Route files are intentionally thin:
  - validate input  (Pydantic, automatic)
  - call orchestrator
  - return response
No business logic lives here.
"""

from __future__ import annotations

from fastapi import APIRouter, Depends, status

from app.schemas.symptom import SymptomRequest, SymptomResponse
from app.services.symptom.orchestrator import SymptomOrchestrator
from app.utils.dependencies import get_orchestrator

router = APIRouter(prefix="/symptoms", tags=["Symptoms"])


@router.post(
    "/analyse",
    response_model=SymptomResponse,
    status_code=status.HTTP_200_OK,
    summary="Analyse symptoms and return tiered suggestions",
    response_description="Structured analysis with confidence tier and flags",
)
async def analyse_symptoms(
    request: SymptomRequest,
    orchestrator: SymptomOrchestrator = Depends(get_orchestrator),
) -> SymptomResponse:
    """
    Submit a symptom description for AI-powered triage.

    **Confidence tiers:**
    - `> 0.8` → automated suggestions returned directly
    - `0.5 - 0.8` → suggestions + `verification_required: true`
    - `< 0.5` → `escalate_to_human: true`
    """
    return await orchestrator.process(request)
