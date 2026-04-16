package com.aferent.payment_service.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    // Topics this service PRODUCES
    @Bean
    public NewTopic paymentSuccessTopic() {
        return TopicBuilder.name("payment.success").partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic paymentFailedTopic() {
        return TopicBuilder.name("payment.failed").partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic refundCompletedTopic() {
        return TopicBuilder.name("refund.completed").partitions(1).replicas(1).build();
    }
}
