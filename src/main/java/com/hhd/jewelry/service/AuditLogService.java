package com.hhd.jewelry.service;

import com.hhd.jewelry.entity.AuditLog;
import com.hhd.jewelry.repository.AuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    /**
     * Ghi log audit đơn giản
     */
    @Transactional
    public void log(String adminName, String role, String action, String details) {
        AuditLog log = new AuditLog(adminName, role, action, details);
        auditLogRepository.save(log);
    }

    /**
     * Ghi log audit với thông tin request
     */
    @Transactional
    public void log(String adminName, String role, String action, String details, HttpServletRequest request) {
        AuditLog log = new AuditLog();
        log.setAdminName(adminName);
        log.setRole(role);
        log.setAction(action);
        log.setDetails(details);
        log.setTime(LocalDateTime.now());
        log.setIpAddress(getClientIp(request));
        log.setUserAgent(request.getHeader("User-Agent"));
        auditLogRepository.save(log);
    }

    /**
     * Ghi log tự động lấy thông tin user từ Security Context
     */
    @Transactional
    public void logWithCurrentUser(String action, String details) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            String username = auth.getName();
            String role = auth.getAuthorities().stream()
                    .findFirst()
                    .map(a -> a.getAuthority().replace("ROLE_", ""))
                    .orElse("UNKNOWN");
            log(username, role, action, details);
        }
    }

    /**
     * Ghi log với request và tự động lấy user
     */
    @Transactional
    public void logWithCurrentUser(String action, String details, HttpServletRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            String username = auth.getName();
            String role = auth.getAuthorities().stream()
                    .findFirst()
                    .map(a -> a.getAuthority().replace("ROLE_", ""))
                    .orElse("UNKNOWN");
            log(username, role, action, details, request);
        }
    }

    // Các phương thức truy vấn
    public List<AuditLog> getAllLogs() {
        return auditLogRepository.findAllByOrderByTimeDesc();
    }

    public List<AuditLog> getLogsByUser(String adminName) {
        return auditLogRepository.findByAdminNameOrderByTimeDesc(adminName);
    }

    public List<AuditLog> getLogsByAction(String action) {
        return auditLogRepository.findByActionOrderByTimeDesc(action);
    }

    public List<AuditLog> getLogsByRole(String role) {
        return auditLogRepository.findByRoleOrderByTimeDesc(role);
    }

    public List<AuditLog> getLogsByTimeRange(LocalDateTime start, LocalDateTime end) {
        return auditLogRepository.findByTimeBetweenOrderByTimeDesc(start, end);
    }

    // Utility: Lấy IP thực của client
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
    public Page<AuditLog> getAllLogs(Pageable pageable) {
        return auditLogRepository.findAll(pageable);
    }

    public Page<AuditLog> findByDateRange(LocalDateTime start, LocalDateTime end, Pageable pageable) {
        return auditLogRepository.findByTimeBetween(start, end, pageable);
    }
}