package com.aferent.appointment_service.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    @Bean public NewTopic appointmentBookedTopic() {
        return TopicBuilder.name("appointment.booked").partitions(1).replicas(1).build();
    }
    @Bean public NewTopic appointmentCancelledTopic() {
        return TopicBuilder.name("appointment.cancelled").partitions(1).replicas(1).build();
    }
    @Bean public NewTopic refundTriggerTopic() {
        return TopicBuilder.name("refund.trigger").partitions(1).replicas(1).build();
    }
    @Bean public NewTopic slotBookedTopic() {
        return TopicBuilder.name("slot.booked").partitions(1).replicas(1).build();
    }
    @Bean public NewTopic appointmentConfirmedTopic() {
        return TopicBuilder.name("appointment.confirmed").partitions(1).replicas(1).build();
    }
}