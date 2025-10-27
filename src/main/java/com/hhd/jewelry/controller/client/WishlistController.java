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
import org.springframework.http.ResponseEntity; // Import này cần thiết
import java.util.Map; // Import này cần thiết
import java.util.HashMap; // Import này cần thiết

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

    // ===== API TOGGLE WISHLIST (AJAX) - Cần sửa lại để trả về JSON (khi ADD) cho client JS cập nhật UI =====
    @PostMapping("/toggle/{productId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> toggleWishlist(@PathVariable Integer productId, Authentication authentication) {
        Map<String, Object> response = new HashMap<>();

        if (authentication == null) {
            response.put("status", "unauthorized");
            return ResponseEntity.ok(response);
        }

        User user = userRepository.findByEmail(authentication.getName()).orElse(null);
        if (user == null) {
            response.put("status", "unauthorized");
            return ResponseEntity.ok(response);
        }

        Product product = productRepository.findById(Long.valueOf(productId)).orElse(null);
        if (product == null) {
            response.put("status", "not_found");
            return ResponseEntity.ok(response);
        }

        Optional<Wishlist> existing = wishlistRepository.findByUserAndProduct(user, product);

        if (existing.isPresent()) {
            // Xóa
            wishlistRepository.delete(existing.get());
            response.put("status", "removed");
            response.put("wishlistItemId", existing.get().getId()); // Thêm ID để client có thể xác nhận
            return ResponseEntity.ok(response);
        }

        // Thêm
        Wishlist newWishlist = wishlistRepository.save(new Wishlist(null, user, product));
        response.put("status", "added");
        response.put("wishlistItemId", newWishlist.getId());

        // TODO: THÊM logic để lấy HTML fragment hoặc dữ liệu product chi tiết vào response.put("productHtml", ...)
        // Ví dụ: response.put("productHtml", renderProductFragment(newWishlist.getProduct()));

        return ResponseEntity.ok(response);
    }

    // ===== API XÓA SẢN PHẨM KHỎI WISHLIST (AJAX) - ĐÃ SỬA LẠI ĐỂ TRẢ VỀ JSON =====
    @PostMapping("/remove/{wishlistItemId}")
    @ResponseBody // Rất quan trọng: Báo Spring trả về dữ liệu (JSON) thay vì tên View
    public ResponseEntity<Map<String, Object>> deleteFromWishlist(@PathVariable Integer wishlistItemId, Authentication auth) {
        Map<String, Object> response = new HashMap<>();

        if (auth == null) {
            response.put("success", false);
            response.put("message", "Vui lòng đăng nhập.");
            return ResponseEntity.status(401).body(response);
        }

        User user = userRepository.findByEmail(auth.getName()).orElse(null);
        if (user == null) {
            response.put("success", false);
            response.put("message", "Người dùng không hợp lệ.");
            return ResponseEntity.status(401).body(response);
        }

        // 1. Tìm Wishlist Item theo ID
        Optional<Wishlist> existing = wishlistRepository.findById(wishlistItemId);

        if (existing.isEmpty() || !existing.get().getUser().equals(user)) {
            // 2. Nếu không tìm thấy hoặc không thuộc về người dùng hiện tại
            response.put("success", false);
            response.put("message", "Sản phẩm yêu thích không tồn tại hoặc bạn không có quyền xóa.");
            return ResponseEntity.status(404).body(response);
        }

        // 3. Xóa
        try {
            wishlistRepository.delete(existing.get());
            response.put("success", true);
            response.put("message", "Đã xóa sản phẩm khỏi danh sách yêu thích.");
            response.put("productId", existing.get().getProduct().getId());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Lỗi server khi xóa: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
}