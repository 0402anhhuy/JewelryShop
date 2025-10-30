package com.hhd.jewelry.controller.client;

import com.hhd.jewelry.entity.Cart;
import com.hhd.jewelry.entity.Product;
import com.hhd.jewelry.entity.ProductReview;
import com.hhd.jewelry.entity.User;
import com.hhd.jewelry.service.CartService;
import com.hhd.jewelry.service.ProductService;
import com.hhd.jewelry.service.UserService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
@RequestMapping("/products")
public class DetailProductController {
    private final ProductService productService;
    private final CartService cartService;
    private final UserService userService;

    @GetMapping("/detail/{serialNumber}")
    public String productDetail(@PathVariable String serialNumber,
                                HttpSession session,
                                Model model,
                                Authentication auth) {

        // Lấy product
        Product product = productService.getProductBySerialNumber(serialNumber);

        int cartSize = 0;
        if (auth != null && auth.isAuthenticated()) {
            Optional<User> userOpt = userService.findByEmail(auth.getName());
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                Cart cart = cartService.getCartByUser(user);
                if (cart != null && cart.getItems() != null) {
                    cartSize = cart.getItems().stream()
                            .map(item -> item.getQuantity() != null ? item.getQuantity() : 0)
                            .reduce(0, Integer::sum);
                }
            }
        }
        model.addAttribute("cartSize", cartSize);

        List<ProductReview> reviews = new ArrayList<>();
        try {
            if (product.getReviews() != null) {
                reviews = new ArrayList<>(product.getReviews());
            }
        } catch (Exception e) {
            System.err.println("Error loading reviews: " + e.getMessage());
            e.printStackTrace();
            reviews = new ArrayList<>();
        }

        double averageRating = reviews.stream()
                .mapToInt(ProductReview::getRating)
                .average()
                .orElse(0.0);

        Map<Integer, Long> ratingCounts = new HashMap<>();
        for (int i = 1; i <= 5; i++) {
            final int star = i;
            long count = reviews.stream()
                    .filter(r -> r.getRating() == star)
                    .count();
            ratingCounts.put(star, count);
        }

        List<Integer> recentlyViewedIds = (List<Integer>) session.getAttribute("recentlyViewed");
        if (recentlyViewedIds == null) {
            recentlyViewedIds = new ArrayList<>();
        }

        recentlyViewedIds.remove(product.getId());
        recentlyViewedIds.add(0, product.getId());

        if (recentlyViewedIds.size() > 20) {
            recentlyViewedIds = recentlyViewedIds.subList(0, 20);
        }
        session.setAttribute("recentlyViewed", recentlyViewedIds);

        model.addAttribute("product", product);
        model.addAttribute("reviews", reviews);
        model.addAttribute("averageRating", averageRating);
        model.addAttribute("ratingCounts", ratingCounts);

        return "client_1/product/detail";
    }
}