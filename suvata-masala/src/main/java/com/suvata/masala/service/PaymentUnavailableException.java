package com.suvata.masala.service;

/** Jab Razorpay keys set nahi hain ya gateway se baat nahi ho paa rahi. */
public class PaymentUnavailableException extends RuntimeException {

    public PaymentUnavailableException(String message) {
        super(message);
    }

    public PaymentUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
