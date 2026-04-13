package com.aferent.patient_service.model;

public enum PatientDocumentType {
    PROFILE_IMAGE("profile-images", "public", false),
    MEDICAL_REPORT("medical-reports", "private", false),
    PRESCRIPTION("prescriptions", "private", false),
    QR_CODE("qr-codes", "private", false),
    SCAN("scans", "private", false);

    private final String categoryPrefix;
    private final String visibility;
    private final boolean appendUuid;

    PatientDocumentType(String categoryPrefix, String visibility, boolean appendUuid) {
        this.categoryPrefix = categoryPrefix;
        this.visibility = visibility;
        this.appendUuid = appendUuid;
    }

    public String categoryPrefix() {
        return categoryPrefix;
    }

    public String visibility() {
        return visibility;
    }

    public boolean appendUuid() {
        return appendUuid;
    }

    public static PatientDocumentType fromString(String raw) {
        if (raw == null || raw.isBlank()) {
            return MEDICAL_REPORT;
        }
        String normalized = raw.trim().replace('-', '_').replace(' ', '_').toUpperCase();
        return PatientDocumentType.valueOf(normalized);
    }
}
