package com.hhd.jewelry.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    @ToString.Exclude @EqualsAndHashCode.Exclude
    private User user;

    @Enumerated(EnumType.STRING)
    private Status status = Status.PENDING;

    private LocalDateTime createdAt = LocalDateTime.now();

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    @ToString.Exclude @EqualsAndHashCode.Exclude
    private List<OrderItem> items;

    public enum Status {
        PENDING, PAID, SHIPPING, COMPLETED, CANCELLED
    }

    public String getStatusDisplay() {
        return switch (this.status) {
            case PENDING -> "Chờ xác nhận";
            case PAID -> "Đã thanh toán";
            case SHIPPING -> "Đang giao hàng";
            case COMPLETED -> "Hoàn thành";
            case CANCELLED -> "Đã hủy";
            default -> this.status.name();
        };
    }

    public BigDecimal getTotalAmount() {
        return items.stream()
                .map(OrderItem::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}