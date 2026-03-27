"""
app/services/llm/executor.py
────────────────────────────
Gemini-backed LLM executor with resilient fallback behavior.
"""

from __future__ import annotations

import json

import httpx

from app.core.config import get_settings
from app.core.exceptions import LLMExecutionError
from app.core.logging import get_logger
from app.models.prompt import PromptDocument
from app.schemas.symptom import LLMAnalysisResult

logger = get_logger(__name__)


class LLMExecutor:
    def __init__(self) -> None:
        self._settings = get_settings()

    async def execute(self, *, prompt: PromptDocument, symptoms: str) -> LLMAnalysisResult:
        """Execute model inference, falling back to heuristic logic on failures."""
        try:
            return await self._execute_gemini(prompt=prompt, symptoms=symptoms)
        except Exception as exc:
            logger.error(
                "llm_executor.gemini_failed_falling_back specialization=%s version=%s error=%s",
                prompt.specialization,
                prompt.version,
                str(exc),
            )
            return self._fallback_analysis(prompt=prompt, symptoms=symptoms)

    async def _execute_gemini(self, *, prompt: PromptDocument, symptoms: str) -> LLMAnalysisResult:
        api_key = (self._settings.gemini_api_key or "").strip()
        if not api_key:
            raise LLMExecutionError("GEMINI_API_KEY is not configured.")

        url = (
            f"https://generativelanguage.googleapis.com/v1beta/models/"
            f"{self._settings.gemini_model}:generateContent?key={api_key}"
        )

        request_payload = {
            "system_instruction": {
                "parts": [{"text": prompt.system_instruction}],
            },
            "contents": [
                {
                    "role": "user",
                    "parts": [
                        {
                            "text": (
                                "Analyze the following patient symptom description and respond with "
                                "strict JSON only.\n\n"
                                "Required JSON fields:\n"
                                "- confidence_score: float between 0.0 and 1.0\n"
                                "- suggestions: array of 3-5 actionable suggestions\n"
                                "- reasoning: brief string\n\n"
                                f"Patient symptoms:\n{symptoms}"
                            )
                        }
                    ],
                }
            ],
            "generationConfig": {
                "temperature": 0.2,
                "responseMimeType": "application/json",
            },
        }

        timeout = httpx.Timeout(self._settings.gemini_timeout_seconds)
        async with httpx.AsyncClient(timeout=timeout) as client:
            response = await client.post(url, json=request_payload)

        if response.status_code >= 400:
            raise LLMExecutionError(
                f"Gemini request failed with {response.status_code}: {response.text[:500]}"
            )

        payload = response.json()
        text = self._extract_response_text(payload)
        parsed = json.loads(text)

        confidence = float(parsed["confidence_score"])
        if confidence < 0.0 or confidence > 1.0:
            raise LLMExecutionError("LLM returned confidence_score outside [0.0, 1.0].")

        suggestions_raw = parsed["suggestions"]
        if not isinstance(suggestions_raw, list) or not suggestions_raw:
            raise LLMExecutionError("LLM returned invalid suggestions payload.")
        suggestions = [str(item).strip() for item in suggestions_raw if str(item).strip()]

        reasoning = str(parsed["reasoning"]).strip()
        if not reasoning:
            raise LLMExecutionError("LLM returned empty reasoning.")

        return LLMAnalysisResult(
            confidence_score=confidence,
            suggestions=suggestions,
            reasoning=reasoning,
        )

    @staticmethod
    def _extract_response_text(payload: dict) -> str:
        try:
            return payload["candidates"][0]["content"]["parts"][0]["text"]
        except Exception as exc:
            raise LLMExecutionError("Gemini response format was not as expected.") from exc

    @staticmethod
    def _fallback_analysis(*, prompt: PromptDocument, symptoms: str) -> LLMAnalysisResult:
        """Deterministic fallback if remote LLM call fails."""
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
