package com.hhd.jewelry.controller.admin;

import com.hhd.jewelry.entity.Collection;
import com.hhd.jewelry.entity.Product;
import com.hhd.jewelry.service.CollectionService;
import com.hhd.jewelry.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
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
@RequestMapping("/admin/collections")
@RequiredArgsConstructor
public class AdminCollectionController {
    private final CollectionService collectionService;
    private final ProductService productService;

    @GetMapping
    public String listCollections(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "5") int size,
            @RequestParam(value = "keyword", required = false) String keyword,
            Model model
    ) {
        var pageable = org.springframework.data.domain.PageRequest.of(page - 1, size);
        Page<Collection> collectionPage = collectionService.getCollectionsPage(page, size);

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

        return "admin/collections/list";
    }


    @GetMapping("/form")
    public String addForm(Model model) {
        model.addAttribute("collection", new Collection());
        model.addAttribute("isEdit", false);
        model.addAttribute("products", productService.getAllProducts());
        return "admin/collections/form";
    }


    @GetMapping("/form/{id}")
    public String editForm(@PathVariable Integer id, Model model) {
        Collection collection = collectionService.getCollectionById(Long.valueOf(id))
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bộ sưu tập"));

        collection.getProducts().size();

        model.addAttribute("collection", collection);
        model.addAttribute("isEdit", true);
        model.addAttribute("products", productService.getAllProducts());
        return "admin/collections/form";
    }

    @PostMapping("/save")
    public String save(@ModelAttribute Collection form,
                       @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
                       @RequestParam(value = "productIds", required = false) List<Long> productIds) throws IOException {

        Collection collection;
        boolean isEdit = form.getId() != null;

        if (isEdit) {
            collection = collectionService.getCollectionById(Long.valueOf(form.getId()))
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy bộ sưu tập"));
        } else {
            collection = new Collection();
        }

        collection.setName(form.getName());

        if (productIds != null && !productIds.isEmpty()) {
            List<Product> selectedProducts = productService.getProductsById(productIds);
            for (Product p : selectedProducts) {
                p.setCollection(collection);
            }
            collection.setProducts(selectedProducts);
        } else {
            collection.getProducts().clear();
        }

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

        collectionService.save(collection);
        return "redirect:/admin/collections";
    }

    private String getFileExtension(String fileName) {
        int index = fileName.lastIndexOf('.');
        return index > 0 ? fileName.substring(index) : "";
    }

    @GetMapping("/delete/{id}")
    public String delete(@PathVariable Integer id) {
        collectionService.deleteById(Long.valueOf(id));
        return "redirect:/admin/collections";
    }
}
