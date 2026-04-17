# MongoDB Schemas for Aferent Project

This document describes the MongoDB collections and their schemas used across the microservices. MongoDB is document-based, so schemas are flexible, but these are the defined structures based on the Java models.

## auth-service (Database: auth_db)

### Collection: users
- **_id**: String (ObjectId)
- **email**: String (unique indexed)
- **passwordHash**: String
- **role**: String (enum: PATIENT/DOCTOR/ADMIN)
- **active**: Boolean (default: true)
- **refreshTokenVersion**: Long (default: 1)
- **activatedAt**: Instant
- **activatedBy**: String
- **deactivatedAt**: Instant
- **deactivatedBy**: String
- **deactivationReason**: String

## patient-service (Database: patient_db)

### Collection: patients
- **_id**: String (ObjectId)
- **authId**: String (unique indexed)
- **patientId**: String (unique indexed)
- **email**: String (unique indexed)
- **firstName**: String
- **lastName**: String
- **phone**: String
- **dateOfBirth**: String
- **gender**: String
- **bloodGroup**: String
- **address**: Embedded document
  - street: String
  - city: String
  - country: String
- **createdDate**: Date
- **lastModifiedDate**: Date

### Collection: medical_records
- **_id**: String (ObjectId)
- **patientId**: String
- **recordType**: String
- **title**: String
- **description**: String
- **fileUrl**: String
- **uploadedBy**: String
- **uploadedAt**: Date
- **createdDate**: Date
- **lastModifiedDate**: Date

### Collection: medical_history
- **_id**: String (ObjectId)
- **patientId**: String
- **condition**: String
- **diagnosisDate**: Date
- **treatment**: String
- **notes**: String
- **createdDate**: Date
- **lastModifiedDate**: Date

### Collection: patient_doctor_access
- **_id**: String (ObjectId)
- **patientId**: String
- **doctorId**: String
- **accessGranted**: Boolean
- **grantedAt**: Date
- **grantedBy**: String
- **createdDate**: Date
- **lastModifiedDate**: Date

## doctor-service (Database: doctor_db)

### Collection: doctors
- **_id**: String (ObjectId)
- **authId**: String (unique indexed)
- **doctorId**: String (unique indexed)
- **email**: String (unique indexed)
- **firstName**: String
- **lastName**: String
- **phone**: String
- **specialization**: String
- **licenseNumber**: String
- **licenseDocKey**: String
- **yearsOfExperience**: Integer
- **qualifications**: Array of Strings
- **bio**: String
- **profilePicKey**: String
- **profilePicUrl**: String
- **hospitals**: Array of Strings
- **consultationFee**: Double
- **languages**: Array of Strings
- **status**: String (RegistrationStatus enum)
- **createdDate**: Date
- **lastModifiedDate**: Date

## appointment-service (Database: appointment_db)

### Collection: appointments
- **_id**: String (ObjectId)
- **patientId**: String
- **patientName**: String
- **patientEmail**: String
- **doctorId**: String
- **doctorAuthId**: String
- **doctorName**: String
- **scheduleId**: String
- **generatedSlotId**: String
- **type**: String (AppointmentType enum)
- **status**: String (AppointmentStatus enum)
- **appointmentDate**: Date
- **appointmentNumber**: Integer
- **calculatedTime**: Time
- **hospitalName**: String
- **hospitalLocation**: String
- **videoSlotId**: String
- **videoSlotStart**: Time
- **videoSlotEnd**: Time
- **videoSessionLink**: String
- **paymentId**: String

## payment-service (Database: payment_db)

### Collection: payments
- **_id**: String (ObjectId)
- **appointmentId**: String
- **appointmentType**: String
- **patientId**: String
- **patientEmail**: String
- **doctorId**: String
- **status**: String (PaymentStatus enum)
- **amount**: Double
- **currency**: String
- **stripeSessionId**: String
- **stripePaymentIntentId**: String
- **stripeRefundId**: String
- **checkoutUrl**: String
- **createdAt**: DateTime
- **updatedAt**: DateTime
- **paidAt**: DateTime
- **refundedAt**: DateTime

## notification-service (Database: notification_db)

### Collection: notification_logs
- **_id**: String (ObjectId)
- **sourceService**: String
- **eventType**: String
- **recipient**: String
- **channel**: String (SMS/EMAIL)
- **status**: String (SENT/FAILED)
- **message**: String
- **sentAt**: DateTime

## symptom-detector (Database: symptom_db)

### Collection: specialization_prompts
- **_id**: String (ObjectId)
- **specialization**: String
- **version**: String (semantic versioning)
- **is_active**: Boolean
- **system_instruction**: String
- **author**: String
- **updated_by**: String
- **created_at**: DateTime
- **updated_at**: DateTime

### Collection: symptom_results
- **_id**: String (ObjectId)
- **request_id**: String
- **patient_id**: String or null
- **category**: String (MedicalCategory enum)
- **confidence_score**: Double
- **confidence_tier**: String (ConfidenceTier enum)
- **suggestions**: Array of Strings
- **reasoning**: String
- **verification_required**: Boolean
- **escalate_to_human**: Boolean
- **prompt_version**: String
- **workflow_metadata**: Object
- **created_at**: DateTime

**Note**: symptom_results has a TTL index set to 90 days on created_at field.