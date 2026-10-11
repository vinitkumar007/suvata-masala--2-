package com.suvata.masala.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SignatureUtilTest {

    private static final String SECRET = "test_secret_key";

    @Test
    void validPaymentSignatureIsAccepted() {
        String orderId = "order_TestABC123";
        String paymentId = "pay_TestXYZ789";
        String signature = SignatureUtil.hmacSha256Hex(SECRET, orderId + "|" + paymentId);

        assertTrue(SignatureUtil.isValidPaymentSignature(SECRET, orderId, paymentId, signature));
    }

    @Test
    void tamperedPaymentIdIsRejected() {
        String signature = SignatureUtil.hmacSha256Hex(SECRET, "order_A|pay_1");

        assertFalse(SignatureUtil.isValidPaymentSignature(SECRET, "order_A", "pay_2", signature));
    }

    @Test
    void wrongSecretIsRejected() {
        String signature = SignatureUtil.hmacSha256Hex("other_secret", "order_A|pay_1");

        assertFalse(SignatureUtil.isValidPaymentSignature(SECRET, "order_A", "pay_1", signature));
    }

    @Test
    void missingSecretOrSignatureIsRejected() {
        assertFalse(SignatureUtil.isValidPaymentSignature("", "order_A", "pay_1", "abc"));
        assertFalse(SignatureUtil.isValidPaymentSignature(SECRET, "order_A", "pay_1", null));
    }

    @Test
    void webhookSignatureMatchesRawBody() {
        String body = "{\"event\":\"payment.captured\"}";
        String signature = SignatureUtil.hmacSha256Hex("webhook_secret", body);

        assertTrue(SignatureUtil.isValidWebhookSignature("webhook_secret", body, signature));
        assertFalse(SignatureUtil.isValidWebhookSignature("webhook_secret", body + " ", signature));
    }
}
