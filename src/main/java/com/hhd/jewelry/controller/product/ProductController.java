package com.hhd.jewelry.controller.product;

import com.hhd.jewelry.entity.Product;
import com.hhd.jewelry.repository.ProductRepository;
import com.hhd.jewelry.service.ProductService;
import com.hhd.jewelry.service.specification.ProductSpecs;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.util.*;

@Controller
@RequiredArgsConstructor
public class ProductController {

    private final ProductRepository productRepository;
    private final ProductService productService;

    @GetMapping("/products")
    public String products(
            @RequestParam(value = "q", required = false) String q,
            @RequestParam(value = "category", required = false) List<Long> category,
            @RequestParam(value = "catName", required = false) List<String> categoryName,
            @RequestParam(value = "priceRange", required = false) String priceRange,
            @RequestParam(value = "minPrice", required = false) BigDecimal minPrice,
            @RequestParam(value = "maxPrice", required = false) BigDecimal maxPrice,
            @RequestParam(value = "sort", defaultValue = "") String sort,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "9") int size,
            Model model)
    {

        // 1) LUÔN chuẩn bị params để view không lỗi
        Map<String, Object> params = new LinkedHashMap<>();
        if (q != null) params.put("q", q);
        if (category != null) params.put("category", category);
        if (categoryName != null) params.put("categoryName", categoryName);
        if (priceRange != null) params.put("priceRange", priceRange);
        if (minPrice != null) params.put("minPrice", minPrice);
        if (maxPrice != null) params.put("maxPrice", maxPrice);
        if (!sort.isBlank()) params.put("sort", sort);
        model.addAttribute("params", params);

        // 3) Có filter → Specification + Pageable
        Specification<Product> priceSpec = Optional
                .ofNullable(ProductSpecs.priceRange(priceRange))
                .orElse(ProductSpecs.priceBetween(minPrice, maxPrice));

        Specification<Product> spec = Specification.allOf(
                ProductSpecs.keywordSafe(q),
                ProductSpecs.categoryIds(category),
                ProductSpecs.categoryNames(categoryName),
                priceSpec
        );

        Pageable pageable = PageRequest.of(page, size, parseSort(sort));
        Page<Product> result = productRepository.findAll(spec, pageable);

        model.addAttribute("products", result.getContent());
        model.addAttribute("pageData", result);
        return "client_1/product/show";
    }

    private Sort parseSort(String sort) {
        return switch (sort) {
            case "priceAsc"  -> Sort.by(Sort.Direction.ASC, "price");
            case "priceDesc" -> Sort.by(Sort.Direction.DESC, "price");
            case "nameAsc"   -> Sort.by(Sort.Direction.ASC, "name");
            case "nameDesc"  -> Sort.by(Sort.Direction.DESC, "name");
            default          -> Sort.unsorted();
        };
    }
}
