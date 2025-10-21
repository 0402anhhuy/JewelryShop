package com.hhd.jewelry.controller.client;

import com.hhd.jewelry.entity.Collection;
import com.hhd.jewelry.entity.Product;
import com.hhd.jewelry.service.CollectionService;
import com.hhd.jewelry.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private final ProductService productService;
    private final CollectionService collectionService;

    @GetMapping("/")
    public String home(
            @RequestParam(required = false) String collectionName,
            @RequestParam(required = false) String sort,
            Model model) {

        List<Product> products = productService.getAllProducts();
        List<Collection> collections = collectionService.getAllCollections();

        // 🔹 Nếu có chọn collection
        if (collectionName != null && !collectionName.isEmpty()) {
            products = products.stream()
                    .filter(p -> p.getCollection() != null &&
                            p.getCollection().getName().equalsIgnoreCase(collectionName))
                    .collect(Collectors.toList());

            model.addAttribute("selectedCollection", collectionName);

            // ✅ Lấy thêm thông tin collection (ảnh, mô tả) để hiển thị banner
            collections.stream()
                    .filter(c -> c.getName().equalsIgnoreCase(collectionName))
                    .findFirst()
                    .ifPresent(selected -> {
                        model.addAttribute("selectedImage", selected.getImageUrl());
                        // Nếu bạn có description trong Collection entity, thêm dòng này:
                        // model.addAttribute("selectedDescription", selected.getDescription());
                    });
        }

        // 🔹 Sắp xếp theo sort option
        if (sort != null) {
            switch (sort) {
                case "priceAsc":
                    products.sort(Comparator.comparing(Product::getPrice));
                    break;
                case "priceDesc":
                    products.sort(Comparator.comparing(Product::getPrice).reversed());
                    break;
                case "bestSelling":
                    products.sort(Comparator.comparing(Product::getOrder).reversed());
                    break;
                case "newest":
                default:
                    products.sort(Comparator.comparing(Product::getCreatedAt).reversed());
                    break;
            }
            model.addAttribute("selectedSort", sort);
        }

        // 🔹 Gửi dữ liệu sang view
        model.addAttribute("products", products);
        model.addAttribute("collections", collections);

        return "client_1/homepage/home";
    }
}