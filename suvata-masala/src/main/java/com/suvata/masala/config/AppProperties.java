package com.suvata.masala.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
        String brand,
        String currency,
        long shippingPaise,
        long freeShippingThresholdPaise,
        String adminToken,
        Razorpay razorpay
) {
    public record Razorpay(String keyId, String keySecret, String webhookSecret, String baseUrl) {

        /** Payment tabhi chalega jab key-id aur key-secret dono set hon. */
        public boolean configured() {
            return keyId != null && !keyId.isBlank() && keySecret != null && !keySecret.isBlank();
        }
    }

    /** Admin panel tabhi khulega jab ADMIN_TOKEN set ho. */
    public boolean adminEnabled() {
        return adminToken != null && adminToken.length() >= 12;
    }
}
