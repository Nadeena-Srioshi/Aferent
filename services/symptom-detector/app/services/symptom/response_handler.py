"""
app/services/symptom/response_handler.py
─────────────────────────────────────────
3-tier confidence routing logic.

Tier boundaries (configurable via constants):
    HIGH   score > 0.8  → automated suggestions only
    MEDIUM 0.5-0.8      → suggestions + verification_required flag
    LOW    < 0.5        → escalate_to_human flag (suggestions still included)

Design contract:
    - Pure function: no I/O, no side-effects.
    - All flag logic is centralised here; nothing else sets these flags.
    - Phase 2: LangGraph can call `handle_response` as a node function.
"""

from __future__ import annotations

import uuid

from app.core.logging import get_logger
from app.models.enums import ConfidenceTier, MedicalCategory
from app.schemas.symptom import LLMAnalysisResult, SymptomResponse

logger = get_logger(__name__)

# ── Tier thresholds ────────────────────────────────────────────────────────────
HIGH_THRESHOLD: float = 0.8
MEDIUM_THRESHOLD: float = 0.5


def _determine_tier(score: float) -> ConfidenceTier:
    if score > HIGH_THRESHOLD:
        return ConfidenceTier.HIGH
    if score >= MEDIUM_THRESHOLD:
        return ConfidenceTier.MEDIUM
    return ConfidenceTier.LOW


def handle_response(
    *,
    analysis: LLMAnalysisResult,
    category: MedicalCategory,
    patient_id: str | None = None,
    request_id: str | None = None,
) -> SymptomResponse:
    """
    Map an LLMAnalysisResult to a final SymptomResponse with tier flags.

    Args:
        analysis:   Parsed LLM output containing confidence_score + suggestions.
        category:   Classified MedicalCategory for this request.
        patient_id: Optional opaque patient reference (passed through).
        request_id: Idempotency / trace ID; auto-generated if not provided.

    Returns:
        SymptomResponse with exactly the right flags set per tier.
    """
    rid = request_id or str(uuid.uuid4())
    tier = _determine_tier(analysis.confidence_score)

    # ── Tier flags ─────────────────────────────────────────────────────────────
    verification_required = False
    escalate_to_human = False

    if tier == ConfidenceTier.HIGH:
        # Fully automated – no extra flags
        pass
    elif tier == ConfidenceTier.MEDIUM:
        verification_required = True
    else:  # LOW
        escalate_to_human = True

    logger.info(
        "response_handler.tier_assigned",
        request_id=rid,
        tier=tier.value,
        confidence_score=analysis.confidence_score,
        verification_required=verification_required,
        escalate_to_human=escalate_to_human,
    )

    return SymptomResponse(
        request_id=rid,
        patient_id=patient_id,
        category=category,
        confidence_score=analysis.confidence_score,
        confidence_tier=tier,
        suggestions=analysis.suggestions,
        reasoning=analysis.reasoning,
        verification_required=verification_required,
        escalate_to_human=escalate_to_human,
    )
