package com.hhd.jewelry.controller.manager;

import com.hhd.jewelry.entity.Product;
import com.hhd.jewelry.entity.StockMovement;
import com.hhd.jewelry.repository.ManagerRepository;
import com.hhd.jewelry.repository.ProductRepository;
import com.hhd.jewelry.repository.StockMovementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.persistence.EntityManager;  // ✅ ĐỔI TỪ javax → jakarta
import jakarta.persistence.PersistenceContext;
import java.time.LocalDateTime;

@Slf4j
@Controller
@RequestMapping("/manager/stocks")
@RequiredArgsConstructor
public class ManagerStockController {

    private final StockMovementRepository stockRepo;
    private final ProductRepository productRepo;
    private final ManagerRepository managerRepo;

    @PersistenceContext
    private EntityManager entityManager;

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
        return "manager/stocks/list";
    }

    // ===== FORM THÊM GIAO DỊCH =====
    @GetMapping("/form")
    public String form(Model model) {
        model.addAttribute("movement", new StockMovement());
        model.addAttribute("products", productRepo.findAll());
        model.addAttribute("managers", managerRepo.findAll());
        model.addAttribute("isEdit", false);
        return "manager/stocks/form";
    }

    // ===== LƯU GIAO DỊCH (NHẬP / XUẤT / ĐIỀU CHỈNH) =====
    @PostMapping("/save")
    @Transactional  // ✅ Transaction sẽ tự động commit khi method kết thúc thành công
    public String save(@ModelAttribute("movement") StockMovement movement,
                       RedirectAttributes redirect) {

        try {
            // ✅ Lấy product từ DB với ID chính xác
            Integer productId = movement.getProduct().getId();
            Product product = productRepo.findById(Long.valueOf(productId))
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm ID: " + productId));

            log.info("🔍 [BEFORE] Product ID: {}, Tên: {}, Tồn kho: {}",
                    product.getId(), product.getName(), product.getStockQuantity());

            int currentStock = product.getStockQuantity() != null ? product.getStockQuantity() : 0;
            int qty = movement.getQuantity() != null ? movement.getQuantity() : 0;
            int newStock = currentStock;

            // ✅ Xử lý theo loại giao dịch
            switch (movement.getChangeType()) {
                case IMPORT -> {
                    newStock = currentStock + qty;
                    product.setStockQuantity(newStock);
                    redirect.addFlashAttribute("success", "✅ Đã nhập kho: +" + qty + " sản phẩm.");
                    log.info("📥 NHẬP KHO: +{} | {} → {}", qty, currentStock, newStock);
                }
                case SALE -> {
                    if (qty > currentStock) {
                        log.warn("❌ Xuất kho thất bại! Yêu cầu: {}, Tồn kho: {}", qty, currentStock);
                        redirect.addFlashAttribute("error",
                                "❌ Xuất kho thất bại! Số lượng tồn không đủ (" + currentStock + ").");
                        return "redirect:/manager/stocks/form";
                    }
                    newStock = currentStock - qty;
                    product.setStockQuantity(newStock);
                    redirect.addFlashAttribute("success", "📦 Đã xuất kho: -" + qty + " sản phẩm.");
                    log.info("📤 XUẤT KHO: -{} | {} → {}", qty, currentStock, newStock);
                }
                case ADJUSTMENT -> {
                    newStock = qty;
                    product.setStockQuantity(newStock);
                    redirect.addFlashAttribute("success",
                            "⚙️ Đã điều chỉnh tồn kho về: " + qty + " sản phẩm.");
                    log.info("⚙️ ĐIỀU CHỈNH: {} → {}", currentStock, newStock);
                }
            }

            // ✅ LƯU PRODUCT TRƯỚC (cập nhật stock_quantity)
            product = productRepo.save(product);
            entityManager.flush();  // Ghi xuống DB ngay lập tức
            log.info("💾 [SAVED] Product ID: {}, Tồn kho mới: {}", product.getId(), product.getStockQuantity());

            // ✅ Tạo StockMovement record
            movement.setProduct(product);
            movement.setCreatedAt(LocalDateTime.now());
            stockRepo.save(movement);
            entityManager.flush();

            log.info("✅ Đã lưu StockMovement thành công!");

            // ✅ VERIFY: Đọc lại từ DB để chắc chắn
            entityManager.clear();  // Clear cache
            Product verifyProduct = productRepo.findById(Long.valueOf(productId)).orElse(null);
            if (verifyProduct != null) {
                log.info("🔍 [VERIFY] Product ID {} có tồn kho = {} trong DB",
                        verifyProduct.getId(), verifyProduct.getStockQuantity());

                if (!verifyProduct.getStockQuantity().equals(newStock)) {
                    log.error("⚠️ WARNING: Stock không khớp! Expected: {}, Actual: {}",
                            newStock, verifyProduct.getStockQuantity());
                }
            }

        } catch (Exception e) {
            log.error("❌ LỖI khi lưu stock movement: ", e);
            redirect.addFlashAttribute("error", "❌ Có lỗi xảy ra: " + e.getMessage());
            return "redirect:/manager/stocks/form";
        }

        return "redirect:/manager/stocks";
    }

    // ===== XÓA GIAO DỊCH (Có hoàn tác tồn kho) =====
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
            entityManager.flush();

            // ✅ Xóa giao dịch
            stockRepo.deleteById(id);
            entityManager.flush();

            log.info("✅ Đã xóa giao dịch và hoàn tác tồn kho thành công");
            redirect.addFlashAttribute("success", "🗑️ Đã xóa giao dịch kho và hoàn tác số lượng thành công.");

        } catch (Exception e) {
            log.error("❌ LỖI khi xóa giao dịch: ", e);
            redirect.addFlashAttribute("error", "❌ Có lỗi xảy ra: " + e.getMessage());
        }

        return "redirect:/manager/stocks";
    }
}