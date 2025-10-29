package com.hhd.jewelry.service.impl;

import com.hhd.jewelry.entity.*;
import com.hhd.jewelry.repository.OrderRepository;
import com.hhd.jewelry.repository.ProductRepository;
import com.hhd.jewelry.repository.ProductReviewRepository;
import com.hhd.jewelry.repository.UserRepository;
import com.hhd.jewelry.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
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

    private final ProductReviewRepository reviewRepo;
    private final OrderRepository orderRepo;
    private final ProductRepository productRepo;
    private final UserRepository userRepo;

    // ⭐ Đường dẫn lưu file
    private static final String UPLOAD_DIR = "src/main/resources/static/images/reviews/";

    @Override
    public void saveAll(ReviewRequest request, Map<Integer, List<MultipartFile>> filesMap) {
        // 🔹 Lấy đơn hàng theo ID
        Order order = orderRepo.findById(request.getOrderId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng có ID = " + request.getOrderId()));

        // 🔹 Lấy user từ đơn hàng
        User user = order.getUser();

        // 🔹 Lặp qua từng review gửi từ form
        request.getReviews().forEach(r -> {
            Product product = productRepo.findById(Long.valueOf(r.getItemId()))
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm có ID = " + r.getItemId()));

            // ⭐ Xử lý upload file nếu có
            List<String> mediaPaths = new ArrayList<>();
            List<MultipartFile> files = filesMap.get(r.getItemId());

            if (files != null && !files.isEmpty()) {
                mediaPaths = uploadFiles(files);
            }

            // ⭐ Tạo review với mediaPaths
            ProductReview review = ProductReview.builder()
                    .order(order)
                    .user(user)
                    .product(product)
                    .rating(r.getRating())
                    .comment(r.getComment())
                    .mediaPaths(mediaPaths) // ⭐ Thêm đường dẫn file
                    .build();

            // ⭐ Lưu review vào DB (JPA tự động lưu mediaPaths vào review_media)
            reviewRepo.save(review);
        });
    }

    /**
     * ⭐ Upload files và trả về list đường dẫn
     */
    private List<String> uploadFiles(List<MultipartFile> files) {
        List<String> paths = new ArrayList<>();

        // Tạo thư mục nếu chưa tồn tại
        File uploadDir = new File(UPLOAD_DIR);
        if (!uploadDir.exists()) {
            boolean created = uploadDir.mkdirs();
            System.out.println(created ? "✅ Created directory: " + UPLOAD_DIR : "❌ Failed to create directory");
        }

        for (MultipartFile file : files) {
            if (file.isEmpty()) continue;

            try {
                // Tạo tên file unique: timestamp_uuid_originalname
                String originalFilename = file.getOriginalFilename();
                String extension = "";

                if (originalFilename != null && originalFilename.contains(".")) {
                    extension = originalFilename.substring(originalFilename.lastIndexOf("."));
                }

                String newFilename = System.currentTimeMillis() + "_" + UUID.randomUUID() + extension;

                // Lưu file vào disk
                Path filePath = Paths.get(UPLOAD_DIR + newFilename);
                Files.write(filePath, file.getBytes());

                // Lưu đường dẫn để trả về (dạng URL để hiển thị trên web)
                String webPath = "/images/reviews/" + newFilename;
                paths.add(webPath);

                System.out.println("✅ Uploaded: " + webPath);

            } catch (IOException e) {
                System.err.println("❌ Lỗi upload file: " + e.getMessage());
                e.printStackTrace();
            }
        }

        return paths;
    }
}