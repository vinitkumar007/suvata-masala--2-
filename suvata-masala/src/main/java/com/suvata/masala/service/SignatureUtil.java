package com.suvata.masala.service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.HexFormat;

/**
 * Razorpay ke HMAC-SHA256 signatures check karne ke liye.
 * Compare constant-time hota hai, taaki timing attack na ho.
 */
public final class SignatureUtil {

    private SignatureUtil() {
    }

    public static String hmacSha256Hex(String secret, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] raw = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(raw);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HMAC calculate nahi ho paya", e);
        }
    }

    /** Checkout ke baad: order_id|payment_id ka signature key_secret se milta hai ya nahi. */
    public static boolean isValidPaymentSignature(String keySecret, String orderId, String paymentId, String signature) {
        if (keySecret == null || keySecret.isBlank() || signature == null) {
            return false;
        }
        String expected = hmacSha256Hex(keySecret, orderId + "|" + paymentId);
        return constantTimeEquals(expected, signature);
    }

    /** Webhook ke liye: poora raw body webhook secret se sign hota hai. */
    public static boolean isValidWebhookSignature(String webhookSecret, String rawBody, String signature) {
        if (webhookSecret == null || webhookSecret.isBlank() || signature == null) {
            return false;
        }
        String expected = hmacSha256Hex(webhookSecret, rawBody);
        return constantTimeEquals(expected, signature);
    }

    static boolean constantTimeEquals(String a, String b) {
        return MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }
}
