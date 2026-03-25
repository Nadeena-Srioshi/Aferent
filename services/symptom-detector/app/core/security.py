"""
app/core/security.py
─────────────────────
HTTP security dependencies for the admin API.

Phase 1: static API key via X-Admin-API-Key header.
Phase 2: swap `verify_admin_key` for JWT / OAuth2 without changing any route.

Usage in a route:
    @router.post("/prompts", dependencies=[Depends(verify_admin_key)])
    async def create_prompt(...): ...
"""

from __future__ import annotations

from fastapi import Depends, HTTPException, Security, status
from fastapi.security import APIKeyHeader

from app.core.config import get_settings

_API_KEY_HEADER = APIKeyHeader(
    name="X-Admin-API-Key",
    description="Static admin API key. Set ADMIN_API_KEY in your .env file.",
    auto_error=True,          # raises 403 automatically if header is absent
)


async def verify_admin_key(
    api_key: str = Security(_API_KEY_HEADER),
) -> None:
    """
    FastAPI dependency that validates the X-Admin-API-Key header.
    Raises HTTP 401 on mismatch so clients know they're unauthorised,
    not merely forbidden from a resource they know exists.
    """
    settings = get_settings()
    if api_key != settings.admin_api_key:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid admin API key.",
        )
