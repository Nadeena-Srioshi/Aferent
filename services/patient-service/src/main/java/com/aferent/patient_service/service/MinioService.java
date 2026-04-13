package com.aferent.patient_service.service;

import io.minio.*;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class MinioService {

    private final MinioClient minioClient;

    @Value("${minio.bucket}")
    private String bucket;

    @Value("${minio.public-endpoint:}")
    private String publicEndpoint;

    @Value("${minio.access-key}")
    private String accessKey;

    @Value("${minio.secret-key}")
    private String secretKey;

    @Value("${minio.region:us-east-1}")
    private String region;

    // called on startup to make sure bucket exists
    public void ensureBucketExists() {
        try {
            boolean exists = minioClient.bucketExists(
                    BucketExistsArgs.builder().bucket(bucket).build()
            );
            if (!exists) {
                minioClient.makeBucket(
                        MakeBucketArgs.builder().bucket(bucket).build()
                );
                log.info("Created MinIO bucket: {}", bucket);
            }
        } catch (Exception e) {
            log.error("Failed to ensure bucket exists: {}", e.getMessage());
        }
    }

    // generates a URL the client uses to upload DIRECTLY to MinIO
    // the file never passes through our service or the gateway
    public String generateUploadUrl(String objectKey) {
        try {
            return getPresignClient().getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .bucket(bucket)
                            .object(objectKey)
                            .method(Method.PUT)
                            .expiry(15, TimeUnit.MINUTES)  // upload must happen within 15 min
                            .build()
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate upload URL: " + e.getMessage());
        }
    }

    // generates a URL the client uses to download directly from MinIO
    public String generateDownloadUrl(String objectKey) {
        try {
            return getPresignClient().getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .bucket(bucket)
                            .object(objectKey)
                            .method(Method.GET)
                            .expiry(1, TimeUnit.HOURS)
                            .build()
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate download URL: " + e.getMessage());
        }
    }

    private MinioClient getPresignClient() {
        if (publicEndpoint == null || publicEndpoint.isBlank()) {
            return minioClient;
        }

        try {
            return MinioClient.builder()
                    .endpoint(publicEndpoint.trim())
                    .region(region)
                    .credentials(accessKey, secretKey)
                    .build();
        } catch (Exception e) {
            log.warn("Invalid minio.public-endpoint. Falling back to internal endpoint: {}", e.getMessage());
            return minioClient;
        }
    }

    public void deleteObject(String objectKey) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectKey)
                            .build()
            );
        } catch (Exception e) {
            log.error("Failed to delete object {}: {}", objectKey, e.getMessage());
        }
    }
}