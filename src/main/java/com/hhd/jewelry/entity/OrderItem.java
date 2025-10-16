package com.hhd.jewelry.entity;

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
    private Order order;

    // 🔹 Sản phẩm được mua
    @ManyToOne
    @JoinColumn(name = "product_id")
    @ToString.Exclude @EqualsAndHashCode.Exclude
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
}
