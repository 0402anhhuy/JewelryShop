package com.hhd.jewelry.repository;

import com.hhd.jewelry.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    // Tự động tìm tất cả tin nhắn của một user, sắp xếp theo thời gian
    List<ChatMessage> findByUserIdentifierOrderByTimestampAsc(String userIdentifier);

    // Tìm các cuộc trò chuyện gần đây (ví dụ, trong 3 ngày)
    // @Query("SELECT DISTINCT c.userIdentifier FROM ChatMessage c WHERE c.timestamp > :since")
    // List<String> findDistinctUserIdentifiersSince(LocalDateTime since);
}