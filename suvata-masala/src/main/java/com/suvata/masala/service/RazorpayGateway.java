package com.suvata.masala.service;

import com.suvata.masala.config.AppProperties;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

/**
 * Razorpay ke REST API se baat karne wala client.
 * Docs: https://razorpay.com/docs/api/orders/create
 */
@Component
public class RazorpayGateway {

    private final AppProperties.Razorpay config;
    private final RestClient restClient;

    public RazorpayGateway(AppProperties props) {
        this.config = props.razorpay();
        this.restClient = RestClient.builder()
                .baseUrl(config.baseUrl())
                .defaultHeader("Authorization", basicAuth(config.keyId(), config.keySecret()))
                .build();
    }

    public boolean isConfigured() {
        return config.configured();
    }

    public String keyId() {
        return config.keyId();
    }

    /**
     * Razorpay par order banata hai. Amount paise me hona chahiye.
     * Returns: Razorpay order id (order_xxxx).
     */
    public String createOrder(String receipt, long amountPaise, String currency) {
        if (!isConfigured()) {
            throw new PaymentUnavailableException("Payment gateway abhi configure nahi hai. RAZORPAY_KEY_ID aur RAZORPAY_KEY_SECRET set karein.");
        }
        try {
            Map<?, ?> response = restClient.post()
                    .uri("/orders")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "amount", amountPaise,
                            "currency", currency,
                            "receipt", receipt
                    ))
                    .retrieve()
                    .body(Map.class);
            if (response == null || response.get("id") == null) {
                throw new PaymentUnavailableException("Razorpay ne order id nahi diya");
            }
            return response.get("id").toString();
        } catch (RestClientException e) {
            throw new PaymentUnavailableException("Razorpay se order banane mein dikkat aayi", e);
        }
    }

    private static String basicAuth(String user, String pass) {
        String raw = (user == null ? "" : user) + ":" + (pass == null ? "" : pass);
        return "Basic " + Base64.getEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }
}
