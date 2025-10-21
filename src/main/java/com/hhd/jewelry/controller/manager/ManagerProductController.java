package com.hhd.jewelry.controller.manager;

import com.hhd.jewelry.entity.Product;
import com.hhd.jewelry.repository.CategoryRepository;
import com.hhd.jewelry.repository.CollectionRepository;
import com.hhd.jewelry.repository.ProductRepository;
import com.hhd.jewelry.repository.SupplierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;
@Controller
@RequestMapping("/manager/products")
@RequiredArgsConstructor
@Slf4j
public class ManagerProductController {

    private final ProductRepository productRepo;
    private final CategoryRepository categoryRepo;
    private final CollectionRepository collectionRepo;
    private final SupplierRepository supplierRepo;

    // Danh sách sản phẩm
    @GetMapping({"", "/"})
    public String listProducts(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            Model model
    ) {
        int pageSize = 5;
        Pageable pageable = PageRequest.of(page - 1, pageSize, Sort.by(Sort.Direction.DESC, "id"));
        Page<Product> productPage;

        if (keyword != null && !keyword.isBlank()) {
            try {
                productPage = productRepo.findByKeyword(keyword.trim().toLowerCase(), pageable);
            } catch (Exception e) {
                productPage = productRepo.findAll(pageable);
            }
        } else {
            productPage = productRepo.findAll(pageable);
        }

        model.addAttribute("products", productPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", productPage.getTotalPages());
        model.addAttribute("keyword", keyword);
        model.addAttribute("page", "products");
        return "manager/products/list";
    }

    // Form thêm sản phẩm
    @GetMapping("/form")
    public String showAddForm(Model model) {
        model.addAttribute("product", new Product());
        model.addAttribute("categories", categoryRepo.findAll());
        model.addAttribute("collections", collectionRepo.findAll());
        model.addAttribute("suppliers", supplierRepo.findAll()); // ✅ thêm supplier
        model.addAttribute("isEdit", false);
        return "manager/products/form";
    }

    // Form chỉnh sửa sản phẩm
    @GetMapping("/form/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        Product product = productRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm"));

