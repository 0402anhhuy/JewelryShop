package com.hhd.jewelry.controller.client;

import com.hhd.jewelry.entity.Product;
import com.hhd.jewelry.entity.User;
import com.hhd.jewelry.entity.Wishlist;
import com.hhd.jewelry.repository.ProductRepository;
import com.hhd.jewelry.repository.UserRepository;
import com.hhd.jewelry.repository.WishlistRepository;
import com.hhd.jewelry.service.WishlistService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/wishlist")
@RequiredArgsConstructor
public class WishlistController {

    private final WishlistService wishlistService;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final WishlistRepository wishlistRepository;

    // ===== HIỂN THỊ TRANG WISHLIST =====
    @GetMapping
    public String viewWishlist(Model model, Authentication auth, HttpSession session) {
        if (auth == null) {
            return "redirect:/login";
        }

        User user = userRepository.findByEmail(auth.getName()).orElse(null);
        if (user == null) {
            return "redirect:/login";
        }

        // Lấy danh sách wishlist
        List<Wishlist> wishlists = wishlistService.getAllByUser(user);
        model.addAttribute("wishlists", wishlists);
        model.addAttribute("wishlistCount", wishlists.size());

        // Lấy số lượng recently viewed từ session
        List<Integer> recentlyViewedIds = (List<Integer>) session.getAttribute("recentlyViewed");
        int recentlyViewedCount = (recentlyViewedIds != null) ? recentlyViewedIds.size() : 0;
        model.addAttribute("recentlyViewedCount", recentlyViewedCount);

        return "client_1/product/wishlist";
    }

    // ===== API TOGGLE WISHLIST (AJAX) =====
    @PostMapping("/toggle/{productId}")
    @ResponseBody
    public String toggleWishlist(@PathVariable Integer productId, Authentication authentication) {
        if (authentication == null) return "unauthorized";

        User user = userRepository.findByEmail(authentication.getName()).orElse(null);
        if (user == null) return "unauthorized";

        Product product = productRepository.findById(Long.valueOf(productId)).orElse(null);
        if (product == null) return "not_found";

        Optional<Wishlist> existing = wishlistRepository.findByUserAndProduct(user, product);
        if (existing.isPresent()) {
            wishlistRepository.delete(existing.get());
            return "removed";
        }

        wishlistRepository.save(new Wishlist(null, user, product));
        return "added";
    }

    // ===== XÓA SẢN PHẨM KHỎI WISHLIST =====
    @PostMapping("/remove/{productId}")
    public String removeFromWishlist(@PathVariable Integer productId, Authentication auth) {
        if (auth == null) {
            return "redirect:/login";
        }

        User user = userRepository.findByEmail(auth.getName()).orElse(null);
        Product product = productRepository.findById(Long.valueOf(productId)).orElse(null);

        if (user != null && product != null) {
            Optional<Wishlist> existing = wishlistRepository.findByUserAndProduct(user, product);
            existing.ifPresent(wishlistRepository::delete);
        }

        return "redirect:/wishlist";
    }
    @PostMapping("/remove/account/{productId}")
    public String removeWishlistFromAccount(@PathVariable Integer productId, Authentication auth) {
        if (auth == null) {
            return "redirect:/login";
        }

        User user = userRepository.findByEmail(auth.getName()).orElse(null);
        Product product = productRepository.findById(Long.valueOf(productId)).orElse(null);

        if (user != null && product != null) {
            Optional<Wishlist> existing = wishlistRepository.findByUserAndProduct(user, product);
            existing.ifPresent(wishlistRepository::delete);
        }

        return "redirect:/account";
    }
}