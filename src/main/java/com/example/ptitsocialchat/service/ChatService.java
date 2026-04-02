package com.example.ptitsocialchat.service;

import com.example.ptitsocialchat.dto.MessageDTO;
import com.example.ptitsocialchat.entity.Message;
import com.example.ptitsocialchat.entity.User;
import com.example.ptitsocialchat.repository.MessageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ChatService {
    @Autowired
    private MessageRepository messageRepository;

    public Message saveMessage(User sender, User receiver, String content) {
        Message message = new Message();
        message.setSender(sender);
        message.setReceiver(receiver);
        message.setContent(content);
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
        dto.setReceiverUsername(message.getReceiver().getUsername());
        return dto;
    }
}
