package com.hhd.jewelry.controller.manager; // Bạn có thể tạo package mới

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
@RequestMapping("/manager/api/chat") // <-- ĐÃ THAY ĐỔI
public class ManagerChatHistoryController { // <-- ĐÃ THAY ĐỔI

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    /**
     * API này tải TẤT CẢ tin nhắn và nhóm chúng theo userIdentifier
     * Logic giữ nguyên, chỉ đổi endpoint
     */
    @GetMapping("/history")
    public Map<String, List<ChatMessage>> getChatHistory() {

        List<ChatMessage> allMessages = chatMessageRepository.findAll();

        return allMessages.stream()
                .collect(Collectors.groupingBy(ChatMessage::getSessionId));
    }
}