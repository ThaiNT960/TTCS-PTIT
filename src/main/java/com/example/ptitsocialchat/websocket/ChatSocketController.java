package com.example.ptitsocialchat.websocket;

import com.example.ptitsocialchat.dto.MessageDTO;
import com.example.ptitsocialchat.entity.User;
import com.example.ptitsocialchat.service.ChatService;
import com.example.ptitsocialchat.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;

@Controller
public class ChatSocketController {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private ChatService chatService;

    @Autowired
    private UserService userService;

    @MessageMapping("/chat")
    public void processMessage(@Payload MessageDTO messageDTO) {
        User sender = userService.findByUsername(messageDTO.getSenderUsername()).orElseThrow();
        User receiver = userService.findByUsername(messageDTO.getReceiverUsername()).orElseThrow();

        chatService.saveMessage(sender, receiver, messageDTO.getContent());

        // Gán timestamp từ server
        messageDTO.setTimestamp(LocalDateTime.now());

        // Gửi đến người nhận qua topic (không cần Principal/authentication)
        messagingTemplate.convertAndSend(
                "/topic/messages/" + messageDTO.getReceiverUsername(),
                messageDTO);
    }
}
