package com.hhd.jewelry.controller.admin; // Đặt vào package controller admin

import com.hhd.jewelry.entity.ChatMessage;
import com.hhd.jewelry.repository.ChatMessageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin/api/chat") // Tiền tố API cho admin
public class ChatHistoryController {

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    /**
     * API này tải TẤT CẢ tin nhắn và nhóm chúng theo userIdentifier
     * Đây là cách đơn giản nhất, nhưng có thể chậm nếu có nhiều tin nhắn
     */
    @GetMapping("/history")
    public Map<String, List<ChatMessage>> getChatHistory() {

        // Tải tất cả tin nhắn từ DB
        List<ChatMessage> allMessages = chatMessageRepository.findAll();

        // Nhóm chúng lại theo userIdentifier
        // Kết quả: Map<"trandao...", [list tin nhắn]>, Map<"session-xyz", [list tin nhắn]>
        return allMessages.stream()
                .collect(Collectors.groupingBy(ChatMessage::getSessionId)); // Dùng hàm getSessionId() (chính là userIdentifier)
    }
}