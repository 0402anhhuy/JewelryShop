package com.hhd.jewelry.repository;

import com.hhd.jewelry.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    // Tìm theo người thực hiện
    List<AuditLog> findByAdminNameOrderByTimeDesc(String adminName);

    // Tìm theo hành động
    List<AuditLog> findByActionOrderByTimeDesc(String action);

    // Tìm theo vai trò
    List<AuditLog> findByRoleOrderByTimeDesc(String role);

    // Tìm theo khoảng thời gian
    List<AuditLog> findByTimeBetweenOrderByTimeDesc(LocalDateTime start, LocalDateTime end);

    // Lấy tất cả, sắp xếp theo thời gian mới nhất
    List<AuditLog> findAllByOrderByTimeDesc();

    // Tìm theo khoảng thời gian với phân trang
    Page<AuditLog> findByTimeBetween(LocalDateTime start, LocalDateTime end, Pageable pageable);
}