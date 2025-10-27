package com.hhd.jewelry.controller.manager;

import com.hhd.jewelry.entity.Order;
import com.hhd.jewelry.entity.Product;
import com.hhd.jewelry.entity.User;
import com.hhd.jewelry.repository.OrderRepository;
import com.hhd.jewelry.repository.ProductRepository;
import com.hhd.jewelry.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j  // ✅ Thêm logging
@Controller
@RequiredArgsConstructor
public class ManagerDashboardController {

    private final UserRepository userRepo;
    private final ProductRepository productRepo;
    private final OrderRepository orderRepo;

    @GetMapping("/manager/dashboard")
    @Transactional(readOnly = true)  // ✅ ĐẢM BẢO LUÔN ĐỌC DATA MỚI NHẤT
    public String dashboard(Model model, Authentication auth) {

        // === 1. Tổng số liệu cơ bản ===
        long totalUsers = userRepo.count();
        long totalProducts = productRepo.count();
        long totalOrders = orderRepo.count();

        // === 2. Tính doanh thu (chỉ tính đơn PAID, COMPLETED, DELIVERED - loại trừ CANCELLED) ===
        List<Order> allOrders = orderRepo.findAll();

        double totalRevenue = allOrders.stream()
                .filter(o -> o.getStatus() != Order.Status.CANCELLED
                        && o.getStatus() != Order.Status.RETURNED)
                .mapToDouble(o -> o.getTotalAmount() != null ? o.getTotalAmount().doubleValue() : 0.0)
                .sum();

        // === 3. Đơn hàng bị hủy (tính tổng tiền mất) ===
        long cancelledOrders = allOrders.stream()
                .filter(o -> o.getStatus() == Order.Status.CANCELLED)
                .count();

        double cancelledRevenue = allOrders.stream()
                .filter(o -> o.getStatus() == Order.Status.CANCELLED)
                .mapToDouble(o -> o.getTotalAmount() != null ? o.getTotalAmount().doubleValue() : 0.0)
                .sum();

        // === 4. Đơn hàng mới nhất (5 đơn gần nhất) ===
        List<Order> recentOrders = orderRepo.findAll(
                PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "createdAt"))
        ).getContent();

        // === 5. Doanh thu 12 tháng gần nhất ===
        Map<String, Double> monthlyRevenue = calculateMonthlyRevenue(allOrders);

        // === 6. Thống kê trạng thái đơn hàng ===
        Map<String, Long> orderStatusCount = allOrders.stream()
                .collect(Collectors.groupingBy(
                        o -> o.getStatus().name(),
                        Collectors.counting()
                ));

        // === 7. Lấy tên user ===
        String email = auth != null ? auth.getName() : null;
        String fullName = "Quản trị viên";

        if (email != null) {
            User user = userRepo.findByEmail(email).orElse(null);
            if (user != null && user.getFullName() != null && !user.getFullName().isBlank()) {
                fullName = user.getFullName();
            }
        }

        // === 8. ✅ SẢN PHẨM SẮP HẾT HÀNG (ĐỌC LẠI TỪ DB) ===
        List<Product> lowStockProducts = productRepo.findTop5ByStockQuantityLessThanOrderByStockQuantityAsc(10);

        // ✅ LOG ĐỂ DEBUG
        log.info("📊 Dashboard - Tìm thấy {} sản phẩm sắp hết hàng:", lowStockProducts.size());
        lowStockProducts.forEach(p ->
                log.info("   - ID: {}, Tên: {}, Tồn kho: {}", p.getId(), p.getName(), p.getStockQuantity())
        );

        model.addAttribute("lowStockProducts", lowStockProducts);

        // === Gán sang Thymeleaf ===
        model.addAttribute("totalUsers", totalUsers);
        model.addAttribute("totalProducts", totalProducts);
        model.addAttribute("totalOrders", totalOrders);
        model.addAttribute("totalRevenue", totalRevenue);
        model.addAttribute("cancelledOrders", cancelledOrders);
        model.addAttribute("cancelledRevenue", cancelledRevenue);
        model.addAttribute("recentOrders", recentOrders);
        model.addAttribute("monthlyRevenue", monthlyRevenue);
        model.addAttribute("orderStatusCount", orderStatusCount);
        model.addAttribute("page", "dashboard");
        model.addAttribute("username", fullName);

        return "manager/dashboard";
    }

    // === Helper: Tính doanh thu 12 tháng gần nhất ===
    private Map<String, Double> calculateMonthlyRevenue(List<Order> orders) {
        Map<String, Double> result = new LinkedHashMap<>();
        LocalDateTime now = LocalDateTime.now();

        for (int i = 11; i >= 0; i--) {
            YearMonth month = YearMonth.from(now.minusMonths(i));
            String label = month.getMonth().getValue() + "/" + month.getYear();

            double revenue = orders.stream()
                    .filter(o -> {
                        if (o.getCreatedAt() == null) return false;
                        YearMonth orderMonth = YearMonth.from(o.getCreatedAt());
                        return orderMonth.equals(month)
                                && o.getStatus() != Order.Status.CANCELLED
                                && o.getStatus() != Order.Status.RETURNED;
                    })
                    .mapToDouble(o -> o.getTotalAmount() != null ? o.getTotalAmount().doubleValue() : 0.0)
                    .sum();

            result.put(label, revenue);
        }

        return result;
    }
}