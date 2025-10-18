package com.hhd.jewelry.controller;

import com.hhd.jewelry.model.ChatMessage;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

@Controller
public class ChatController {

    // client gửi tin nhắn đến /app/sendMessage
    @MessageMapping("/sendMessage")
    @SendTo("/topic/messages")
    public ChatMessage sendMessage(ChatMessage message) {
        message.setSender(message.getSender());
        message.setContent(message.getContent());
        return message;
    }
}
