"""
app/api/v1/router.py
─────────────────────
Aggregates all v1 endpoint routers.
Adding a new endpoint == one import + one include_router call here.
"""

from fastapi import APIRouter

from app.api.v1.endpoints import admin, health, symptoms

api_router = APIRouter(prefix="/api/v1")

api_router.include_router(health.router)
api_router.include_router(symptoms.router)
api_router.include_router(admin.router)
