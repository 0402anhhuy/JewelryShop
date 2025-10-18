package com.hhd.jewelry.controller.client;

import com.hhd.jewelry.entity.ChatMessage;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
@Controller
public class ChatController {

    @MessageMapping("/sendMessage")
    @SendTo("/topic/messages")
    public ChatMessage handleMessage(ChatMessage message) {
        String reply = getAutoReply(message.getContent());
        ChatMessage response = new ChatMessage();
        response.setSender("Tư vấn viên");
        response.setContent(reply);
        return response;
    }

    // Trả lời tự động cơ bản
    private String getAutoReply(String question) {
        question = question.toLowerCase();
        if (question.contains("khuyến mãi"))
            return "Hiện PNJ đang có nhiều chương trình ưu đãi, bạn có thể xem tại mục 'Sản phẩm khuyến mãi' 💎";
        if (question.contains("đổi trả"))
            return "Chính sách đổi trả: trong vòng 48h kể từ khi nhận hàng, còn nguyên vẹn hóa đơn và sản phẩm.";
        if (question.contains("phí giao hàng"))
            return "PNJ miễn phí giao hàng toàn quốc cho đơn hàng trên 500.000đ 🚚";
        if (question.contains("size"))
            return "Bạn có thể đo chu vi cổ tay và cộng thêm 1–2cm để chọn size phù hợp nhé 💫";
        if (question.contains("zalo"))
            return "Bạn có thể chat trực tiếp với tư vấn viên qua Zalo: https://zalo.me/0123456789 📱";
        return "Cảm ơn bạn! Bộ phận tư vấn sẽ liên hệ hỗ trợ thêm 💬";
    }
}