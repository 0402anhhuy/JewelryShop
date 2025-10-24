package com.hhd.jewelry.service.impl;


import com.hhd.jewelry.entity.*;
import com.hhd.jewelry.repository.OrderRepository;
import com.hhd.jewelry.repository.ProductRepository;
import com.hhd.jewelry.repository.ProductReviewRepository;
import com.hhd.jewelry.repository.UserRepository;
import com.hhd.jewelry.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final ProductReviewRepository reviewRepo;
    private final OrderRepository orderRepo;
    private final ProductRepository productRepo;
    private final UserRepository userRepo;

    @Override
    public void saveAll(ReviewRequest request) {
        // 🔹 Lấy đơn hàng theo ID
        Order order = orderRepo.findById(request.getOrderId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng có ID = " + request.getOrderId()));

        // 🔹 Lấy user từ đơn hàng
        User user = order.getUser();

        // 🔹 Lặp qua từng review gửi từ form
        request.getReviews().forEach(r -> {
            Product product = productRepo.findById(Long.valueOf(r.getItemId()))
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm có ID = " + r.getItemId()));

            ProductReview review = ProductReview.builder()
                    .order(order)
                    .user(user)
                    .product(product)
                    .rating(r.getRating())
                    .comment(r.getComment())
                    .mediaPaths(r.getMediaPaths())
                    .build();

            reviewRepo.save(review);
        });
    }
}
