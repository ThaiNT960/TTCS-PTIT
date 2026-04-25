package com.example.ptitsocialchat.websocket;

import com.example.ptitsocialchat.dto.MessageDTO;
import com.example.ptitsocialchat.entity.Conversation;
import com.example.ptitsocialchat.entity.ConversationMember;
import com.example.ptitsocialchat.entity.Message;
import com.example.ptitsocialchat.entity.User;
import com.example.ptitsocialchat.repository.ConversationMemberRepository;
import com.example.ptitsocialchat.service.ChatService;
import com.example.ptitsocialchat.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.List;

@Controller
public class ChatSocketController {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private ChatService chatService;

    @Autowired
    private UserService userService;

    @Autowired
    private ConversationMemberRepository conversationMemberRepository;

    @MessageMapping("/chat")
    public void processMessage(@Payload MessageDTO messageDTO) {
        User sender = userService.findByUsername(messageDTO.getSenderUsername()).orElseThrow();
        
        Conversation conversation;
        if (messageDTO.getConversationId() != null) {
            conversation = chatService.getConversation(messageDTO.getConversationId())
                    .orElseThrow(() -> new RuntimeException("Conversation not found"));
        } else if (messageDTO.getReceiverUsername() != null) {
            User receiver = userService.findByUsername(messageDTO.getReceiverUsername()).orElseThrow();
            conversation = chatService.getOrCreateConversation(sender, receiver);
        } else {
            throw new RuntimeException("Conversation ID or Receiver Username must be provided");
        }

        Message savedMsg = chatService.saveMessage(sender, conversation, messageDTO.getContent(), messageDTO.getImageUrl());

        // Cập nhật DTO với thông tin từ database
        messageDTO.setId(savedMsg.getId());
        messageDTO.setTimestamp(savedMsg.getTimestamp());
        messageDTO.setConversationId(conversation.getId());

        // Lấy tất cả thành viên của cuộc hội thoại
        List<ConversationMember> members = conversationMemberRepository.findByConversation(conversation);
        
        // Gửi tin nhắn đến topic cá nhân của TẤT CẢ thành viên (để cập nhật UI cho cả người gửi và người nhận)
        for (ConversationMember member : members) {
            messagingTemplate.convertAndSend("/topic/messages/" + member.getUser().getUsername(), messageDTO);
        }
    }
}
