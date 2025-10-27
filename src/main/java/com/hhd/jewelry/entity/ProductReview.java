package com.hhd.jewelry.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "product_reviews")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // 🔹 Sản phẩm được đánh giá
    @ManyToOne
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;


    // 🔹 Người dùng đánh giá
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // 🔹 Số sao (1–5)
    @Column(nullable = false)
    private int rating;

    // 🔹 Nội dung bình luận
    @Column(length = 1000)
    private String comment;

    // 🔹 Thời gian tạo
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
