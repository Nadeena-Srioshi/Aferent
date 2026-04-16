"""
app/models/enums.py
───────────────────
Shared enums for symptom triage and prompt management.
"""

from __future__ import annotations

from enum import Enum


class MedicalCategory(str, Enum):
    CARDIOLOGY = "cardiology"
    NEUROLOGY = "neurology"
    DERMATOLOGY = "dermatology"
    GASTROENTEROLOGY = "gastroenterology"
    ORTHOPEDICS = "orthopedics"
    GENERAL_MEDICINE = "general_medicine"


class ConfidenceTier(str, Enum):
    HIGH = "high"
    MEDIUM = "medium"
    LOW = "low"
