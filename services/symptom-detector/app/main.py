"""
app/main.py
────────────
FastAPI application factory.

Responsibilities:
  - Configure structured logging
  - Manage MongoDB connection lifecycle
  - Register all routers
  - Register all exception handlers
  - Expose /docs and /redoc (disabled in production)
"""

from __future__ import annotations

from contextlib import asynccontextmanager

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from app.api.v1.router import api_router
from app.core.config import get_settings
from app.core.logging import configure_logging, get_logger
from app.db.indexes import ensure_indexes
from app.db.mongo import connect, disconnect, get_client
from app.utils.exception_handlers import register_exception_handlers

configure_logging()
logger = get_logger(__name__)


@asynccontextmanager
async def lifespan(app: FastAPI):
    """Manage startup and graceful shutdown."""
    settings = get_settings()
    logger.info(
        "app.starting",
        name=settings.app_name,
        version=settings.app_version,
        env=settings.app_env,
    )

    # ── Startup ───────────────────────────────────────────────────────────────
    await connect()
    db = get_client()[settings.mongo_db_name]
    await ensure_indexes(db)

    # Phase 2: initialise Kafka producer here
    # Phase 2: initialise LangGraph runner here

    logger.info("app.ready")
    yield

    # ── Shutdown ──────────────────────────────────────────────────────────────
    await disconnect()
    logger.info("app.shutdown")


def create_app() -> FastAPI:
    settings = get_settings()

    app = FastAPI(
        title="AI Symptom Service",
        version=settings.app_version,
        description=(
            "Phase 1 – AI-powered symptom triage with confidence-tiered responses.\n\n"
            "Phase 2 will add Kafka event streaming and LangGraph multi-step reasoning."
        ),
        docs_url=None if settings.is_production else "/docs",
        redoc_url=None if settings.is_production else "/redoc",
        openapi_url=None if settings.is_production else "/openapi.json",
        lifespan=lifespan,
    )

    # ── Middleware ────────────────────────────────────────────────────────────
    app.add_middleware(
        CORSMiddleware,
        allow_origins=["*"] if not settings.is_production else [],
        allow_methods=["*"],
        allow_headers=["*"],
    )

    # ── Routers ───────────────────────────────────────────────────────────────
    app.include_router(api_router)

    # ── Exception handlers ────────────────────────────────────────────────────
    register_exception_handlers(app)

    return app


app = create_app()
