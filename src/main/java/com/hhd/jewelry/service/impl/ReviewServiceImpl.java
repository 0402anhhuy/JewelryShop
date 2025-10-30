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
    private String UPLOAD_DIR_ROOT; // Sẽ là "C:/my-app-uploads/"

    // ⭐ 2. Định nghĩa thư mục con cho ảnh reviews
    private static final String REVIEW_SUBFOLDER = "images/reviews/";

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

        // ⭐ 3. Tạo đường dẫn vật lý đầy đủ
        // Ví dụ: "C:/my-app-uploads/images/reviews/"
        Path physicalUploadDir = Paths.get(UPLOAD_DIR_ROOT + REVIEW_SUBFOLDER);

        try {
            // Tạo thư mục nếu chưa tồn tại
            if (!Files.exists(physicalUploadDir)) {
                Files.createDirectories(physicalUploadDir);
                System.out.println("✅ Tạo thư mục: " + physicalUploadDir);
            }
        } catch (IOException e) {
            log.error("❌ (Service-upload) LỖI NGHIÊM TRỌNG: Không thể tạo thư mục {}", physicalUploadDir, e);
            e.printStackTrace();
            return paths; // Trả về list rỗng
        }

        for (MultipartFile file : files) {
            String originalFilename = file.getOriginalFilename();
            log.info("--- (Service-upload) Đang xử lý file: {}", originalFilename); // ⭐ LOG TỪNG FILE
            try {
                String extension = "";

                if (originalFilename != null && originalFilename.contains(".")) {
                    extension = originalFilename.substring(originalFilename.lastIndexOf("."));
                }
                // Dùng UUID để tên file không bao giờ trùng lặp
                String newFilename = UUID.randomUUID().toString() + extension;

                // ⭐ 4. Đường dẫn VẬT LÝ để lưu file
                // Ví dụ: C:/my-app-uploads/images/reviews/ten-file-uuid.jpg
                Path physicalFilePath = physicalUploadDir.resolve(newFilename);
                log.info("--- (Service-upload) Đường dẫn lưu file vật lý: {}", physicalFilePath); // ⭐ LOG ĐƯỜNG DẪN LƯU
                Files.write(physicalFilePath, file.getBytes());

                // ⭐ 5. Đường dẫn WEB để lưu vào DB (khớp với WebConfig)
                // Ví dụ: /uploads/images/reviews/ten-file-uuid.jpg
                String webPath = "/uploads/" + REVIEW_SUBFOLDER + newFilename;
                paths.add(webPath);

                log.info("✅ (Service-upload) Đã upload thành công file {} thành {}", originalFilename, webPath); // ⭐ LOG THÀNH CÔNG
            } catch (IOException e) {
                log.error("❌ (Service-upload) LỖI NGHIÊM TRỌNG: Không thể ghi file {}. Nguyên nhân:", originalFilename, e);
                e.printStackTrace();
            }
        }
        log.info("--- (Service-upload) Hoàn tất xử lý {} file(s). Upload thành công {} file(s).", files.size(), paths.size()); // ⭐ LOG KẾT QUẢ UPLOAD
        return paths;
    }
}