        model.addAttribute("product", product);
        model.addAttribute("categories", categoryRepo.findAll());
        model.addAttribute("collections", collectionRepo.findAll());
        model.addAttribute("suppliers", supplierRepo.findAll());
        model.addAttribute("isEdit", true);
        return "manager/products/form";
    }

    @PostMapping("/save")
    public String saveProduct(
            @ModelAttribute Product form,
            @RequestParam(required = false) MultipartFile[] images
    ) {
        Product product;
        boolean isEdit = form.getId() != null;

        if (isEdit) {
            product = productRepo.findById(Long.valueOf(form.getId()))
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm"));
        } else {
            product = new Product();
        }

        product.setName(form.getName());
        product.setMaterial(form.getMaterial());
        product.setBrand(form.getBrand());
        product.setSerialNumber(form.getSerialNumber());
        product.setGemstone(form.getGemstone());
        product.setPrice(form.getPrice());
        product.setDiscount(form.getDiscount());
        product.setGender(form.getGender());
        product.setStockQuantity(form.getStockQuantity());
        product.setCategory(form.getCategory());
        product.setCollection(form.getCollection());
        product.setSupplier(form.getSupplier());

        product = productRepo.save(product);
        System.out.println("✅ Sản phẩm được lưu tạm để lấy ID: " + product.getId());

        if (images != null && images.length > 0 && !images[0].isEmpty()) {

            // Xác định danh mục
            String categoryName = (product.getCategory() != null && product.getCategory().getName() != null)
                    ? product.getCategory().getName().trim().toLowerCase().replaceAll("\\s+", "")
                    : "others";

            // Ánh xạ tiếng Việt → thư mục tiếng Anh
            Map<String, String> folderMap = Map.of(
                    "vongtay", "bracelets",
                    "daychuyen", "necklaces",
                    "bongtai", "earrings",
                    "nhan", "rings",
                    "dongho", "watch",
                    "trangsucvang", "charms"
            );

            String folderCategory = folderMap.getOrDefault(categoryName, "others");
            String folderName = folderCategory + product.getId(); // ví dụ watch9

            // Hai đường dẫn lưu (src + target)
            String[] uploadDirs = {
                    "src/main/resources/static/images/categories/" + folderCategory + "/products/" + folderName + "/",
                    "target/classes/static/images/categories/" + folderCategory + "/products/" + folderName + "/"
            };

            // Xóa folder cũ (nếu có) và tạo mới
            for (String uploadDir : uploadDirs) {
                File uploadFolder = new File(uploadDir);
                if (uploadFolder.exists()) {
                    try {
                        Files.walk(uploadFolder.toPath())
                                .sorted(Comparator.reverseOrder())
                                .map(Path::toFile)
                                .forEach(File::delete);
                        System.out.println("🗑️ Đã xóa folder cũ: " + uploadDir);
                    } catch (IOException e) {
                        System.out.println("⚠️ Không thể xóa: " + uploadDir + " - " + e.getMessage());
                    }
                }
                if (!uploadFolder.mkdirs() && !uploadFolder.exists()) {
                    System.out.println("❌ Không thể tạo folder: " + uploadDir);
                    continue;
                }
                System.out.println("✅ Đã tạo folder: " + uploadDir);

                // Lưu ảnh
                int index = 1;
                for (MultipartFile image : images) {
                    if (image != null && !image.isEmpty()) {
                        try {
                            String ext = StringUtils.getFilenameExtension(image.getOriginalFilename());
                            String fileName = index++ + "." + ext;
                            Path filePath = Paths.get(uploadDir + fileName);
                            Files.copy(image.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
                            System.out.println("💾 Lưu thành công: " + uploadDir + fileName);
                        } catch (IOException e) {
                            System.out.println("❌ Lỗi lưu ảnh: " + e.getMessage());
                        }
                    }
                }
            }

            // Lưu URL ảnh vào DB
            List<String> imageUrls = new ArrayList<>();
            int index = 1;
            for (MultipartFile image : images) {
                if (image != null && !image.isEmpty()) {
                    String ext = StringUtils.getFilenameExtension(image.getOriginalFilename());
                    String fileName = index++ + "." + ext;
                    String relativePath = "/images/categories/" + folderCategory + "/products/" + folderName + "/" + fileName;
                    imageUrls.add(relativePath);
                }
            }

            product.setImageUrls(imageUrls);
            productRepo.save(product);
            System.out.println("✅ Đã lưu " + imageUrls.size() + " ảnh vào DB cho sản phẩm ID " + product.getId());
        }

        return "redirect:/manager/products";
    }


    // Xóa sản phẩm
    @GetMapping("/delete/{id}")
    public String deleteProduct(@PathVariable Long id) {
        productRepo.deleteById(id);
        return "redirect:/manager/products";
    }

    // ===== THÊM METHOD NÀY VÀO ManagerProductController.java =====

    @PostMapping("/update-prices")
    @Transactional
    public String updatePrices(
            @RequestParam(value = "productIds", required = false) List<Integer> productIds,
            @RequestParam(value = "prices", required = false) List<Integer> prices,
            RedirectAttributes redirect
    ) {
        try {
            // Kiểm tra dữ liệu đầu vào
            if (productIds == null || prices == null || productIds.isEmpty()) {
                redirect.addFlashAttribute("warning", "⚠️ Không có dữ liệu để cập nhật!");
                return "redirect:/manager/products";
            }

            if (productIds.size() != prices.size()) {
                redirect.addFlashAttribute("error", "❌ Dữ liệu không hợp lệ!");
                return "redirect:/manager/products";
            }

            int updatedCount = 0;
            List<String> updatedProducts = new ArrayList<>();

            for (int i = 0; i < productIds.size(); i++) {
                Integer productId = productIds.get(i);
                Integer newPrice = prices.get(i);

                // Bỏ qua nếu giá <= 0
                if (newPrice == null || newPrice <= 0) {
                    continue;
                }

                // Lấy product từ DB
                Product product = productRepo.findById(Long.valueOf(productId)).orElse(null);
                if (product == null) {
                    log.warn("⚠️ Không tìm thấy sản phẩm ID: {}", productId);
                    continue;
                }

                // Chỉ cập nhật nếu giá thay đổi
                Integer oldPrice = product.getPrice();
                if (!oldPrice.equals(newPrice)) {
                    product.setPrice(newPrice);
                    productRepo.save(product);

                    log.info("💰 Cập nhật giá: {} | {} ₫ → {} ₫",
                            product.getName(), oldPrice, newPrice);

                    updatedProducts.add(product.getName() + ": " +
                            String.format("%,d", oldPrice) + " ₫ → " +
                            String.format("%,d", newPrice) + " ₫");
                    updatedCount++;
                }
            }

            if (updatedCount > 0) {
                redirect.addFlashAttribute("success",
                        "✅ Đã cập nhật giá cho " + updatedCount + " sản phẩm thành công!");

                // Log chi tiết (tùy chọn)
                log.info("📋 Danh sách cập nhật giá:");
                updatedProducts.forEach(log::info);
            } else {
                redirect.addFlashAttribute("info",
                        "ℹ️ Không có giá nào thay đổi.");
            }

        } catch (Exception e) {
            log.error("❌ Lỗi khi cập nhật giá hàng loạt: ", e);
            redirect.addFlashAttribute("error",
                    "❌ Có lỗi xảy ra: " + e.getMessage());
        }

        return "redirect:/manager/products";
    }


}
