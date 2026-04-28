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

    @Autowired
    private com.example.ptitsocialchat.repository.FriendRepository friendRepository;

    @MessageMapping("/chat")
    public void processMessage(@Payload MessageDTO messageDTO) {
        User sender = userService.findByUsername(messageDTO.getSenderUsername()).orElseThrow();
        User receiver = userService.findByUsername(messageDTO.getReceiverUsername()).orElseThrow();

        if (friendRepository.findByUserAndFriend(sender, receiver).isEmpty()) {
            return; // Ignore message if not friends
        }

        com.example.ptitsocialchat.entity.Message savedMsg = chatService.saveMessage(sender, receiver, messageDTO.getContent(), messageDTO.getImageUrl());
        
        // Gán ID từ database vào DTO để gửi lại cho người dùng
        messageDTO.setId(savedMsg.getId());
        messageDTO.setTimestamp(java.time.LocalDateTime.now());

        // Gửi đến người nhận
        messagingTemplate.convertAndSend(
                "/topic/messages/" + messageDTO.getReceiverUsername(),
                messageDTO);
        
        // Gửi ngược lại cho người gửi để đồng bộ ID
        messagingTemplate.convertAndSend(
                "/topic/messages/" + messageDTO.getSenderUsername(),
                messageDTO);
    }

    @MessageMapping("/chat/group")
    public void processGroupMessage(@Payload MessageDTO messageDTO) {
        User sender = userService.findByUsername(messageDTO.getSenderUsername()).orElseThrow();
        com.example.ptitsocialchat.entity.Conversation conv = chatService.getConversation(messageDTO.getConversationId()).orElseThrow();

        com.example.ptitsocialchat.entity.Message savedMsg = chatService.saveGroupMessage(sender, conv, messageDTO.getContent(), messageDTO.getImageUrl());
        
        // Đồng bộ ID và timestamp từ database
        messageDTO.setId(savedMsg.getId());
        messageDTO.setTimestamp(java.time.LocalDateTime.now());

        messagingTemplate.convertAndSend(
                "/topic/conversation/" + messageDTO.getConversationId(),
                messageDTO);
    }
}
