"""
app/core/config.py
──────────────────
Lightweight settings loader for the symptom-detector service.

This project snapshot does not ship with a full pydantic-settings
dependency tree, so configuration is loaded from environment variables
with sensible defaults and cached for the lifetime of the process.
"""

from __future__ import annotations

from dataclasses import dataclass
from functools import lru_cache
from os import getenv
from urllib.parse import urlparse


def _mongo_db_from_uri(mongo_uri: str) -> str:
    parsed = urlparse(mongo_uri)
    database = parsed.path.lstrip("/")
    if not database:
        raise ValueError("MONGO_URI must include a database name, e.g. mongodb://host/dbname")
    return database


@dataclass(frozen=True, slots=True)
class Settings:
    app_name: str = getenv("APP_NAME", "AI Symptom Service")
    app_version: str = getenv("APP_VERSION", "0.1.0")
    app_env: str = getenv("APP_ENV", "development")
    mongo_uri: str = getenv("MONGO_URI", "mongodb://localhost:27017")
    mongo_db_name: str = _mongo_db_from_uri(getenv("MONGO_URI", "mongodb://localhost:27017"))
    user_role_header: str = getenv("USER_ROLE_HEADER", "X-User-Role")
    user_id_header: str = getenv("USER_ID_HEADER", "X-User-ID")
    admin_role: str = getenv("ADMIN_ROLE", "ADMIN")
    gemini_api_key: str | None = getenv("GEMINI_API_KEY")
    gemini_model: str = getenv("GEMINI_MODEL", "gemini-1.5-flash")
    gemini_timeout_seconds: float = float(getenv("GEMINI_TIMEOUT_SECONDS", "30"))

    @property
    def is_production(self) -> bool:
        return self.app_env.strip().lower() == "production"

    @property
    def is_development(self) -> bool:
        return not self.is_production


@lru_cache(maxsize=1)
def get_settings() -> Settings:
    return Settings()
