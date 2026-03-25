"""
app/services/llm/executor.py
────────────────────────────
Small local stand-in for the LLM executor.
"""

from __future__ import annotations

from app.core.exceptions import LLMExecutionError
from app.models.prompt import PromptDocument
from app.schemas.symptom import LLMAnalysisResult


class LLMExecutor:
    async def execute(self, *, prompt: PromptDocument, symptoms: str) -> LLMAnalysisResult:
        try:
            text = symptoms.lower()
            score = 0.55
            suggestions = [f"Review the {prompt.specialization.lower()} symptoms with a clinician."]

            if any(token in text for token in ["chest pain", "left arm", "sweating", "heart"]):
                score = 0.92
                suggestions = [
                    "Call emergency services immediately if symptoms are severe or worsening.",
                    "Do not drive yourself to the hospital.",
                    "Rest and seek urgent clinical evaluation.",
                ]
            elif any(token in text for token in ["rash", "itch", "skin"]):
                score = 0.84
                suggestions = [
                    "Avoid irritants and monitor for spread.",
                    "Consider a same-day clinical review if the rash worsens.",
                ]
            elif len(text.split()) < 15:
                score = 0.35
                suggestions = ["Gather more detail about onset, location, and severity."]

            reasoning = (
                f"Heuristic analysis for {prompt.specialization} using prompt version {prompt.version}."
            )
            return LLMAnalysisResult(
                confidence_score=score,
                suggestions=suggestions,
                reasoning=reasoning,
            )
        except Exception as exc:  # pragma: no cover - safety net for unexpected failures
            raise LLMExecutionError(f"LLM execution failed: {exc}") from exc
