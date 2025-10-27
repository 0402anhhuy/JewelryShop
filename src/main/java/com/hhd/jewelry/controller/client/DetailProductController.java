package com.hhd.jewelry.controller.client;

import com.hhd.jewelry.entity.Product;
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
import java.util.List;

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
        Product product = productRepository.findBySerialNumber(serialNumber)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        // ===== LƯU VÀO RECENTLY VIEWED =====
        List<Integer> recentlyViewedIds = (List<Integer>) session.getAttribute("recentlyViewed");
        if (recentlyViewedIds == null) {
            recentlyViewedIds = new ArrayList<>();
        }

        // Xóa nếu đã tồn tại (để đưa lên đầu)
        recentlyViewedIds.remove(product.getId());

        // Thêm vào đầu danh sách
        recentlyViewedIds.add(0, product.getId());

        // Giới hạn 20 sản phẩm
        if (recentlyViewedIds.size() > 20) {
            recentlyViewedIds = recentlyViewedIds.subList(0, 20);
        }

        session.setAttribute("recentlyViewed", recentlyViewedIds);

        model.addAttribute("product", product);
        return "client_1/product/detail";
    }
}
