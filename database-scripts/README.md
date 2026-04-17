# Database Scripts

This folder contains database initialization scripts and schema documentation for the Aferent healthcare platform.

## Files

- `postgres-init.sql` - PostgreSQL DDL script for telemedicine-service database
- `mongodb-schemas.md` - Documentation of MongoDB collections and schemas
- `setup-instructions.md` - Complete guide for setting up all databases

## Usage

See `setup-instructions.md` for detailed setup procedures.

## Notes

- PostgreSQL requires manual script execution
- MongoDB schemas are created automatically by Spring Boot
- Redis and MinIO are configured via Docker Compose