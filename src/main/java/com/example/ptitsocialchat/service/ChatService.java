package com.example.ptitsocialchat.service;

import com.example.ptitsocialchat.dto.MessageDTO;
import com.example.ptitsocialchat.entity.Conversation;
import com.example.ptitsocialchat.entity.ConversationMember;
import com.example.ptitsocialchat.entity.Message;
import com.example.ptitsocialchat.entity.User;
import com.example.ptitsocialchat.repository.ConversationMemberRepository;
import com.example.ptitsocialchat.repository.ConversationRepository;
import com.example.ptitsocialchat.repository.MessageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ChatService {
    @Autowired
    private MessageRepository messageRepository;
    @Autowired
    private ConversationRepository conversationRepository;
    @Autowired
    private ConversationMemberRepository conversationMemberRepository;
    @Autowired
    private UserService userService;

    @Transactional
    public Conversation getOrCreateConversation(User user1, User user2) {
        List<ConversationMember> members1 = conversationMemberRepository.findByUser(user1);
        for (ConversationMember cm1 : members1) {
            Conversation conv = cm1.getConversation();
            if (!conv.isGroupChat()) {
                List<ConversationMember> members = conversationMemberRepository.findByConversation(conv);
                if (members.size() == 2) {
                    boolean hasUser2 = members.stream().anyMatch(m -> m.getUser().getId().equals(user2.getId()));
                    if (hasUser2) return conv;
                }
            }
        }
        Conversation newConv = new Conversation();
        newConv.setGroupChat(false);
        newConv = conversationRepository.save(newConv);

        ConversationMember m1 = new ConversationMember();
        m1.setConversation(newConv);
        m1.setUser(user1);
        m1.setJoinedAt(LocalDateTime.now());
        conversationMemberRepository.save(m1);

        ConversationMember m2 = new ConversationMember();
        m2.setConversation(newConv);
        m2.setUser(user2);
        m2.setJoinedAt(LocalDateTime.now());
        conversationMemberRepository.save(m2);

        return newConv;
    }

    @Transactional
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

    public List<Map<String, Object>> getUserConversations(User user) {
        List<ConversationMember> memberships = conversationMemberRepository.findByUser(user);
        return memberships.stream().map(m -> {
            Conversation conv = m.getConversation();
            Map<String, Object> map = new HashMap<>();
            map.put("id", conv.getId());
            map.put("isGroupChat", conv.isGroupChat());
            
            if (conv.isGroupChat()) {
                map.put("name", conv.getName());
                map.put("avatar", null);
            } else {
                List<ConversationMember> allMembers = conversationMemberRepository.findByConversation(conv);
                User other = allMembers.stream()
                        .filter(mem -> !mem.getUser().getId().equals(user.getId()))
                        .map(ConversationMember::getUser)
                        .findFirst().orElse(user);
                map.put("name", other.getFullName());
                map.put("otherUsername", other.getUsername());
                map.put("avatar", other.getAvatar());
            }
            
            List<Message> msgs = messageRepository.findByConversationOrderByTimestampAsc(conv);
            if (!msgs.isEmpty()) {
                Message last = msgs.get(msgs.size() - 1);
                map.put("lastMessage", last.getContent());
                map.put("lastTimestamp", last.getTimestamp());
            }
            return map;
        }).collect(Collectors.toList());
    }

    public Message saveMessage(User sender, Conversation conversation, String content, String imageUrl) {
        Message message = new Message();
        message.setSender(sender);
        message.setConversation(conversation);
        message.setContent(content);
        message.setImageUrl(imageUrl);
        message.setTimestamp(LocalDateTime.now());
        return messageRepository.save(message);
    }

    public List<MessageDTO> getChatHistory(Conversation conversation) {
        return messageRepository.findByConversationOrderByTimestampAsc(conversation).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public Optional<Conversation> getConversation(Long id) {
        return conversationRepository.findById(id);
    }

    private MessageDTO convertToDTO(Message message) {
        MessageDTO dto = new MessageDTO();
        dto.setId(message.getId());
        dto.setContent(message.getContent());
        dto.setImageUrl(message.getImageUrl());
        dto.setTimestamp(message.getTimestamp());
        dto.setSenderUsername(message.getSender().getUsername());
        dto.setConversationId(message.getConversation().getId());
        return dto;
    }
}
