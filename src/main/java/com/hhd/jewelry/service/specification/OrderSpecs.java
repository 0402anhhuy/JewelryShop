package com.hhd.jewelry.service.specification; // Hoặc package tương ứng của bạn

import com.hhd.jewelry.entity.Order;
import com.hhd.jewelry.entity.User;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.time.LocalDate;

public final class OrderSpecs {

    /**
     * Lọc theo người dùng (bắt buộc).
     */
    public static Specification<Order> byUser(User user) {
        return (root, query, cb) -> cb.equal(root.get("user"), user);
    }

    /**
     * Lọc theo trạng thái đơn hàng.
     * Bỏ qua nếu status là null hoặc rỗng.
     */
    public static Specification<Order> byStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return null; // Bỏ qua điều kiện này nếu không có status
        }
        return (root, query, cb) -> {
            try {
                Order.Status orderStatus = Order.Status.valueOf(status.toUpperCase());
                return cb.equal(root.get("status"), orderStatus);
            } catch (IllegalArgumentException e) {
                // Nếu status không hợp lệ, trả về một điều kiện luôn sai
                return cb.disjunction();
            }
        };
    }

    /**
     * Lọc theo ngày bắt đầu (từ ngày).
     * Bỏ qua nếu fromDate là null.
     */
    public static Specification<Order> byFromDate(LocalDate fromDate) {
        if (fromDate == null) {
            return null;
        }
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), fromDate.atStartOfDay());
    }

    /**
     * Lọc theo ngày kết thúc (đến ngày).
     * Bỏ qua nếu toDate là null.
     */
    public static Specification<Order> byToDate(LocalDate toDate) {
        if (toDate == null) {
            return null;
        }
        // Lấy đến cuối ngày (23:59:59)
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("createdAt"), toDate.atTime(23, 59, 59));
    }
    public static Specification<Order> byKeyword(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return null; // Bỏ qua
        }
        return (root, query, cb) -> {
            String likePattern = "%" + keyword.toLowerCase() + "%";

            // Phải join với bảng User để tìm kiếm
            // "user" là tên thuộc tính (field) trong Entity Order
            var userJoin = root.join("user");

            // Tìm kiếm trên fullName hoặc phone
            return cb.or(
                    cb.like(cb.lower(userJoin.get("fullName")), likePattern),
                    cb.like(userJoin.get("phone"), likePattern) // Giả sử phone đã là chuỗi
            );
        };
    }

    /**
     * Lọc theo Tháng của ngày tạo (createdAt).
     * Bỏ qua nếu month là null.
     */
    public static Specification<Order> byMonth(Integer month) {
        if (month == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(
                cb.function("MONTH", Integer.class, root.get("createdAt")),
                month
        );
    }

    /**
     * Lọc theo Năm của ngày tạo (createdAt).
     * Bỏ qua nếu year là null.
     */
    public static Specification<Order> byYear(Integer year) {
        if (year == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(
                cb.function("YEAR", Integer.class, root.get("createdAt")),
                year
        );
    }
}