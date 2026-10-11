package com.suvata.masala.web;

import com.suvata.masala.dto.ApiModels.CheckoutRequest;
import com.suvata.masala.dto.ApiModels.CheckoutResponse;
import com.suvata.masala.dto.ApiModels.OrderView;
import com.suvata.masala.dto.ApiModels.PaymentVerifyRequest;
import com.suvata.masala.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class CheckoutController {

    private final OrderService orders;

    public CheckoutController(OrderService orders) {
        this.orders = orders;
    }

    /** Frontend isi response se Razorpay checkout popup kholta hai. */
    @PostMapping("/orders")
    public ResponseEntity<CheckoutResponse> checkout(@Valid @RequestBody CheckoutRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(orders.placeOrder(req));
    }

    /** Browser payment success ke baad frontend ye call karta hai. */
    @PostMapping("/payments/verify")
    public OrderView verify(@Valid @RequestBody PaymentVerifyRequest req) {
        return orders.verifyPayment(req);
    }

    @GetMapping("/orders/{orderRef}")
    public OrderView getOrder(@PathVariable String orderRef) {
        return orders.getOrder(orderRef);
    }

    /**
     * Razorpay dashboard mein ye URL register karein:
     * https://YOUR-DOMAIN/api/payments/webhook  (event: payment.captured)
     * Raw body par signature check hota hai, isliye String mein lete hain.
     */
    @PostMapping("/payments/webhook")
    public Map<String, String> webhook(@RequestBody String rawBody,
                                       @RequestHeader(value = "X-Razorpay-Signature", required = false) String signature) {
        orders.handleWebhook(rawBody, signature);
        return Map.of("status", "ok");
    }
}
