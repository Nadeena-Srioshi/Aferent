"""
app/core/security.py
─────────────────────
HTTP security dependencies for gateway-forwarded role checks.

All prompt management routes currently require `ADMIN`.
"""

from __future__ import annotations

from fastapi import Header, HTTPException, status

from app.core.config import get_settings

async def require_admin_role(
    x_user_role: str | None = Header(default=None, alias="X-User-Role"),
) -> None:
    """Allow only requests whose forwarded role is ADMIN."""
    settings = get_settings()
    if x_user_role is None:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Missing X-User-Role header.",
        )

    if x_user_role.strip().upper() != settings.admin_role:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Admin privileges are required.",
        )


async def verify_admin_key(*args, **kwargs) -> None:  # pragma: no cover - compatibility shim
    """Backward-compatible shim for older imports."""
    return await require_admin_role(*args, **kwargs)

