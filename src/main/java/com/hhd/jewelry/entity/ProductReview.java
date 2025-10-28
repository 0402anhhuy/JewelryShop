package com.hhd.jewelry.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    // 🔹 Người dùng đánh giá
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // 🔹 Đơn hàng gốc (để biết review thuộc đơn nào)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(nullable = false)
    private int rating;

    // 🔹 Nội dung bình luận
    @Column(length = 1000)
    private String comment;

    @OneToOne
    @JoinColumn(name = "order_item_id")
    private OrderItem orderItem;
    // 🔹 Thời gian tạo
    private LocalDateTime createdAt;

    // 🔹 Danh sách file đính kèm (ảnh/video)
    @ElementCollection
    @CollectionTable(name = "review_media", joinColumns = @JoinColumn(name = "review_id"))
    @Column(name = "file_path")
    private List<String> mediaPaths = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }

    // 🔹 Tiện ích thêm file
    public void addMedia(String path) {
        if (mediaPaths == null) mediaPaths = new ArrayList<>();
        mediaPaths.add(path);
    }
}