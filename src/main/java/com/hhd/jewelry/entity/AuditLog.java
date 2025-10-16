package com.hhd.jewelry.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String adminName; // Email hoặc tên người thực hiện

    @Column(nullable = false)
    private String role; // ADMIN, MANAGER, USER

    @Column(nullable = false)
    private String action; // LOGIN, LOGOUT, REGISTER, CREATE_USER, UPDATE_USER, DELETE_USER, etc.

    @Column(length = 1000)
    private String details; // Chi tiết về hành động

    @Column(nullable = false)
    private LocalDateTime time;

    private String ipAddress; // Địa chỉ IP của người thực hiện

    @Column(length = 500)
    private String userAgent; // Thông tin trình duyệt

    // Constructor tiện lợi
    public AuditLog(String adminName, String role, String action, String details) {
        this.adminName = adminName;
        this.role = role;
        this.action = action;
        this.details = details;
        this.time = LocalDateTime.now();
    }
}