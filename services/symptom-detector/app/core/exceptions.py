"""
app/core/exceptions.py
──────────────────────
Domain exceptions shared by repositories, services, and HTTP handlers.
"""

from __future__ import annotations


class SymptomServiceError(Exception):
    """Base class for all domain-level failures in symptom-detector."""


class DatabaseError(SymptomServiceError):
    """Raised when MongoDB operations fail."""


class PromptNotFoundError(SymptomServiceError):
    """Raised when a prompt document cannot be found."""


class CategoryClassificationError(SymptomServiceError):
    """Raised when symptom classification fails."""


class LLMExecutionError(SymptomServiceError):
    """Raised when the LLM executor cannot produce a response."""


class LLMResponseParseError(SymptomServiceError):
    """Raised when LLM output cannot be parsed into the expected schema."""
