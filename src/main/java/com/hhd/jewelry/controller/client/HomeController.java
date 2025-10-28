package com.hhd.jewelry.controller.client;

import com.hhd.jewelry.entity.Cart;
import com.hhd.jewelry.entity.Collection;
import com.hhd.jewelry.entity.Product;
import com.hhd.jewelry.entity.User;
import com.hhd.jewelry.service.CartService;
import com.hhd.jewelry.service.CollectionService;
import com.hhd.jewelry.service.ProductService;
import com.hhd.jewelry.service.UserService;
import com.hhd.jewelry.service.specification.ProductSpecs;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.*;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private final ProductService productService;
    private final CollectionService collectionService;
    private final CartService cartService;
    private final UserService userService;


    @GetMapping("/")
    public String home(
            @RequestParam(required = false) String collectionName,
            @RequestParam(required = false) String sort,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "4") int size,
            Model model,
            Authentication auth) {

        List<Collection> collections = collectionService.getAllCollections();
        Specification<Product> spec = Specification.where(null);
        Sort sorting = parseSort(sort);

        if (collectionName != null && !collectionName.isEmpty()) {
            spec = spec.and(ProductSpecs.collectionName(collectionName));

            model.addAttribute("selectedCollection", collectionName);

            collections.stream()
                    .filter(c -> c.getName().equalsIgnoreCase(collectionName))
                    .findFirst()
                    .ifPresent(selected -> model.addAttribute("selectedImage", selected.getImageUrl()));
        }

        List<Product> allFilteredProducts = productService.findAllProducts(spec, sorting);

        Pageable pageable = PageRequest.of(page, size, sorting);
        Page<Product> productPage = productService.findProducts(spec, pageable);


        int cartSize = 0;
        if (auth != null && auth.isAuthenticated()) {
            User user = userService.findByEmail(auth.getName()).orElse(null);
            if (user != null) {
                Cart cart = cartService.getCartByUser(user);
                if (cart != null && cart.getItems() != null) {
                    cartSize = cart.getItems().stream()
                            .map(item -> item.getQuantity() != null ? item.getQuantity() : 0)
                            .reduce(0, Integer::sum);
                }
            }
        }

        model.addAttribute("allFilteredProducts", allFilteredProducts);
        model.addAttribute("productPage", productPage);
        model.addAttribute("products", productPage.getContent());

        model.addAttribute("collections", collections);
        model.addAttribute("cartSize", cartSize);
        model.addAttribute("selectedSort", sort);

        Map<String, Object> params = new LinkedHashMap<>();
        if (collectionName != null) params.put("collectionName", collectionName);
        if (sort != null) params.put("sort", sort);
        model.addAttribute("params", params);

        return "client_1/homepage/home";
    }

    private Sort parseSort(String sort) {
        if (sort == null) return Sort.by(Sort.Direction.DESC, "createdAt");
        return switch (sort) {
            case "priceAsc" -> Sort.by(Sort.Direction.ASC, "price");
            case "priceDesc" -> Sort.by(Sort.Direction.DESC, "price");
            case "bestSelling" -> Sort.by(Sort.Direction.DESC, "order");
            case "newest" -> Sort.by(Sort.Direction.DESC, "createdAt");
            default -> Sort.by(Sort.Direction.DESC, "id");
        };
    }
}