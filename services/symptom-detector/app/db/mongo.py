"""
app/db/mongo.py
───────────────
Minimal Motor client lifecycle helpers.
"""

from __future__ import annotations

from motor.motor_asyncio import AsyncIOMotorClient, AsyncIOMotorDatabase

from app.core.config import get_settings

_client: AsyncIOMotorClient | None = None


async def connect() -> None:
    global _client
    if _client is None:
        _client = AsyncIOMotorClient(get_settings().mongo_uri)


async def disconnect() -> None:
    global _client
    if _client is not None:
        _client.close()
        _client = None


def get_client() -> AsyncIOMotorClient:
    if _client is None:
        raise RuntimeError("Mongo client has not been initialized.")
    return _client


def get_database() -> AsyncIOMotorDatabase:
    settings = get_settings()
    return get_client()[settings.mongo_db_name]
