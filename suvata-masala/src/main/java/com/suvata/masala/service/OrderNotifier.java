package com.suvata.masala.service;

import com.suvata.masala.domain.CustomerOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Order ke events par customer ko message bhejne ka hook.
 * Abhi ye sirf log karta hai. Email (SMTP) ya WhatsApp provider ka code yahin add karein.
 */
@Component
public class OrderNotifier {

    private static final Logger log = LoggerFactory.getLogger(OrderNotifier.class);

    public void orderPaid(CustomerOrder order) {
        log.info("[notify] Payment confirmed: {} -> {} ({})", order.getOrderRef(), order.getEmail(), order.getPhone());
    }

    public void orderShipped(CustomerOrder order) {
        log.info("[notify] Shipped: {} tracking={} -> {}", order.getOrderRef(), order.getTrackingNumber(), order.getEmail());
    }
}
