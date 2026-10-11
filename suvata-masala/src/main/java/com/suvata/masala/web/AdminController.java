package com.suvata.masala.web;

import com.suvata.masala.dto.ApiModels.AdminOrderView;
import com.suvata.masala.dto.ApiModels.ProductUpsert;
import com.suvata.masala.dto.ApiModels.ProductView;
import com.suvata.masala.dto.ApiModels.ShipRequest;
import com.suvata.masala.service.AdminService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Sab routes ke liye header X-Admin-Token zaroori hai. */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService admin;

    public AdminController(AdminService admin) {
        this.admin = admin;
    }

    @GetMapping("/products")
    public List<ProductView> products(@RequestHeader(value = "X-Admin-Token", required = false) String token) {
        return admin.listProducts(token);
    }

    @PostMapping("/products")
    @ResponseStatus(HttpStatus.CREATED)
    public ProductView createProduct(@RequestHeader(value = "X-Admin-Token", required = false) String token,
                                     @Valid @RequestBody ProductUpsert req) {
        return admin.createProduct(token, req);
    }

    @PutMapping("/products/{id}")
    public ProductView updateProduct(@RequestHeader(value = "X-Admin-Token", required = false) String token,
                                     @PathVariable Long id,
                                     @Valid @RequestBody ProductUpsert req) {
        return admin.updateProduct(token, id, req);
    }

    @GetMapping("/orders")
    public List<AdminOrderView> orders(@RequestHeader(value = "X-Admin-Token", required = false) String token) {
        return admin.listOrders(token);
    }

    @PostMapping("/orders/{orderRef}/ship")
    public AdminOrderView ship(@RequestHeader(value = "X-Admin-Token", required = false) String token,
                               @PathVariable String orderRef,
                               @Valid @RequestBody ShipRequest req) {
        return admin.shipOrder(token, orderRef, req.trackingNumber());
    }
}
