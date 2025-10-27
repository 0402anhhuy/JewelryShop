package com.hhd.jewelry.controller.manager;

import com.hhd.jewelry.entity.Collection;
import com.hhd.jewelry.entity.Product;
import com.hhd.jewelry.repository.CollectionRepository;
import com.hhd.jewelry.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/manager/collections")
@RequiredArgsConstructor
public class ManagerCollectionController {

    private final CollectionRepository collectionRepo;
    private final ProductRepository productRepo;

    // Danh sách có phân trang
    @GetMapping
    public String listCollections(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "5") int size,
            @RequestParam(value = "keyword", required = false) String keyword,
            Model model
    ) {
        var pageable = org.springframework.data.domain.PageRequest.of(page - 1, size);
        var collectionPage = collectionRepo.findAll(pageable);

        // ✅ Lọc theo keyword (nếu có)
        if (keyword != null && !keyword.isBlank()) {
            var filtered = collectionPage.getContent().stream()
                    .filter(c -> c.getName() != null && c.getName().toLowerCase().contains(keyword.toLowerCase()))
                    .toList();
            collectionPage = new org.springframework.data.domain.PageImpl<>(filtered, pageable, filtered.size());
        }

        model.addAttribute("collections", collectionPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", collectionPage.getTotalPages());
        model.addAttribute("keyword", keyword);

        return "manager/collections/list";
    }


    // Form thêm mới
    @GetMapping("/form")
    public String addForm(Model model) {
        model.addAttribute("collection", new Collection());
        model.addAttribute("isEdit", false);
        model.addAttribute("products", productRepo.findAll());
        return "manager/collections/form";
    }

    // Form chỉnh sửa
    @GetMapping("/form/{id}")
    public String editForm(@PathVariable Integer id, Model model) {
        Collection collection = collectionRepo.findById(Long.valueOf(id))
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bộ sưu tập"));

        // ⚡ ép Hibernate load danh sách sản phẩm để tránh LazyInitializationException
        collection.getProducts().size();

        model.addAttribute("collection", collection);
        model.addAttribute("isEdit", true);
        model.addAttribute("products", productRepo.findAll());
        return "manager/collections/form";
    }

    // Lưu
    @PostMapping("/save")
    public String save(@ModelAttribute Collection form,
                       @RequestParam(required = false) MultipartFile imageFile,
                       @RequestParam(required = false) List<Long> productIds) throws IOException {

        Collection collection;
        boolean isEdit = form.getId() != null;

        if (isEdit) {
            collection = collectionRepo.findById(Long.valueOf(form.getId()))
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy bộ sưu tập"));
        } else {
            collection = new Collection();
        }

        collection.setName(form.getName());

        // Cập nhật danh sách sản phẩm
        if (productIds != null && !productIds.isEmpty()) {
            List<Product> selectedProducts = productRepo.findAllById(productIds);
            for (Product p : selectedProducts) {
                p.setCollection(collection);
            }
            collection.setProducts(selectedProducts);
        } else {
            collection.getProducts().clear();
        }

        // Upload ảnh
        if (imageFile != null && !imageFile.isEmpty()) {
            String fileName = "collection_" + UUID.randomUUID() + getFileExtension(imageFile.getOriginalFilename());
            String[] uploadDirs = {
                    "src/main/resources/static/images/collections/",
                    "target/classes/static/images/collections/"
            };

            for (String uploadDir : uploadDirs) {
                File dir = new File(uploadDir);
                if (!dir.exists()) dir.mkdirs();
                Path filePath = Paths.get(uploadDir + fileName);
                Files.copy(imageFile.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            }

            collection.setImageUrl("/images/collections/" + fileName);
        }

        collectionRepo.save(collection);
        return "redirect:/manager/collections";
    }

    private String getFileExtension(String fileName) {
        int index = fileName.lastIndexOf('.');
        return index > 0 ? fileName.substring(index) : "";
    }

    // Xóa
    @GetMapping("/delete/{id}")
    public String delete(@PathVariable Integer id) {
        collectionRepo.deleteById(Long.valueOf(id));
        return "redirect:/manager/collections";
    }
}
