package com.hhd.jewelry.controller.client;

import com.hhd.jewelry.entity.Product;
import com.hhd.jewelry.entity.User;
import com.hhd.jewelry.entity.Wishlist;
import com.hhd.jewelry.repository.ProductRepository;
import com.hhd.jewelry.repository.UserRepository;
import com.hhd.jewelry.repository.WishlistRepository;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/recently-viewed")
@RequiredArgsConstructor
public class RecentlyViewedController {

    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final WishlistRepository wishlistRepository;

    // ===== HIỂN THỊ TRANG RECENTLY VIEWED =====
    @GetMapping
    public String viewRecentlyViewed(Model model, Authentication auth, HttpSession session) {
        if (auth == null) {
            return "redirect:/login";
        }

        User user = userRepository.findByEmail(auth.getName()).orElse(null);
        if (user == null) {
            return "redirect:/login";
        }

        // Lấy danh sách sản phẩm đã xem từ session
        List<Integer> recentlyViewedIds = (List<Integer>) session.getAttribute("recentlyViewed");
        List<Product> recentlyViewed = new ArrayList<>();

        if (recentlyViewedIds != null && !recentlyViewedIds.isEmpty()) {
            recentlyViewed = recentlyViewedIds.stream()
                    .map(id -> productRepository.findById(Long.valueOf(id)).orElse(null))
                    .filter(product -> product != null)
                    .collect(Collectors.toList());
        }

        model.addAttribute("recentlyViewed", recentlyViewed);
        model.addAttribute("recentlyViewedCount", recentlyViewed.size());

        // Lấy số lượng wishlist
        List<Wishlist> wishlists = wishlistRepository.findAllByUser(user);
        model.addAttribute("wishlistCount", wishlists.size());

        return "client_1/product/recently-viewed";
    }

    // ===== XÓA TOÀN BỘ LỊCH SỬ =====
    @PostMapping("/clear")
    @ResponseBody
    public String clearRecentlyViewed(HttpSession session) {
        session.removeAttribute("recentlyViewed");
        return "success";
    }
}