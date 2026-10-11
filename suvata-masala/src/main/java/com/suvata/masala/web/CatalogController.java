package com.suvata.masala.web;

import com.suvata.masala.domain.Inquiry;
import com.suvata.masala.domain.Product;
import com.suvata.masala.dto.ApiModels.ContactRequest;
import com.suvata.masala.dto.ApiModels.ProductView;
import com.suvata.masala.repo.InquiryRepository;
import com.suvata.masala.repo.ProductRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class CatalogController {

    private final ProductRepository products;
    private final InquiryRepository inquiries;

    public CatalogController(ProductRepository products, InquiryRepository inquiries) {
        this.products = products;
        this.inquiries = inquiries;
    }

    @GetMapping("/products")
    public List<ProductView> listProducts() {
        return products.findAllByOrderByCategoryAscNameAsc().stream().map(CatalogController::toView).toList();
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "ok");
    }

    @PostMapping("/contact")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, String> contact(@Valid @RequestBody ContactRequest req) {
        String phone = req.phone() == null ? null : req.phone().trim();
        inquiries.save(new Inquiry(req.name().trim(), req.email().trim(), phone, req.message().trim()));
        return Map.of("message", "Shukriya! Hum jald hi aapse sampark karenge.");
    }

    public static ProductView toView(Product p) {
        return new ProductView(p.getId(), p.getSlug(), p.getName(), p.getNameHindi(), p.getCategory(),
                p.getDescription(), p.getWeightGrams(), p.getPricePaise(), p.isInStock(), p.getAccent(), p.getImageUrl());
    }
}
