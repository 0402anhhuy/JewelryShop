package com.hhd.jewelry.service.impl;

import com.hhd.jewelry.entity.*;
import com.hhd.jewelry.repository.OrderRepository;
import com.hhd.jewelry.repository.ProductRepository;
import com.hhd.jewelry.repository.ProductReviewRepository;
import com.hhd.jewelry.repository.UserRepository;
import com.hhd.jewelry.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value; // ⭐ THÊM MỚI
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {
    private static final Logger log = LoggerFactory.getLogger(ReviewServiceImpl.class);
    private final ProductReviewRepository reviewRepo;
    private final OrderRepository orderRepo;
    private final ProductRepository productRepo;
    private final UserRepository userRepo;

    // ⭐ 1. Tiêm đường dẫn từ application-dev.properties
    @Value("${file.upload-dir}")
    private String uploadDir; // Sẽ là "C:/my-app-uploads/"

    // ⭐ 2. Định nghĩa thư mục con cho ảnh reviews
    private static final String REVIEW_SUBFOLDER_WEB = "images/reviews";
    private static final Path REVIEW_SUBFOLDER_PHYSICAL = Paths.get("images", "reviews");

    @Override
    public void saveAll(ReviewRequest request, Map<Integer, List<MultipartFile>> filesMap) {
        Order order = orderRepo.findById(request.getOrderId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng có ID = " + request.getOrderId()));

        User user = order.getUser();

        request.getReviews().forEach(r -> {
            Product product = productRepo.findById(Long.valueOf(r.getItemId()))
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm có ID = " + r.getItemId()));

            List<String> mediaPaths = new ArrayList<>();
            List<MultipartFile> files = filesMap.get(r.getItemId());

            if (files != null && !files.isEmpty()) {
                // ⭐ Sẽ lưu file vào thư mục bên ngoài
                mediaPaths = uploadFiles(files);
            }

            ProductReview review = ProductReview.builder()
                    .order(order)
                    .user(user)
                    .product(product)
                    .rating(r.getRating())
                    .comment(r.getComment())
                    .mediaPaths(mediaPaths) // ⭐ Lưu đường dẫn web vào DB
                    .build();

            reviewRepo.save(review);
            log.info("--- (Service) Đã lưu review vào DB cho Product ID: {}. Số lượng ảnh: {}", r.getItemId(), mediaPaths.size()); // ⭐ LOG SAU KHI LƯU DB
            // ⭐ XÓA CACHE SAU KHI LƯU
            clearProductCache(product.getSerialNumber());
        });
    }
    @CacheEvict(cacheNames = "productDetails", key = "#serialNumber")
    public void clearProductCache(String serialNumber) {
        log.info("✅ (Service) Đã xóa cache cho sản phẩm: {}", serialNumber); // ⭐ Sửa System.out
    }
    /**
     * ⭐ (ĐÃ SỬA) Upload files vào thư mục bên ngoài và trả về list đường dẫn web
     */
    private List<String> uploadFiles(List<MultipartFile> files) {
        List<String> paths = new ArrayList<>();

        try {
            // ⭐ 4. (SỬA) Lấy đường dẫn gốc TUYỆT ĐỐI (từ "./uploads/" -> "/var/app/uploads")
            Path rootAbsoluteUploadPath = Paths.get(this.uploadDir).toAbsolutePath();

            // ⭐ 5. (SỬA) Tạo đường dẫn vật lý đầy đủ cho thư mục review
            //    (ví dụ: "/var/app/uploads" + "images/reviews" -> "/var/app/uploads/images/reviews")
            Path physicalUploadDir = rootAbsoluteUploadPath.resolve(REVIEW_SUBFOLDER_PHYSICAL);

            // Tạo thư mục nếu chưa tồn tại
            if (!Files.exists(physicalUploadDir)) {
                Files.createDirectories(physicalUploadDir);
                log.info("✅ (Service-upload) Đã tạo thư mục: {}", physicalUploadDir);
            }

            for (MultipartFile file : files) {
                String originalFilename = file.getOriginalFilename();
                log.info("--- (Service-upload) Đang xử lý file: {}", originalFilename);
                try {
                    String extension = "";

                    if (originalFilename != null && originalFilename.contains(".")) {
                        extension = originalFilename.substring(originalFilename.lastIndexOf("."));
                    }
                    String newFilename = UUID.randomUUID().toString() + extension;

                    // ⭐ 6. (SỬA) Đường dẫn VẬT LÝ để lưu file
                    //    (ví dụ: /var/app/uploads/images/reviews/ten-file-uuid.jpg)
                    Path physicalFilePath = physicalUploadDir.resolve(newFilename);
                    log.info("--- (Service-upload) Đường dẫn lưu file vật lý: {}", physicalFilePath);
                    Files.write(physicalFilePath, file.getBytes());

                    // ⭐ 7. (SỬA) Đường dẫn WEB để lưu vào DB (khớp với WebConfig)
                    //    (ví dụ: /uploads/images/reviews/ten-file-uuid.jpg)
                    String webPath = "/uploads/" + REVIEW_SUBFOLDER_WEB + "/" + newFilename;
                    paths.add(webPath);

                    log.info("✅ (Service-upload) Đã upload thành công file {} thành {}", originalFilename, webPath);
                } catch (IOException e) {
                    log.error("❌ (Service-upload) LỖI: Không thể ghi file {}. Nguyên nhân:", originalFilename, e);
                    e.printStackTrace();
                }
            }

        } catch (IOException e) {
            log.error("❌ (Service-upload) LỖI NGHIÊM TRỌNG: Không thể tạo thư mục gốc. Nguyên nhân:", e);
            e.printStackTrace();
            return paths; // Trả về list rỗng
        }

        log.info("--- (Service-upload) Hoàn tất xử lý {} file(s). Upload thành công {} file(s).", files.size(), paths.size());
        return paths;
    }
}