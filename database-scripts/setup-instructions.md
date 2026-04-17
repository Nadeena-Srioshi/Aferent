# Database Setup Instructions for Aferent Project

This guide explains how to initialize and set up the databases used in the Aferent healthcare platform.

## Overview of Databases

- **PostgreSQL**: Used by telemedicine-service for session management
- **MongoDB**: Used by most services (auth, patient, doctor, appointment, payment, notification, symptom-detector)
- **Redis**: Configured in appointment-service for caching (not actively used yet)
- **MinIO**: Object storage for documents and files

## PostgreSQL Setup

### Using Docker Compose (Recommended)

1. Start the PostgreSQL container:
   ```bash
   docker-compose up postgres-telemedicine -d
   ```

2. Run the initialization script:
   ```bash
   docker exec -i postgres-telemedicine psql -U telemedicine -d telemedicine_db < database-scripts/postgres-init.sql
   ```

### Manual Setup

1. Connect to PostgreSQL:
   ```bash
   psql -h localhost -p 5434 -U telemedicine -d telemedicine_db
   ```

2. Run the SQL script:
   ```sql
   \i database-scripts/postgres-init.sql
   ```

## MongoDB Setup

MongoDB collections are created automatically by Spring Boot applications on first use. No manual initialization is required.

### Using Docker Compose

1. Start MongoDB containers (one per service):
   ```bash
   docker-compose up mongo-auth mongo-patient mongo-doctor mongo-appointment mongo-payment mongo-notification mongo-symptom -d
   ```

### Verification

You can verify collections are created by connecting to MongoDB:
```bash
mongosh mongodb://localhost:27017/auth_db
db.users.find()
```

## Redis Setup

Redis is configured but not actively used in the current codebase.

### Using Docker Compose

```bash
docker-compose up redis -d
```

### Configuration

Redis connection is configured in `appointment-service/src/main/resources/application.yaml`:
```yaml
spring:
  data:
    redis:
      host: redis
      port: 6379
```

## MinIO Setup

MinIO is used for document storage and is initialized via Docker Compose.

### Using Docker Compose

```bash
docker-compose up minio -d
```

### Bucket Creation

Buckets are created automatically by the services. The main bucket is `app-storage`.

### Access

- Console: http://localhost:9001
- API: http://localhost:9000
- Credentials: Defined in `.env` file (MINIO_ROOT_USER, MINIO_ROOT_PASSWORD)

## Environment Variables

Make sure your `.env` file contains the necessary database URIs:

```env
# MongoDB URIs
MONGO_URI_AUTH_SERVICE=mongodb://mongo-auth:27017/auth_db
MONGO_URI_PATIENT_SERVICE=mongodb://mongo-patient:27017/patient_db
MONGO_URI_DOCTOR_SERVICE=mongodb://mongo-doctor:27017/doctor_db
MONGO_URI_APPOINTMENT_SERVICE=mongodb://mongo-appointment:27017/appointment_db
MONGO_URI_PAYMENT_SERVICE=mongodb://mongo-payment:27017/payment_db
MONGO_URI_NOTIFICATION_SERVICE=mongodb://mongo-notification:27017/notification_db
MONGO_URI_SYMPTOM_SERVICE=mongodb://mongo-symptom:27017/symptom_db

# PostgreSQL
# (Handled by telemedicine-service connection string)

# Redis
# (Configured in application.yaml)

# MinIO
MINIO_ENDPOINT=http://minio:9000
MINIO_ROOT_USER=your_minio_user
MINIO_ROOT_PASSWORD=your_minio_password
MINIO_BUCKET=app-storage
```

## Full System Startup

To start all databases and services:

```bash
# Start databases
docker-compose up -d zookeeper kafka postgres-telemedicine mongo-auth mongo-patient mongo-doctor mongo-appointment mongo-payment mongo-notification mongo-symptom redis minio

# Start services (after databases are ready)
docker-compose up -d auth-service patient-service doctor-service appointment-service payment-service notification-service telemedicine-service symptom-detector api-gateway nginx
```

## Troubleshooting

- **PostgreSQL connection issues**: Check if the container is running and ports are not conflicting.
- **MongoDB connection issues**: Ensure each service connects to its dedicated database.
- **Redis connection issues**: Verify host/port in application.yaml matches docker-compose.
- **MinIO issues**: Check credentials and network connectivity.

## Notes

- All databases use Docker containers for consistency across environments.
- Schemas are defined in code and created automatically (except PostgreSQL which uses explicit DDL).
- No data migration tools are used; schema changes should be handled carefully in production.