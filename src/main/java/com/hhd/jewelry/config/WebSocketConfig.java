package com.hhd.jewelry.config; // Giữ nguyên package của bạn

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.*;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {

        // SỬA ĐỔI DÒNG NÀY:
        // /topic: Dùng cho kênh chung (như /topic/admin/inbox)
        // /queue: Dùng cho các hàng đợi tin nhắn (cần thiết cho /user)
        config.enableSimpleBroker("/topic", "/queue");

        config.setApplicationDestinationPrefixes("/app");

        // THÊM DÒNG NÀY:
        // Định nghĩa tiền tố cho kênh riêng của User (ví dụ: /user/queue/replies)
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws-chat")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }
}