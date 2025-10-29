package com.hhd.jewelry.entity;

import jakarta.persistence.*; // Sử dụng jakarta.persistence cho Spring Boot 3+
import lombok.Data;
import java.time.LocalDateTime; // Thêm thời gian

@Data
@Entity // 1. Đánh dấu đây là một Entity
@Table(name = "chat_messages") // 2. Tên của bảng trong CSDL
public class ChatMessage {

    @Id // 3. Đánh dấu đây là khóa chính
    @GeneratedValue(strategy = GenerationType.IDENTITY) // 4. Tự động tăng ID
    private Long id;

    private String sender;    // "Khách hàng" hoặc "Tư vấn viên"
    private String content;   // Nội dung tin nhắn

    // Đổi tên trường này để rõ ràng hơn
    // Sẽ lưu username (nếu đã đăng nhập) hoặc sessionId (nếu là khách)
    @Column(name = "user_identifier")
    private String userIdentifier;

    // Thêm trường thời gian để sắp xếp tin nhắn
    private LocalDateTime timestamp;

    // Thêm constructor rỗng (Lombok @Data đã bao gồm)
    // Thêm Getters/Setters (Lombok @Data đã bao gồm)

    // Thêm một hàm @PrePersist để tự động đặt thời gian
    @PrePersist
    protected void onCreate() {
        this.timestamp = LocalDateTime.now();
    }

    // LƯU Ý: Đổi tên trường từ "sessionId" thành "userIdentifier"
    // Hãy cập nhật Getters/Setters nếu bạn không dùng Lombok
    public String getSessionId() {
        return userIdentifier;
    }

    public void setSessionId(String userIdentifier) {
        this.userIdentifier = userIdentifier;
    }
}