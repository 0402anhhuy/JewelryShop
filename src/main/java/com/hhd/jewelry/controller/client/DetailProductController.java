package com.hhd.jewelry.controller.client;

import com.hhd.jewelry.entity.Product;
import com.hhd.jewelry.entity.ProductReview;
import com.hhd.jewelry.repository.ProductRepository;
import com.hhd.jewelry.service.ProductService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
@RequestMapping("/products")
public class DetailProductController {
    private final ProductService productService;
    private final ProductRepository productRepository;

    @GetMapping("/detail/{serialNumber}")
    public String productDetail(@PathVariable String serialNumber,
                                HttpSession session,
                                Model model) {
        // Lấy product
        Product product = productRepository.findBySerialNumber(serialNumber)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm"));

        // Force trigger lazy loading nếu cần
        List<ProductReview> reviews = new ArrayList<>();
        try {
            if (product.getReviews() != null) {
                product.getReviews().size(); // Trigger lazy loading
                reviews = new ArrayList<>(product.getReviews());

                // ===== THÊM ĐOẠN NÀY ĐỂ FIX ĐƯỜNG DẪN ẢNH =====
                reviews.forEach(review -> {
                    if (review.getMediaPaths() != null && !review.getMediaPaths().isEmpty()) {
                        List<String> fixedPaths = new ArrayList<>();
                        for (String path : review.getMediaPaths()) {
                            // Chuyển từ "uploads/reviews/..." sang "/images/reviews/..."
                            String fixedPath = path.replace("uploads/reviews/", "/images/reviews/");
                            fixedPaths.add(fixedPath);
                        }
                        review.setMediaPaths(fixedPaths);
                    }
                });
                // ===== HẾT ĐOẠN THÊM =====
            }
        } catch (Exception e) {
            System.err.println("Error loading reviews: " + e.getMessage());
            reviews = new ArrayList<>();
        }

        // Tính average rating
        double averageRating = 0.0;
        if (!reviews.isEmpty()) {
            averageRating = reviews.stream()
                    .mapToInt(ProductReview::getRating)
                    .average()
                    .orElse(0.0);
        }

        // ===== ĐẾM SỐ LƯỢNG REVIEW THEO TỪNG RATING (5-4-3-2-1 sao) =====
        Map<Integer, Long> ratingCounts = new HashMap<>();
        for (int i = 1; i <= 5; i++) {
            final int star = i;
            long count = reviews.stream()
                    .filter(r -> r.getRating() == star)
                    .count();
            ratingCounts.put(star, count);
        }

        // ====== Recently viewed ======
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

        // ====== Truyền dữ liệu vào view ======
        model.addAttribute("product", product);
        model.addAttribute("reviews", reviews);
        model.addAttribute("averageRating", averageRating);
        model.addAttribute("ratingCounts", ratingCounts);

        return "client_1/product/detail";
    }
}