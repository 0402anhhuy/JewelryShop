package com.hhd.jewelry.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
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
    @JsonBackReference
    private User user;

    // 🔹 Danh sách sản phẩm trong đơn
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude @EqualsAndHashCode.Exclude
    @JsonManagedReference
    private List<OrderItem> items;

    // 🔹 Trạng thái đơn hàng
    @Enumerated(EnumType.STRING)
    private Status status;
    // 🔹 DANH SÁCH LỊCH SỬ TRẠNG THÁI
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonManagedReference // Cho phép trường này được chuyển sang JSON
    @OrderBy("timestamp ASC") // LUÔN LUÔN sắp xếp theo thời gian tăng dần
    @ToString.Exclude @EqualsAndHashCode.Exclude
    private List<OrderStatusHistory> statusHistory = new ArrayList<>();

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
    @JsonBackReference
    private Shipper shipper;

    // 🔹 Phương thức vận chuyển
    @Enumerated(EnumType.STRING)
    private ShippingMethod shippingMethod = ShippingMethod.DELIVERY;

    private Long totalAmount;

    @Column(columnDefinition = "TEXT")
    private String note;

    @Column(nullable = false)
    private String consignee; // Tên người nhận hàng

    @Column(nullable = false)
    private String shippingPhone; // Số điện thoại người nhận

    @Column(nullable = false)
    private String shippingAddressLine; // Địa chỉ chi tiết (số nhà, tên đường)
    @Column(name = "is_reviewed")
    private Boolean isReviewed = false;
    private String shippingWard; // Phường/Xã

    private String shippingDistrict; // Quận/Huyện
    @Column(nullable = false)
    private String shippingCity; // Tỉnh/Thành phố
    private String couponCode;
    private String shippingName;
    private Long discount;
    private Long shippingFee;
    private Long tax;
    // 🔹 Trạng thái giao hàng / xử lý đơn
    public enum Status {
        PENDING,
        PAID,// Đơn mới - chờ xác nhận
        SHIPPING,       // Đang giao
        DELIVERED,      // Đã giao tới tay khách
        COMPLETED,       // Đơn hàng hoàn thành
        CANCELLED,      // Đã hủy
        RETURNED        // Trả hàng / hoàn tiền
    }

    // 🔹 Hình thức giao hàng
    public enum ShippingMethod {
        DELIVERY,       // Giao tận nơi
        STORE_PICKUP    // Nhận tại cửa hàng
    }

    public String displayStatus(){
        String strStatus = "";
        switch (status){
            case PENDING: strStatus = "Chờ xác nhận"; break;
            case PAID: strStatus = "Đang xử lý";  break;
            case SHIPPING: strStatus = "Đang giao";  break;
            case DELIVERED: strStatus = "Đã giao hàng";  break;
            case COMPLETED: strStatus = "Hoàn thành"; break;
            case CANCELLED: strStatus = "Đã hủy";  break;
            case RETURNED: strStatus = "Trả hàng";   break;
        }
        return strStatus;
    }
}