package com.aferent.patient_service.exception;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.Map;

@Value
@Builder
public class ApiErrorResponse {
    LocalDateTime timestamp;
    int status;
    String error;
    String message;
    String path;
    String correlationId;
    Map<String, String> validationErrors;
}