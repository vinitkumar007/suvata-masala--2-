package com.suvata.masala.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.suvata.masala.config.AppProperties;
import com.suvata.masala.domain.CustomerOrder;
import com.suvata.masala.domain.OrderItem;
import com.suvata.masala.domain.OrderStatus;
import com.suvata.masala.domain.Product;
import com.suvata.masala.dto.ApiModels.AdminOrderView;
import com.suvata.masala.dto.ApiModels.CartLine;
import com.suvata.masala.dto.ApiModels.CheckoutRequest;
import com.suvata.masala.dto.ApiModels.CheckoutResponse;
import com.suvata.masala.dto.ApiModels.OrderLineView;
import com.suvata.masala.dto.ApiModels.OrderView;
import com.suvata.masala.dto.ApiModels.PaymentVerifyRequest;
import com.suvata.masala.repo.OrderRepository;
import com.suvata.masala.repo.ProductRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private static final String REF_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final ProductRepository products;
    private final OrderRepository orders;
    private final RazorpayGateway gateway;
    private final AppProperties props;
    private final ObjectMapper json;
    private final OrderNotifier notifier;

    public OrderService(ProductRepository products, OrderRepository orders, RazorpayGateway gateway,
                        AppProperties props, ObjectMapper json, OrderNotifier notifier) {
        this.products = products;
        this.orders = orders;
        this.gateway = gateway;
        this.props = props;
        this.json = json;
        this.notifier = notifier;
    }

    /**
     * Checkout: price server ke database se aata hai, client ka bheja price kabhi trust nahi hota.
     * Order PENDING_PAYMENT state mein save hota hai, phir Razorpay par order banta hai.
     */
    @Transactional
    public CheckoutResponse placeOrder(CheckoutRequest req) {
        if (!gateway.isConfigured()) {
            throw new PaymentUnavailableException("Online payment abhi active nahi hai. Kripya thodi der baad try karein.");
        }

        Map<Long, Product> byId = loadProducts(req.items());
        CustomerOrder order = new CustomerOrder();
        order.setOrderRef(newUniqueOrderRef());
        order.setCustomerName(req.customerName().trim());
        order.setEmail(req.email().trim().toLowerCase());
        order.setPhone(req.phone());
        order.setAddressLine(req.addressLine().trim());
        order.setCity(req.city().trim());
        order.setState(req.state().trim());
        order.setPincode(req.pincode());

        long subtotal = 0;
        for (CartLine line : req.items()) {
            Product p = byId.get(line.productId());
            if (p == null) {
                throw new IllegalArgumentException("Product nahi mila: " + line.productId());
            }
            if (!p.isInStock()) {
                throw new IllegalArgumentException(p.getName() + " abhi stock mein nahi hai");
            }
            OrderItem item = new OrderItem(p.getId(), p.getName(), p.getPricePaise(), line.quantity());
            order.addItem(item);
            subtotal += item.getLineTotalPaise();
        }

        long shipping = subtotal >= props.freeShippingThresholdPaise() ? 0 : props.shippingPaise();
        long total = subtotal + shipping;
        order.setSubtotalPaise(subtotal);
        order.setShippingPaise(shipping);
        order.setTotalPaise(total);

        String razorpayOrderId = gateway.createOrder(order.getOrderRef(), total, props.currency());
        order.setRazorpayOrderId(razorpayOrderId);
        orders.save(order);

        return new CheckoutResponse(
                order.getOrderRef(),
                razorpayOrderId,
                gateway.keyId(),
                total,
                props.currency(),
                order.getCustomerName(),
                order.getEmail(),
                order.getPhone()
        );
    }

    /** Browser checkout ke success ke baad signature verify karta hai. */
    @Transactional
    public OrderView verifyPayment(PaymentVerifyRequest req) {
        CustomerOrder order = orders.findByOrderRef(req.orderRef())
                .orElseThrow(() -> new NoSuchElementException("Order nahi mila"));

        if (!req.razorpayOrderId().equals(order.getRazorpayOrderId())) {
            throw new IllegalArgumentException("Payment is order se match nahi karti");
        }
        boolean valid = SignatureUtil.isValidPaymentSignature(
                props.razorpay().keySecret(), req.razorpayOrderId(), req.razorpayPaymentId(), req.razorpaySignature());
        if (!valid) {
            throw new IllegalArgumentException("Payment signature galat hai");
        }
        markPaid(order, req.razorpayPaymentId());
        return toView(order);
    }

    /**
     * Razorpay webhook (payment.captured). Server-to-server hota hai, isliye
     * browser band hone par bhi order paid ho jata hai.
     */
    @Transactional
    public void handleWebhook(String rawBody, String signature) {
        if (!SignatureUtil.isValidWebhookSignature(props.razorpay().webhookSecret(), rawBody, signature)) {
            throw new IllegalArgumentException("Webhook signature galat hai");
        }
        JsonNode root = parse(rawBody);
        if (!"payment.captured".equals(root.path("event").asText())) {
            return; // dusre events ko ignore karte hain
        }
        JsonNode payment = root.path("payload").path("payment").path("entity");
        String razorpayOrderId = payment.path("order_id").asText();
        String paymentId = payment.path("id").asText();
        orders.findByRazorpayOrderId(razorpayOrderId)
                .ifPresent(order -> markPaid(order, paymentId));
    }

    @Transactional(readOnly = true)
    public OrderView getOrder(String orderRef) {
        return orders.findByOrderRef(orderRef)
                .map(this::toView)
                .orElseThrow(() -> new NoSuchElementException("Order nahi mila"));
    }

    /* ---------- admin ---------- */

    @Transactional(readOnly = true)
    public List<AdminOrderView> listOrdersForAdmin() {
        return orders.findAll(Sort.by(Sort.Direction.DESC, "createdAt")).stream()
                .map(this::toAdminView)
                .toList();
    }

    /** Sirf PAID orders ko ship kiya ja sakta hai. */
    @Transactional
    public AdminOrderView markShipped(String orderRef, String trackingNumber) {
        CustomerOrder order = orders.findByOrderRef(orderRef)
                .orElseThrow(() -> new NoSuchElementException("Order nahi mila"));
        if (order.getStatus() != OrderStatus.PAID) {
            throw new IllegalArgumentException("Sirf paid order hi ship ho sakta hai. Abhi status: " + order.getStatus());
        }
        order.setStatus(OrderStatus.SHIPPED);
        order.setTrackingNumber(trackingNumber == null || trackingNumber.isBlank() ? null : trackingNumber.trim());
        order.setShippedAt(Instant.now());
        orders.save(order);
        notifier.orderShipped(order);
        return toAdminView(order);
    }

    /* ---------- helpers ---------- */

    /** Idempotent: ek hi payment dobara aaye to kuch nahi badalta. */
    private void markPaid(CustomerOrder order, String paymentId) {
        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            return;
        }
        order.setStatus(OrderStatus.PAID);
        order.setRazorpayPaymentId(paymentId);
        order.setPaidAt(Instant.now());
        orders.save(order);
        notifier.orderPaid(order);
    }

    private Map<Long, Product> loadProducts(List<CartLine> lines) {
        List<Long> ids = lines.stream().map(CartLine::productId).distinct().toList();
        return products.findAllById(ids).stream()
                .collect(Collectors.toMap(Product::getId, p -> p, (a, b) -> a, HashMap::new));
    }

    private OrderView toView(CustomerOrder o) {
        return new OrderView(o.getOrderRef(), o.getStatus(), o.getSubtotalPaise(), o.getShippingPaise(),
                o.getTotalPaise(), o.getCreatedAt(), lineViews(o));
    }

    private AdminOrderView toAdminView(CustomerOrder o) {
        return new AdminOrderView(o.getOrderRef(), o.getStatus(), o.getCustomerName(), o.getEmail(), o.getPhone(),
                o.getAddressLine() + ", " + o.getCity() + ", " + o.getState() + " - " + o.getPincode(),
                o.getTotalPaise(), o.getRazorpayPaymentId(), o.getTrackingNumber(), o.getCreatedAt(),
                o.getPaidAt(), lineViews(o));
    }

    private List<OrderLineView> lineViews(CustomerOrder o) {
        return o.getItems().stream()
                .map(i -> new OrderLineView(i.getProductName(), i.getQuantity(), i.getUnitPricePaise(), i.getLineTotalPaise()))
                .toList();
    }

    private JsonNode parse(String body) {
        try {
            return json.readTree(body.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalArgumentException("Webhook body galat hai", e);
        }
    }

    private String newUniqueOrderRef() {
        for (int attempt = 0; attempt < 5; attempt++) {
            StringBuilder sb = new StringBuilder("SMM-");
            for (int i = 0; i < 8; i++) {
                sb.append(REF_CHARS.charAt(RANDOM.nextInt(REF_CHARS.length())));
            }
            String ref = sb.toString();
            if (!orders.existsByOrderRef(ref)) {
                return ref;
            }
        }
        throw new IllegalStateException("Order number generate nahi ho paya, dobara koshish karein");
    }
}
