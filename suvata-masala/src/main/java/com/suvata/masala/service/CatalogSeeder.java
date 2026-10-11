package com.suvata.masala.service;

import com.suvata.masala.domain.Product;
import com.suvata.masala.repo.ProductRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Pehli baar start hone par products ka starter catalog daalta hai.
 * Asli rate, vazan aur photos admin se ya seedha database mein badal sakte hain.
 */
@Component
public class CatalogSeeder implements CommandLineRunner {

    private final ProductRepository products;

    public CatalogSeeder(ProductRepository products) {
        this.products = products;
    }

    @Override
    public void run(String... args) {
        if (products.count() > 0) {
            return;
        }
        products.saveAll(List.of(
                new Product("marwari-lal-mirch", "Marwari Lal Mirch Powder", "मारवाड़ी लाल मिर्च पाउडर",
                        "Mirch", "Rajasthan ki tez aur rangeen lal mirch se bana powder. Sabzi aur dal mein rang aur teekha swad lata hai.",
                        250, 16500, "#9e2a1f"),
                new Product("dhaniya-powder", "Dhaniya Powder", "धनिया पाउडर",
                        "Masale", "Taaza dhaniya ke beej se pisa hua, khushbudar aur halka meetha swad.",
                        250, 11000, "#7a8a3a"),
                new Product("jeera-powder", "Jeera Powder", "जीरा पाउडर",
                        "Masale", "Bhuna hua jeera, raita, chaas aur aloo ki sabzi ke liye.",
                        250, 14500, "#b08a4e"),
                new Product("haldi-powder", "Haldi Powder", "हल्दी पाउडर",
                        "Masale", "Shuddh haldi, jisme rang aur khushboo dono gehre hain.",
                        250, 12000, "#d89a1c"),
                new Product("garam-masala", "Suvata Garam Masala", "सुवाटा गरम मसाला",
                        "Mishran", "Kai khade masalon ko dheere se bhunkar banaya gaya, roz ki sabzi aur biryani ke liye.",
                        200, 22000, "#5b2e1a"),
                new Product("chaat-masala", "Chaat Masala", "चाट मसाला",
                        "Mishran", "Khatta-meetha aur chatpata. Chaat, salad, bhel aur fruits par chhidkein.",
                        200, 13500, "#c0572b"),
                new Product("pav-bhaji-masala", "Pav Bhaji Masala", "पाव भाजी मसाला",
                        "Mishran", "Mumbai style pav bhaji ka asli swad, bina jhanjhat ke.",
                        200, 15500, "#a3401f"),
                new Product("chana-masala", "Chana Masala", "छोले मसाला",
                        "Mishran", "Chhole aur chana ke liye tayyar mix, teekha aur khushbudar.",
                        200, 17000, "#8c5a2b"),
                new Product("amchur-powder", "Amchur Powder", "आमचूर पाउडर",
                        "Masale", "Sukhe aam se bana, khatas ke liye kachhe aam ka asli swad.",
                        100, 9000, "#c9a227"),
                new Product("sambhar-masala", "Sambhar Masala", "सांभर मसाला",
                        "Mishran", "Dakshin bhartiya sambhar ke liye poore masalon ka mix.",
                        200, 18500, "#6b3d24")
        ));
    }
}
