package com.hhd.jewelry.controller.client;

import com.hhd.jewelry.entity.Product;
import com.hhd.jewelry.entity.User;
import com.hhd.jewelry.entity.Wishlist;
import com.hhd.jewelry.service.ProductService;
import com.hhd.jewelry.service.UserService;
import com.hhd.jewelry.service.WishlistService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/recently-viewed")
@RequiredArgsConstructor
public class RecentlyViewedController {
    private final ProductService productService;
    private final UserService userService;
    private final WishlistService wishlistService;

    @GetMapping
    public String viewRecentlyViewed(Model model, Authentication auth, HttpSession session) {
        if (auth == null) {
            return "redirect:/login";
        }

        User user = userService.findByEmail(auth.getName()).orElse(null);
        if (user == null) {
            return "redirect:/login";
        }

        // Lấy danh sách sản phẩm đã xem từ session
        List<Integer> recentlyViewedIds = (List<Integer>) session.getAttribute("recentlyViewed");
        List<Product> recentlyViewed = new ArrayList<>();

        if (recentlyViewedIds != null && !recentlyViewedIds.isEmpty()) {
            recentlyViewed = recentlyViewedIds.stream()
                    .map(id -> productService.getProductById(Long.valueOf(id)))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        }

        model.addAttribute("recentlyViewed", recentlyViewed);
        model.addAttribute("recentlyViewedCount", recentlyViewed.size());

        List<Wishlist> wishlists = wishlistService.getAllByUser(user);
        model.addAttribute("wishlistCount", wishlists.size());

        return "client_1/product/recently-viewed";
    }

    @PostMapping("/clear")
    @ResponseBody
    public String clearRecentlyViewed(HttpSession session) {
        session.removeAttribute("recentlyViewed");
        return "success";
    }
}