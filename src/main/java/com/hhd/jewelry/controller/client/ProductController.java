package com.hhd.jewelry.controller.client;

import com.hhd.jewelry.entity.*;
import com.hhd.jewelry.entity.Collection;
import com.hhd.jewelry.repository.*;
import com.hhd.jewelry.service.OrderService;
import com.hhd.jewelry.service.specification.ProductSpecs;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;


import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class ProductController {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final CollectionRepository collectionRepository;
    private final UserRepository userRepository;
    private final WishlistRepository wishlistRepository;
    private final OrderService orderService;
    @GetMapping("/products")
    public String products(
            @RequestParam(value = "q", required = false) String q,
            @RequestParam(value = "catName", required = false) List<String> categoryNames,
            @RequestParam(value = "material", required = false) List<String> materials,
            @RequestParam(value = "collectionName", required = false) String collectionName,
            @RequestParam(value = "minPrice", required = false) BigDecimal minPrice,
            @RequestParam(value = "maxPrice", required = false) BigDecimal maxPrice,
            @RequestParam(value = "sort", defaultValue = "newest") String sort,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "12") int size,
            Model model,
            Authentication auth)
    {
        // ===== 1. XÂY DỰNG SPECIFICATION =====
        Specification<Product> spec = Specification.where(null);

        // Tìm kiếm theo từ khóa
        if (q != null && !q.isBlank()) {
            spec = spec.and(ProductSpecs.keywordSafe(q));
        }

        // Lọc theo danh mục
        if (categoryNames != null && !categoryNames.isEmpty()) {
            spec = spec.and(ProductSpecs.categoryNames(categoryNames));
        }

        // Lọc theo chất liệu (material)
        if (materials != null && !materials.isEmpty()) {
            spec = spec.and((root, query, cb) -> {
                List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
                for (String mat : materials) {
                    predicates.add(cb.like(cb.lower(root.get("material")), "%" + mat.toLowerCase() + "%"));
                }
                return cb.or(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
            });
        }

        // Lọc theo bộ sưu tập
        if (collectionName != null && !collectionName.isBlank()) {
            spec = spec.and(ProductSpecs.collectionName(collectionName));
        }

        // Lọc theo khoảng giá
        if (minPrice != null || maxPrice != null) {
            spec = spec.and(ProductSpecs.priceBetween(minPrice, maxPrice));
        }

        // ===== 2. SẮP XẾP =====
        Sort sorting = parseSort(sort);
        Pageable pageable = PageRequest.of(page, size, sorting);

        // ===== 3. TRUY VẤN DATABASE =====
        Page<Product> productPage = productRepository.findAll(spec, pageable);

        // ===== 4. LẤY DỮ LIỆU CHO FILTER =====
        List<Category> allCategories = categoryRepository.findAll();
        List<Collection> allCollections = collectionRepository.findAll();

        // Danh sách chất liệu (có thể hard-code hoặc lấy từ DB)
        List<String> allMaterials = Arrays.asList(
                "Vàng 18K", "Vàng 14K", "Vàng 10K",
                "Bạc 925", "Kim cương", "Ngọc trai"
        );

        // ===== ✅ SỬA PHẦN NÀY - Lấy wishlist với kiểu dữ liệu đúng =====
        Set<Integer> wishlistedProductIds = new HashSet<>();
        if (auth != null) {
            User user = userRepository.findByEmail(auth.getName()).orElse(null);
            if (user != null) {
                List<Wishlist> wishlists = wishlistRepository.findAllByUser(user);
                wishlistedProductIds = wishlists.stream()
                        .map(w -> w.getProduct().getId())
                        .collect(Collectors.toSet());
            }
        }

        // ===== 5. GỬI DỮ LIỆU ĐẾN VIEW =====
        model.addAttribute("products", productPage.getContent());
        model.addAttribute("pageData", productPage);
        model.addAttribute("wishlistedProductIds", wishlistedProductIds); // ✅ THÊM DÒNG NÀY

        // Dữ liệu cho filter sidebar
        model.addAttribute("allCategories", allCategories);
        model.addAttribute("allCollections", allCollections);
        model.addAttribute("allMaterials", allMaterials);

        // Giá trị đã chọn (để tích checkbox)
        model.addAttribute("selectedCategories", categoryNames != null ? categoryNames : Collections.emptyList());
        model.addAttribute("selectedMaterials", materials != null ? materials : Collections.emptyList());
        model.addAttribute("selectedCollection", collectionName);
        model.addAttribute("minPrice", minPrice);
        model.addAttribute("maxPrice", maxPrice);
        model.addAttribute("keyword", q);
        model.addAttribute("currentSort", sort);

        // Params để giữ filter khi phân trang
        Map<String, Object> params = new LinkedHashMap<>();
        if (q != null) params.put("q", q);
        if (categoryNames != null) params.put("catName", categoryNames);
        if (materials != null) params.put("material", materials);
        if (collectionName != null) params.put("collectionName", collectionName);
        if (minPrice != null) params.put("minPrice", minPrice);
        if (maxPrice != null) params.put("maxPrice", maxPrice);
        params.put("sort", sort);
        model.addAttribute("params", params);

        return "client_1/product/show";
    }
    @PostMapping("/orders/delete/{orderId}")
    public ResponseEntity<Void> deleteOrder(@PathVariable Integer orderId) {
        try {
            orderService.deleteById(orderId);
            return ResponseEntity.noContent().build();
        } catch (IllegalStateException e) {
            // Trả về 400 Bad Request nếu logic không cho phép xóa (VD: sai trạng thái)
            return ResponseEntity.badRequest().build();
        }
    }
    @PostMapping("/orders/confirm-delivery/{id}")
    public ResponseEntity<?> confirmDelivery(@PathVariable Integer id) {
        try {
            Order order = orderService.updateOrderStatus(id, Order.Status.COMPLETED);
            return ResponseEntity.ok().build();
        }catch (Exception e) {
            return ResponseEntity.badRequest().body("Không thể xác nhận đơn hàng: " + e.getMessage());
        }
    }
    private Sort parseSort(String sort) {
        return switch (sort) {
            case "priceAsc" -> Sort.by(Sort.Direction.ASC, "price");
            case "priceDesc" -> Sort.by(Sort.Direction.DESC, "price");
            case "nameAsc" -> Sort.by(Sort.Direction.ASC, "name");
            case "nameDesc" -> Sort.by(Sort.Direction.DESC, "name");
            case "newest" -> Sort.by(Sort.Direction.DESC, "createdAt");
            case "bestSelling" -> Sort.by(Sort.Direction.DESC, "order");
            default -> Sort.by(Sort.Direction.DESC, "id");
        };
    }
}