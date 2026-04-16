// package com.aferent.appointment_service.config;

// import org.springframework.beans.factory.annotation.Value;
// import org.springframework.context.annotation.Bean;
// import org.springframework.context.annotation.Configuration;
// import org.springframework.web.reactive.function.client.WebClient;

// @Configuration
// public class WebClientConfig {

//     @Value("${services.doctor-service-url}")
//     private String doctorServiceUrl;

//     @Bean
//     public WebClient doctorServiceClient() {
//         return WebClient.builder()
//                 .baseUrl(doctorServiceUrl)
//                 .build();
//     }
// }

package com.aferent.appointment_service.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Value("${services.doctor-service-url}")
    private String doctorServiceUrl;

    @Bean
    public WebClient doctorServiceClient() {
        return WebClient.builder()
                .baseUrl(doctorServiceUrl)
                .build();
    }

    // Add this bean to provide WebClient.Builder for injection
    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }
}