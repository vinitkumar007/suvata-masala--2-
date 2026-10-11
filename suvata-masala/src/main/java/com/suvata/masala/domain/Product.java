package com.suvata.masala.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 80)
    private String slug;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(length = 120)
    private String nameHindi;

    @Column(nullable = false, length = 40)
    private String category;

    @Column(nullable = false, length = 1000)
    private String description;

    /** Pack ka vazan grams me, jaise 100, 250, 500. */
    @Column(nullable = false)
    private int weightGrams;

    /** Price paise me store hota hai, taaki paise ka hisaab float ki galti na kare. */
    @Column(nullable = false)
    private long pricePaise;

    @Column(nullable = false)
    private boolean inStock = true;

    /** Placeholder tile ka rang. Asli photo aane par imageUrl use hoga. */
    @Column(length = 16)
    private String accent;

    @Column(length = 300)
    private String imageUrl;

    public Product(String slug, String name, String nameHindi, String category, String description,
                   int weightGrams, long pricePaise, String accent) {
        this.slug = slug;
        this.name = name;
        this.nameHindi = nameHindi;
        this.category = category;
        this.description = description;
        this.weightGrams = weightGrams;
        this.pricePaise = pricePaise;
        this.accent = accent;
    }
}
