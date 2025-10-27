package com.hhd.jewelry.controller.admin;

import com.hhd.jewelry.entity.Supplier;
import com.hhd.jewelry.repository.ProductRepository;
import com.hhd.jewelry.repository.SupplierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin/suppliers")
@RequiredArgsConstructor
public class AdminSupplierController {

    private final SupplierRepository supplierRepo;
    private final ProductRepository productRepo;

    // --- Danh sách nhà cung cấp (phân trang + tìm kiếm) ---
    @GetMapping({"", "/"})
    public String listSuppliers(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "page", defaultValue = "1") int page,
            Model model
    ) {
        int pageSize = 5; // ✅ mỗi trang hiển thị 5 dòng
        Pageable pageable = PageRequest.of(page - 1, pageSize, Sort.by(Sort.Direction.DESC, "id"));
        Page<Supplier> supplierPage = supplierRepo.findAll(pageable);

        // Nếu có keyword thì lọc thủ công (nếu repo chưa có query tìm kiếm)
        if (keyword != null && !keyword.isBlank()) {
            String lowerKeyword = keyword.toLowerCase();

            var filtered = supplierPage.getContent().stream()
                    .filter(s ->
                            (s.getName() != null && s.getName().toLowerCase().contains(lowerKeyword)) ||
                                    (s.getEmail() != null && s.getEmail().toLowerCase().contains(lowerKeyword)) ||
                                    (s.getPhone() != null && s.getPhone().toLowerCase().contains(lowerKeyword))
                    )
                    .toList();

            supplierPage = new PageImpl<>(filtered, pageable, filtered.size());
        }

        // Gán tổng số sản phẩm cho từng supplier
        supplierPage.getContent().forEach(s ->
                s.setProductCount(productRepo.countBySupplierId(s.getId()))
        );

        model.addAttribute("suppliers", supplierPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", supplierPage.getTotalPages());
        model.addAttribute("keyword", keyword);
        model.addAttribute("page", "suppliers");

        return "admin/suppliers/list";
    }

    // --- Chi tiết nhà cung cấp ---
    @GetMapping("/detail/{id}")
    public String detailSupplier(@PathVariable Long id, Model model) {
        Supplier supplier = supplierRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy nhà cung cấp!"));

        supplier.setProductCount(productRepo.countBySupplierId(id));

        model.addAttribute("supplier", supplier);
        model.addAttribute("page", "suppliers");
        return "admin/suppliers/detail";
    }

    // --- Form thêm/sửa ---
    @GetMapping({"/form", "/form/{id}"})
    public String formSupplier(@PathVariable(required = false) Long id, Model model) {
        Supplier supplier = (id != null)
                ? supplierRepo.findById(id).orElse(new Supplier())
                : new Supplier();

        model.addAttribute("supplier", supplier);
        model.addAttribute("isEdit", id != null);
        model.addAttribute("page", "suppliers");
        return "admin/suppliers/form";
    }

    // --- Lưu nhà cung cấp ---
    @PostMapping("/save")
    public String saveSupplier(@ModelAttribute Supplier supplier) {
        supplierRepo.save(supplier);
        return "redirect:/admin/suppliers";
    }

    // --- Xóa nhà cung cấp ---
    @GetMapping("/delete/{id}")
    public String deleteSupplier(@PathVariable Long id) {
        supplierRepo.deleteById(id);
        return "redirect:/admin/suppliers";
    }
}
