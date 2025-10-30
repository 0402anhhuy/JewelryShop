package com.hhd.jewelry.controller.client;

import com.hhd.jewelry.entity.ChatMessage;
import com.hhd.jewelry.repository.ChatMessageRepository; // 1. Import Repository
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
public class ChatController {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired // 2. Tiêm Repository
    private ChatMessageRepository chatMessageRepository;

    private static final String DEFAULT_WAIT_MESSAGE = "Cảm ơn bạn! Bộ phận tư vấn sẽ liên hệ hỗ trợ thêm 💬";

    @MessageMapping("/chat.sendMessage")
    public void handleMessageFromUser(@Payload ChatMessage message,
                                      SimpMessageHeaderAccessor headerAccessor,
                                      Principal principal) {

        String userIdentifier;
        if (principal != null) {
            userIdentifier = principal.getName();
        } else {
            userIdentifier = headerAccessor.getSessionId();
        }

        message.setSessionId(userIdentifier); // Dùng tên hàm cũ, nhưng giá trị là userIdentifier
        message.setSender("Khách hàng"); // Đảm bảo sender là khách hàng

        // 3. LƯU TIN NHẮN CỦA USER VÀO DB
        chatMessageRepository.save(message);

        String botReplyContent = getAutoReply(message.getContent());

        ChatMessage botResponse = new ChatMessage();
        botResponse.setSender("Tư vấn viên");
        botResponse.setSessionId(userIdentifier);

        if (botReplyContent != null) {
            botResponse.setContent(botReplyContent);

            // 4. LƯU TIN NHẮN CỦA BOT VÀO DB
            chatMessageRepository.save(botResponse);

            messagingTemplate.convertAndSendToUser(userIdentifier, "/queue/replies", botResponse);

        } else {
            // Chuyển cho Admin
            messagingTemplate.convertAndSend("/topic/admin/inbox", message); // Gửi tin của User

            botResponse.setContent(DEFAULT_WAIT_MESSAGE);

            // 5. LƯU TIN NHẮN CHỜ VÀO DB
            chatMessageRepository.save(botResponse);

            messagingTemplate.convertAndSendToUser(userIdentifier, "/queue/replies", botResponse);
        }
    }

    @MessageMapping("/chat.replyToUser")
    public void handleReplyFromAdmin(@Payload ChatMessage message) {

        String userIdentifier = message.getSessionId();
        message.setSender("Tư vấn viên"); // Đảm bảo sender là admin

        if (userIdentifier != null && !userIdentifier.isEmpty()) {

            // 6. LƯU TIN NHẮN CỦA ADMIN VÀO DB
            chatMessageRepository.save(message);

            messagingTemplate.convertAndSendToUser(userIdentifier, "/queue/replies", message);
        }
    }

    // --- Logic Bot (Giữ nguyên) ---
    private String getAutoReply(String question) {
        // ... (giữ nguyên logic bot của bạn)
        question = question.toLowerCase();
        if (question.contains("khuyến mãi")) return "Hiện PNJ đang có nhiều chương trình ưu đãi, bạn có thể xem tại mục 'Sản phẩm khuyến mãi' 💎";
        if (question.contains("đổi trả")) return "Chính sách đổi trả: trong vòng 48h kể từ khi nhận hàng, còn nguyên vẹn hóa đơn và sản phẩm.";
        if (question.contains("phí giao hàng")) return "PNJ miễn phí giao hàng toàn quốc cho đơn hàng trên 500.000đ 🚚";
        if (question.contains("size")) return "Bạn có thể đo chu vi cổ tay và cộng thêm 1–2cm để chọn size phù hợp nhé 💫";
        if (question.contains("zalo")) return "Bạn có thể chat trực tiếp với tư vấn viên qua Zalo: https://zalo.me/0123456789 📱";
        return null;
    }
}