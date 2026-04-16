package com.aferent.patient_service.kafka;

import com.aferent.patient_service.service.PatientDocumentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class MinioUploadEventConsumer {

    private final PatientDocumentService patientDocumentService;

    @KafkaListener(topics = "${app.upload-events.topic:minio.events}", groupId = "patient-service-group")
    public void onObjectCreated(ConsumerRecord<String, Map<String, Object>> record) {
        Map<String, Object> event = record.value();
        for (String objectKey : extractObjectKeys(event)) {
            patientDocumentService.markUploadedByObjectKey(objectKey);
        }
    }

    private List<String> extractObjectKeys(Map<String, Object> event) {
        List<String> keys = new ArrayList<>();
        if (event == null || event.isEmpty()) {
            return keys;
        }

        Object recordsObj = event.get("Records");
        if (recordsObj instanceof List<?> records) {
            for (Object recObj : records) {
                if (!(recObj instanceof Map<?, ?> recordMap)) {
                    continue;
                }

                Object eventNameObj = ((Map<?, ?>) recObj).get("eventName");
                String eventName = eventNameObj == null ? "" : String.valueOf(eventNameObj);
                if (!eventName.startsWith("s3:ObjectCreated")) {
                    continue;
                }

                Object s3Obj = ((Map<?, ?>) recObj).get("s3");
                if (!(s3Obj instanceof Map<?, ?> s3Map)) {
                    continue;
                }
                Object objectObj = s3Map.get("object");
                if (!(objectObj instanceof Map<?, ?> objectMap)) {
                    continue;
                }
                Object keyObj = objectMap.get("key");
                if (keyObj != null) {
                    keys.add(decode(String.valueOf(keyObj)));
                }
            }
            return keys;
        }

        Object singleKey = event.get("Key");
        Object singleEventName = event.get("EventName");
        if (singleKey != null && singleEventName != null && String.valueOf(singleEventName).startsWith("s3:ObjectCreated")) {
            keys.add(decode(String.valueOf(singleKey)));
        }

        return keys;
    }

    private String decode(String raw) {
        try {
            return URLDecoder.decode(raw, StandardCharsets.UTF_8);
        } catch (Exception ex) {
            log.debug("Failed to URL-decode object key '{}': {}", raw, ex.getMessage());
            return raw;
        }
    }
}
