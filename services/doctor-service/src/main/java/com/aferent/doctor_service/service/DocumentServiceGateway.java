package com.aferent.doctor_service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentServiceGateway {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${document-service.base-url}")
    private String documentServiceBaseUrl;

    @Value("${document-service.service-id:doctor-service}")
    private String serviceId;

    public record PresignUploadResult(String uploadUrl, String internalUploadUrl, String objectKey, String permanentUrl) {}

    public PresignUploadResult generateUploadUrl(String visibility, String category, String fileName, boolean appendUuid) {
        String safeFileName = fileName == null || fileName.isBlank() ? "file" : fileName;

        URI uri = UriComponentsBuilder
                .fromHttpUrl(documentServiceBaseUrl)
                .path("/upload/presign")
                .build()
                .toUri();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Internal-Service-ID", serviceId);

        Map<String, Object> payload = Map.of(
                "visibility", visibility,
                "category", category,
                "filename", safeFileName,
                "append_uuid", appendUuid
        );

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(uri, entity, Map.class);
            Map body = response.getBody();
            if (body == null || body.get("upload_url") == null) {
                throw new RuntimeException("Invalid response from document-service /upload/presign");
            }
            return new PresignUploadResult(
                    String.valueOf(body.get("upload_url")),
                    body.get("internal_upload_url") == null ? null : String.valueOf(body.get("internal_upload_url")),
                    body.get("object_key") == null ? null : String.valueOf(body.get("object_key")),
                    body.get("permanent_url") == null ? null : String.valueOf(body.get("permanent_url"))
            );
        } catch (Exception ex) {
            throw new RuntimeException("Failed to generate upload URL via document-service: " + ex.getMessage(), ex);
        }
    }

    public String generateDownloadUrl(String objectKey, int expiresSeconds) {
        URI uri = UriComponentsBuilder
                .fromHttpUrl(documentServiceBaseUrl)
                .path("/sign")
                .queryParam("key", objectKey)
                .queryParam("expires", expiresSeconds)
                .build()
                .toUri();

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Internal-Service-ID", serviceId);

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(uri, HttpMethod.GET, entity, Map.class);
            Map body = response.getBody();
            if (body == null || body.get("url") == null) {
                throw new RuntimeException("Invalid response from document-service /sign");
            }
            return String.valueOf(body.get("url"));
        } catch (Exception ex) {
            throw new RuntimeException("Failed to generate download URL via document-service: " + ex.getMessage(), ex);
        }
    }
}
