package com.suvata.masala.dto;

import com.suvata.masala.domain.OrderStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

/** Sab API ke request aur response ke shape yahin hain. */
public final class ApiModels {

    private ApiModels() {
    }

    /* ---------- public ---------- */

    public record ProductView(
            Long id,
            String slug,
            String name,
            String nameHindi,
            String category,
            String description,
            int weightGrams,
            long pricePaise,
            boolean inStock,
            String accent,
            String imageUrl
    ) {
    }

    public record CartLine(
            @NotNull Long productId,
            @Min(1) @Max(20) int quantity
    ) {
    }

    public record CheckoutRequest(
            @NotBlank @Size(max = 100) String customerName,
            @NotBlank @Email @Size(max = 150) String email,
            @NotBlank @Pattern(regexp = "^[6-9]\\d{9}$", message = "10 digit ka valid mobile number dein") String phone,
            @NotBlank @Size(max = 300) String addressLine,
            @NotBlank @Size(max = 80) String city,
            @NotBlank @Size(max = 80) String state,
            @NotBlank @Pattern(regexp = "^\\d{6}$", message = "6 digit ka pincode dein") String pincode,
            @NotEmpty @Valid List<CartLine> items
    ) {
    }

    public record CheckoutResponse(
            String orderRef,
            String razorpayOrderId,
            String razorpayKeyId,
            long amountPaise,
            String currency,
            String customerName,
            String email,
            String phone
    ) {
    }

    public record PaymentVerifyRequest(
            @NotBlank String orderRef,
            @NotBlank String razorpayOrderId,
            @NotBlank String razorpayPaymentId,
            @NotBlank String razorpaySignature
    ) {
    }

    public record OrderLineView(String productName, int quantity, long unitPricePaise, long lineTotalPaise) {
    }

    public record OrderView(
            String orderRef,
            OrderStatus status,
            long subtotalPaise,
            long shippingPaise,
            long totalPaise,
            Instant createdAt,
            List<OrderLineView> items
    ) {
    }

    public record ContactRequest(
            @NotBlank @Size(max = 100) String name,
            @NotBlank @Email @Size(max = 150) String email,
            @Pattern(regexp = "^$|^[6-9]\\d{9}$", message = "valid mobile number dein") String phone,
            @NotBlank @Size(min = 5, max = 1000) String message
    ) {
    }

    public record ApiError(String message, List<String> details) {
    }

    /* ---------- admin ---------- */

    public record AdminOrderView(
            String orderRef,
            OrderStatus status,
            String customerName,
            String email,
            String phone,
            String deliveryAddress,
            long totalPaise,
            String razorpayPaymentId,
            String trackingNumber,
            Instant createdAt,
            Instant paidAt,
            List<OrderLineView> items
    ) {
    }

    public record ShipRequest(
            @Size(max = 60) String trackingNumber
    ) {
    }

    public record ProductUpsert(
            @NotBlank @Size(max = 80) @Pattern(regexp = "^[a-z0-9-]+$", message = "slug sirf chhote letters, numbers aur - ho sakta hai") String slug,
            @NotBlank @Size(max = 120) String name,
            @Size(max = 120) String nameHindi,
            @NotBlank @Size(max = 40) String category,
            @NotBlank @Size(max = 1000) String description,
            @Min(1) int weightGrams,
            @Min(100) long pricePaise,
            boolean inStock,
            @Size(max = 16) String accent,
            @Size(max = 300) String imageUrl
    ) {
    }
}
