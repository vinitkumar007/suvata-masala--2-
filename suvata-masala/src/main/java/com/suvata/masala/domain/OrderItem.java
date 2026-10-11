package com.suvata.masala.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "order_items")
@Getter
@Setter
@NoArgsConstructor
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(optional = false)
    @JoinColumn(name = "order_id")
    private CustomerOrder order;

    /** Product ka naam order ke waqt save hota hai, taaki baad mein naam badalne se purana order na bigade. */
    @Column(nullable = false, length = 120)
    private String productName;

    @Column(nullable = false)
    private Long productId;

    @Column(nullable = false)
    private long unitPricePaise;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false)
    private long lineTotalPaise;

    public OrderItem(Long productId, String productName, long unitPricePaise, int quantity) {
        this.productId = productId;
        this.productName = productName;
        this.unitPricePaise = unitPricePaise;
        this.quantity = quantity;
        this.lineTotalPaise = unitPricePaise * quantity;
    }
}
