package com.hhd.jewelry.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "managers")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Manager {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Mỗi Manager gắn với 1 tài khoản người dùng (User)
    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    // Tên hiển thị (nếu muốn ghi riêng khác tên user)
    @Column(nullable = false)
    private String fullName;

    // Ghi chú hoặc mô tả vai trò (ví dụ: quản lý chung, điều hành, kiểm soát...)
    @Column(columnDefinition = "TEXT")
    private String note;

    // Trạng thái hoạt động
    @Column(nullable = false)
    private Boolean active = true;

    // Ngày tạo (auto set khi thêm mới)
    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // Liên kết đến bảng nhập xuất kho
    @OneToMany(mappedBy = "manager", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<StockMovement> stockMovements;

}
