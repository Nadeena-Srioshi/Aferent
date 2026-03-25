"""
app/utils/exception_handlers.py
─────────────────────────────────
Maps domain exceptions → HTTP responses.
Registered once in main.py; no HTTP logic leaks into service layers.
"""

from __future__ import annotations

from fastapi import FastAPI, Request
from fastapi.responses import JSONResponse

from app.core.exceptions import (
    CategoryClassificationError,
    DatabaseError,
    LLMExecutionError,
    LLMResponseParseError,
    PromptNotFoundError,
    SymptomServiceError,
)
from app.core.logging import get_logger

logger = get_logger(__name__)


def _error_body(code: str, detail: str) -> dict:
    return {"error": {"code": code, "detail": detail}}


def register_exception_handlers(app: FastAPI) -> None:

    @app.exception_handler(CategoryClassificationError)
    async def classification_error_handler(
        request: Request, exc: CategoryClassificationError
    ) -> JSONResponse:
        logger.warning("http.classification_error", detail=str(exc))
        return JSONResponse(
            status_code=422,
            content=_error_body("CLASSIFICATION_FAILED", str(exc)),
        )

    @app.exception_handler(PromptNotFoundError)
    async def prompt_not_found_handler(
        request: Request, exc: PromptNotFoundError
    ) -> JSONResponse:
        logger.error("http.prompt_not_found", detail=str(exc))
        return JSONResponse(
            status_code=404,
            content=_error_body("PROMPT_NOT_FOUND", str(exc)),
        )

    @app.exception_handler(LLMResponseParseError)
    async def llm_parse_error_handler(
        request: Request, exc: LLMResponseParseError
    ) -> JSONResponse:
        logger.error("http.llm_parse_error", detail=str(exc))
        return JSONResponse(
            status_code=502,
            content=_error_body("LLM_PARSE_ERROR", str(exc)),
        )

    @app.exception_handler(LLMExecutionError)
    async def llm_exec_error_handler(
        request: Request, exc: LLMExecutionError
    ) -> JSONResponse:
        logger.error("http.llm_execution_error", detail=str(exc))
        return JSONResponse(
            status_code=502,
            content=_error_body("LLM_EXECUTION_ERROR", str(exc)),
        )

    @app.exception_handler(DatabaseError)
    async def database_error_handler(
        request: Request, exc: DatabaseError
    ) -> JSONResponse:
        logger.error("http.database_error", detail=str(exc))
        return JSONResponse(
            status_code=503,
            content=_error_body("DATABASE_ERROR", str(exc)),
        )

    @app.exception_handler(SymptomServiceError)
    async def generic_service_error_handler(
        request: Request, exc: SymptomServiceError
    ) -> JSONResponse:
        logger.error("http.service_error", detail=str(exc))
        return JSONResponse(
            status_code=500,
            content=_error_body("SERVICE_ERROR", str(exc)),
        )
