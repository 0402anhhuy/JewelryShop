package com.hhd.jewelry.controller.client; // Hoặc package controller của bạn

import com.hhd.jewelry.entity.ChatMessage;
import com.hhd.jewelry.repository.ChatMessageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/chat") // Tiền tố API chung, không có /admin
public class UserChatHistoryController {

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    /**
     * API này trả về lịch sử chat CHỈ CHO NGƯỜI DÙNG ĐÃ ĐĂNG NHẬP
     *
     * @param principal Đối tượng bảo mật chứa tên người dùng
     * @return Danh sách tin nhắn
     */
    @GetMapping("/my-history")
    public ResponseEntity<List<ChatMessage>> getMyChatHistory(Principal principal) {

        // Nếu người dùng chưa đăng nhập (là khách)
        if (principal == null) {
            // Trả về một danh sách rỗng
            return ResponseEntity.ok(Collections.emptyList());
        }

        // Nếu đã đăng nhập, tìm lịch sử theo tên (username)
        String username = principal.getName();
        List<ChatMessage> history = chatMessageRepository.findByUserIdentifierOrderByTimestampAsc(username);

        return ResponseEntity.ok(history);
    }
}