package com.hhd.jewelry.controller.admin;

import com.hhd.jewelry.entity.Product;
import com.hhd.jewelry.entity.StockMovement;
import com.hhd.jewelry.repository.ManagerRepository;
import com.hhd.jewelry.repository.ProductRepository;
import com.hhd.jewelry.repository.StockMovementRepository;
import com.hhd.jewelry.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;

@Controller
@RequestMapping("/admin/stocks")
@RequiredArgsConstructor
@Slf4j
public class AdminStockController {

    private final StockMovementRepository stockRepo;
    private final ProductRepository productRepo;
    private final ManagerRepository managerRepo;
    private final UserRepository userRepo;

    // ===== DANH SÁCH GIAO DỊCH =====
    @GetMapping({"", "/"})
    public String listStocks(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "page", defaultValue = "1") int page,
            Model model
    ) {
        int pageSize = 5;
        Pageable pageable = PageRequest.of(page - 1, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<StockMovement> stockPage = stockRepo.findAll(pageable);

        if (keyword != null && !keyword.isBlank()) {
            String lower = keyword.toLowerCase();
            var filtered = stockPage.getContent().stream()
                    .filter(m ->
                            (m.getProduct() != null && m.getProduct().getDisplayName().toLowerCase().contains(lower)) ||
                                    (m.getManager() != null && m.getManager().getFullName().toLowerCase().contains(lower))
                    )
                    .toList();
            stockPage = new PageImpl<>(filtered, pageable, filtered.size());
        }

        model.addAttribute("movements", stockPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", stockPage.getTotalPages());
        model.addAttribute("keyword", keyword);
        model.addAttribute("page", "stocks");
        return "admin/stocks/list";
    }

    // ===== FORM THÊM GIAO DỊCH =====
    @GetMapping("/form")
    public String form(Model model) {
        model.addAttribute("movement", new StockMovement());
        model.addAttribute("products", productRepo.findAll());
        model.addAttribute("managers", managerRepo.findAll());
        model.addAttribute("isEdit", false);
        return "admin/stocks/form";
    }

    // ===== LƯU GIAO DỊCH (NHẬP / XUẤT / ĐIỀU CHỈNH) =====
    @PostMapping("/save")
    public String save(@ModelAttribute("movement") StockMovement movement,
                       RedirectAttributes redirect) {

        Product product = productRepo.findById(Long.valueOf(movement.getProduct().getId()))
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm."));

        int currentStock = product.getStockQuantity() != null ? product.getStockQuantity() : 0;
        int qty = movement.getQuantity() != null ? movement.getQuantity() : 0;

        switch (movement.getChangeType()) {
            case IMPORT -> {
                product.setStockQuantity(currentStock + qty);
                redirect.addFlashAttribute("success", "✅ Đã nhập kho: +" + qty + " sản phẩm.");
            }
            case SALE -> {
                if (qty > currentStock) {
                    redirect.addFlashAttribute("error",
                            "❌ Xuất kho thất bại! Số lượng tồn không đủ (" + currentStock + ").");
                    return "redirect:/admin/stocks/form";
                }
                product.setStockQuantity(currentStock - qty);
                redirect.addFlashAttribute("success", "📦 Đã xuất kho: -" + qty + " sản phẩm.");
            }
            case ADJUSTMENT -> {
                product.setStockQuantity(qty);
                redirect.addFlashAttribute("success",
                        "⚙️ Đã điều chỉnh tồn kho về: " + qty + " sản phẩm.");
            }
        }

        productRepo.save(product);

        movement.setCreatedAt(LocalDateTime.now());
        stockRepo.save(movement);

        return "redirect:/admin/stocks";
    }

    // ===== XÓA GIAO DỊCH =====
    @GetMapping("/delete/{id}")
    @Transactional
    public String delete(@PathVariable Integer id, RedirectAttributes redirect) {
        try {
            StockMovement movement = stockRepo.findById(id)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy giao dịch"));

            Product product = movement.getProduct();
            int qty = movement.getQuantity() != null ? movement.getQuantity() : 0;
            int currentStock = product.getStockQuantity() != null ? product.getStockQuantity() : 0;

            log.info("🗑️ Xóa giao dịch ID: {}, Loại: {}, Số lượng: {}",
                    id, movement.getChangeType(), qty);

            // ✅ Hoàn tác số lượng
            switch (movement.getChangeType()) {
                case IMPORT -> {
                    product.setStockQuantity(Math.max(0, currentStock - qty));
                    log.info("↩️ Hoàn tác NHẬP: -{} | {} → {}", qty, currentStock, product.getStockQuantity());
                }
                case SALE -> {
                    product.setStockQuantity(currentStock + qty);
                    log.info("↩️ Hoàn tác XUẤT: +{} | {} → {}", qty, currentStock, product.getStockQuantity());
                }
                case ADJUSTMENT -> {
                    redirect.addFlashAttribute("warning",
                            "⚠️ Không thể tự động hoàn tác giao dịch điều chỉnh. Vui lòng kiểm tra lại tồn kho.");
                    log.warn("⚠️ Không thể hoàn tác giao dịch ADJUSTMENT");
                }
            }

            // ✅ Lưu product sau khi hoàn tác
            productRepo.save(product);


            // ✅ Xóa giao dịch
            stockRepo.deleteById(id);

            log.info("✅ Đã xóa giao dịch và hoàn tác tồn kho thành công");
            redirect.addFlashAttribute("success", "🗑️ Đã xóa giao dịch kho và hoàn tác số lượng thành công.");

        } catch (Exception e) {
            log.error("❌ LỖI khi xóa giao dịch: ", e);
            redirect.addFlashAttribute("error", "❌ Có lỗi xảy ra: " + e.getMessage());
        }

        return "redirect:/admin/stocks";
    }
}
