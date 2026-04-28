package com.example.ptitsocialchat.service;

import com.example.ptitsocialchat.dto.MessageDTO;
import com.example.ptitsocialchat.entity.Message;
import com.example.ptitsocialchat.entity.User;
import com.example.ptitsocialchat.repository.MessageRepository;
import com.example.ptitsocialchat.entity.Conversation;
import com.example.ptitsocialchat.entity.ConversationMember;
import com.example.ptitsocialchat.repository.ConversationMemberRepository;
import com.example.ptitsocialchat.repository.ConversationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ChatService {
    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private ConversationMemberRepository conversationMemberRepository;

    @Autowired
    private UserService userService;

    @org.springframework.transaction.annotation.Transactional
    public Conversation createGroupConversation(String name, List<String> usernames) {
        Conversation conv = new Conversation();
        conv.setName(name);
        conv.setGroupChat(true);
        conv = conversationRepository.save(conv);

        for (String username : usernames) {
            User user = userService.findByUsername(username).orElseThrow();
            ConversationMember member = new ConversationMember();
            member.setConversation(conv);
            member.setUser(user);
            member.setJoinedAt(LocalDateTime.now());
            conversationMemberRepository.save(member);
        }
        return conv;
    }

    public List<Map<String, Object>> getUserGroupConversations(User user) {
        List<ConversationMember> memberships = conversationMemberRepository.findByUser(user);
        return memberships.stream()
                .filter(m -> m.getConversation().isGroupChat())
                .map(m -> {
                    Conversation conv = m.getConversation();
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", conv.getId());
                    map.put("isGroupChat", true);
                    map.put("name", conv.getName());
                    map.put("avatar", null);
                    
                    List<Message> msgs = messageRepository.findByConversationOrderByTimestampAsc(conv);
                    if (!msgs.isEmpty()) {
                        Message last = msgs.get(msgs.size() - 1);
                        map.put("lastMessage", last.getContent());
                        map.put("lastTimestamp", last.getTimestamp());
                    }
                    return map;
                }).collect(Collectors.toList());
    }

    public Message saveGroupMessage(User sender, Conversation conversation, String content, String imageUrl) {
        Message message = new Message();
        message.setSender(sender);
        message.setConversation(conversation);
        message.setContent(content);
        message.setImageUrl(imageUrl);
        message.setTimestamp(LocalDateTime.now());
        return messageRepository.save(message);
    }

    public List<MessageDTO> getGroupChatHistory(Conversation conversation) {
        return messageRepository.findByConversationOrderByTimestampAsc(conversation).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public java.util.Optional<Conversation> getConversation(Long id) {
        return conversationRepository.findById(id);
    }

    public Message saveMessage(User sender, User receiver, String content, String imageUrl) {
        Message message = new Message();
        message.setSender(sender);
        message.setReceiver(receiver);
        message.setContent(content);
        message.setImageUrl(imageUrl);
        message.setTimestamp(LocalDateTime.now());
        return messageRepository.save(message);
    }

    public List<MessageDTO> getChatHistory(User user1, User user2) {
        return messageRepository.findChatHistory(user1, user2).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    private MessageDTO convertToDTO(Message message) {
        MessageDTO dto = new MessageDTO();
        dto.setId(message.getId());
        dto.setContent(message.getContent());
        dto.setTimestamp(message.getTimestamp());
        dto.setSenderUsername(message.getSender().getUsername());
        dto.setReceiverUsername(message.getReceiver() != null ? message.getReceiver().getUsername() : null);
        if (message.getConversation() != null) {
            dto.setConversationId(message.getConversation().getId());
        }
        dto.setImageUrl(message.getImageUrl());
        dto.setIsRevoked(message.getIsRevoked());
        return dto;
    }

    public void clearChatHistory(User currentUser, User otherUser) {
        messageRepository.clearChatHistoryAsSender(currentUser, otherUser);
        messageRepository.clearChatHistoryAsReceiver(currentUser, otherUser);
    }

    public void revokeMessage(Long messageId, User currentUser) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new IllegalArgumentException("Tin nhắn không tồn tại"));
        
        if (!message.getSender().getId().equals(currentUser.getId())) {
            throw new IllegalArgumentException("Chỉ người gửi mới có quyền thu hồi tin nhắn");
        }
        
        message.setIsRevoked(true);
        messageRepository.save(message);

        // Notify receiver via WebSocket
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "MESSAGE_RECALLED");
        payload.put("partnerUsername", currentUser.getUsername());
        payload.put("messageId", messageId);
        
        messagingTemplate.convertAndSend(
                "/topic/messages/" + message.getReceiver().getUsername(), 
                payload
        );
        
        // Notify sender as well so their other tabs update
        Map<String, Object> senderPayload = new HashMap<>();
        senderPayload.put("type", "MESSAGE_RECALLED");
        senderPayload.put("partnerUsername", message.getReceiver().getUsername());
        senderPayload.put("messageId", messageId);
        messagingTemplate.convertAndSend(
                "/topic/messages/" + currentUser.getUsername(), 
                senderPayload
        );
    }
}
