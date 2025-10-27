package com.hhd.jewelry.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "stock_movements")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StockMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // Sản phẩm được nhập/xuất
    @ManyToOne
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    // Loại giao dịch: nhập, xuất, điều chỉnh
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ChangeType changeType;

    // Số lượng nhập hoặc xuất
    @Column(nullable = false)
    private Integer quantity;

    // Ghi chú thêm
    @Column(columnDefinition = "TEXT")
    private String note;

    // Thời gian tạo giao dịch
    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // Người thực hiện (Manager)
    @ManyToOne
    @JoinColumn(name = "manager_id", nullable = false)
    private Manager manager;

    public enum ChangeType {
        IMPORT, SALE, ADJUSTMENT
    }
}
