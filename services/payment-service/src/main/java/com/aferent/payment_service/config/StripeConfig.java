package com.aferent.payment_service.config;

import com.stripe.Stripe;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class StripeConfig {

    @Value("${stripe.api-key}")
    private String apiKey;

    // Runs once on startup — sets global Stripe API key
    @PostConstruct
    public void initStripe() {
        Stripe.apiKey = apiKey;
        log.info("Stripe SDK initialized");
    }
}