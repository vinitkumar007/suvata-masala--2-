package com.suvata.masala.service;

import com.suvata.masala.config.AppProperties;
import com.suvata.masala.domain.Product;
import com.suvata.masala.dto.ApiModels.AdminOrderView;
import com.suvata.masala.dto.ApiModels.ProductUpsert;
import com.suvata.masala.dto.ApiModels.ProductView;
import com.suvata.masala.repo.ProductRepository;
import com.suvata.masala.web.CatalogController;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.NoSuchElementException;

/** Admin panel ke kaam: token check, products add/edit, orders dekhna. */
@Service
public class AdminService {

    private final AppProperties props;
    private final ProductRepository products;
    private final OrderService orders;

    public AdminService(AppProperties props, ProductRepository products, OrderService orders) {
        this.props = props;
        this.products = products;
        this.orders = orders;
    }

    public void requireAdmin(String token) {
        if (!props.adminEnabled()) {
            throw new AdminAccessException("Admin panel band hai. ADMIN_TOKEN set karein (kam se kam 12 characters).", true);
        }
        if (token == null || !MessageDigest.isEqual(
                token.getBytes(StandardCharsets.UTF_8), props.adminToken().getBytes(StandardCharsets.UTF_8))) {
            throw new AdminAccessException("Admin token galat hai", false);
        }
    }

    public List<ProductView> listProducts(String token) {
        requireAdmin(token);
        return products.findAllByOrderByCategoryAscNameAsc().stream()
                .map(CatalogController::toView)
                .toList();
    }

    @Transactional
    public ProductView createProduct(String token, ProductUpsert req) {
        requireAdmin(token);
        if (products.findBySlug(req.slug()).isPresent()) {
            throw new IllegalArgumentException("Ye slug pehle se hai: " + req.slug());
        }
        Product p = new Product(req.slug(), req.name().trim(), blankToNull(req.nameHindi()), req.category().trim(),
                req.description().trim(), req.weightGrams(), req.pricePaise(),
                blankToNull(req.accent()));
        p.setInStock(req.inStock());
        p.setImageUrl(blankToNull(req.imageUrl()));
        return CatalogController.toView(products.save(p));
    }

    @Transactional
    public ProductView updateProduct(String token, Long id, ProductUpsert req) {
        requireAdmin(token);
        Product p = products.findById(id).orElseThrow(() -> new NoSuchElementException("Product nahi mila"));
        products.findBySlug(req.slug())
                .filter(other -> !other.getId().equals(id))
                .ifPresent(other -> {
                    throw new IllegalArgumentException("Ye slug kisi aur product ka hai: " + req.slug());
                });
        p.setSlug(req.slug());
        p.setName(req.name().trim());
        p.setNameHindi(blankToNull(req.nameHindi()));
        p.setCategory(req.category().trim());
        p.setDescription(req.description().trim());
        p.setWeightGrams(req.weightGrams());
        p.setPricePaise(req.pricePaise());
        p.setInStock(req.inStock());
        p.setAccent(blankToNull(req.accent()));
        p.setImageUrl(blankToNull(req.imageUrl()));
        return CatalogController.toView(products.save(p));
    }

    public List<AdminOrderView> listOrders(String token) {
        requireAdmin(token);
        return orders.listOrdersForAdmin();
    }

    public AdminOrderView shipOrder(String token, String orderRef, String trackingNumber) {
        requireAdmin(token);
        return orders.markShipped(orderRef, trackingNumber);
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
