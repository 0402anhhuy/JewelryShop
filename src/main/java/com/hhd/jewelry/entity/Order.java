package com.hhd.jewelry.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "orders")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // 🔹 Khách hàng đặt hàng
    @ManyToOne
    @JoinColumn(name = "user_id")
    @ToString.Exclude @EqualsAndHashCode.Exclude
    private User user;

    // 🔹 Danh sách sản phẩm trong đơn
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude @EqualsAndHashCode.Exclude
    private List<OrderItem> items;

    // 🔹 Trạng thái đơn hàng
    @Enumerated(EnumType.STRING)
    private Status status;


    // 🔹 Phương thức thanh toán (COD, VNPay, MOMO,...)
    @Column(length = 50)
    private String methodPay;

    // 🔹 Đã thanh toán hay chưa
    private boolean paid = false;

    // 🔹 Ngày tạo đơn
    private LocalDateTime createdAt = LocalDateTime.now();

    // 🔹 Shipper được phân công
    @ManyToOne
    @JoinColumn(name = "shipper_id")
    private Shipper shipper;

    // 🔹 Phương thức vận chuyển
    @Enumerated(EnumType.STRING)
    private ShippingMethod shippingMethod = ShippingMethod.DELIVERY;

    private Long totalAmount;

    // 🔹 Trạng thái giao hàng / xử lý đơn
    public enum Status {
        PENDING,
        PAID,// Đơn mới - chờ xác nhận
        SHIPPING,       // Đang giao
        DELIVERED,      // Đã giao tới tay khách
        CANCELLED,      // Đã hủy
        RETURNED        // Trả hàng / hoàn tiền
    }

    // 🔹 Hình thức giao hàng
    public enum ShippingMethod {
        DELIVERY,       // Giao tận nơi
        STORE_PICKUP    // Nhận tại cửa hàng
    }
}
