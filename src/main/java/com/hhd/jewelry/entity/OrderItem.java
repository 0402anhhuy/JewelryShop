package com.hhd.jewelry.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "order_items")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 🔹 Thuộc về đơn hàng nào
    @ManyToOne
    @JoinColumn(name = "order_id")
    @ToString.Exclude @EqualsAndHashCode.Exclude
    @JsonBackReference
    private Order order;

    // 🔹 Sản phẩm được mua
    @ManyToOne
    @JoinColumn(name = "product_id")
    @ToString.Exclude @EqualsAndHashCode.Exclude
    @JsonIgnoreProperties({
            "hibernateLazyInitializer", "handler", // Luôn cần cho lazy loading (dù product đang EAGER)
            "orderItems",     // Product có list OrderItem -> Lặp
            "wishlists",      // Product có list Wishlist -> Lặp qua User -> Order
            "cartItems",      // Product có list CartItem -> Có thể lặp qua User
            "reviews",        // Product có list Review -> Có thể lặp qua User
            "category",       // Có thể không cần khi xem chi tiết OrderItem
            "collection",     // Có thể không cần khi xem chi tiết OrderItem
            "supplier",       // Có thể không cần
            "viewedProducts"  // Thường không cần trong OrderItem
            // Thêm các trường List/Set hoặc Entity khác trong Product nếu cần
    })
    private Product product;

    // 🔹 Số lượng
    private int quantity;

    // 🔹 Giá từng sản phẩm (có thể lưu lại tại thời điểm mua)
    private BigDecimal unitPrice;

    // 🔹 Tổng tiền dòng sản phẩm
    private BigDecimal totalPrice;

    // 🔹 Tự động cập nhật tổng khi lưu
    @PrePersist @PreUpdate
    public void calculateTotal() {
        if (unitPrice == null) unitPrice = BigDecimal.ZERO;
        this.totalPrice = unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
    @OneToOne(mappedBy = "orderItem")
    private ProductReview review;
}