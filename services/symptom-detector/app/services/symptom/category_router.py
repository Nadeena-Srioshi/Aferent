"""
app/services/symptom/category_router.py
───────────────────────────────────────
Rule-based symptom categorisation used by the orchestrator.
"""

from __future__ import annotations

from app.core.exceptions import CategoryClassificationError
from app.models.enums import MedicalCategory
from app.schemas.symptom import ClassificationResult


_KEYWORDS: dict[MedicalCategory, tuple[str, ...]] = {
    MedicalCategory.CARDIOLOGY: ("chest pain", "heart", "palpitations", "left arm", "sweating"),
    MedicalCategory.NEUROLOGY: ("headache", "migraine", "dizziness", "stroke", "seizure", "weakness"),
    MedicalCategory.DERMATOLOGY: ("rash", "itch", "skin", "lesion", "blister"),
    MedicalCategory.GASTROENTEROLOGY: ("stomach", "abdominal", "nausea", "vomiting", "reflux"),
    MedicalCategory.ORTHOPEDICS: ("back pain", "joint", "fracture", "sprain", "bone"),
    MedicalCategory.GENERAL_MEDICINE: tuple(),
}


class CategoryRouter:
    async def classify(self, symptoms: str) -> ClassificationResult:
        text = symptoms.lower().strip()
        if not text:
            raise CategoryClassificationError("Symptoms cannot be empty.")

        for category, keywords in _KEYWORDS.items():
            if any(keyword in text for keyword in keywords):
                confidence = 0.88 if category != MedicalCategory.GENERAL_MEDICINE else 0.55
                return ClassificationResult(category=category, confidence=confidence)

        return ClassificationResult(category=MedicalCategory.GENERAL_MEDICINE, confidence=0.5)
