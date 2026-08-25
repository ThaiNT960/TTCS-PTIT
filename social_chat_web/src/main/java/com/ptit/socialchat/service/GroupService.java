package com.ptit.socialchat.service;

import com.ptit.socialchat.entity.*;
import com.ptit.socialchat.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class GroupService {

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private ConversationMemberRepository conversationMemberRepository;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;



    public List<Conversation> searchPublicCommunities(String category, String name) {
        return conversationRepository.searchPublicCommunities(category, name);
    }

    @Transactional
    public void changeMemberRole(Conversation conv, User targetUser, String newRole, User actor) {
        // Find target member
        ConversationMember targetMember = conversationMemberRepository.findByConversationAndUser(conv, targetUser)
                .orElseThrow(() -> new IllegalArgumentException("Người dùng không thuộc nhóm này"));

        ConversationMember actorMember = conversationMemberRepository.findByConversationAndUser(conv, actor)
                .orElseThrow(() -> new IllegalArgumentException("Bạn không thuộc nhóm này"));

        // Only ADMIN can change roles
        if (!"ADMIN".equals(actorMember.getRole())) {
            throw new org.springframework.security.access.AccessDeniedException("Chỉ trưởng nhóm mới có quyền thay đổi vai trò của thành viên.");
        }

        if ("ADMIN".equals(newRole)) {
            throw new IllegalArgumentException("Không thể thiết lập vai trò Trưởng nhóm trực tiếp");
        }

        targetMember.setRole(newRole);
        conversationMemberRepository.save(targetMember);

        // Notify via WebSocket
        sendSystemMessage(conv, actor, "đã thay đổi vai trò của " + targetUser.getFullName() + " thành " + newRole);
    }

    @Transactional
    public void removeMember(Conversation conv, User targetUser, User actor) {
        ConversationMember targetMember = conversationMemberRepository.findByConversationAndUser(conv, targetUser)
                .orElseThrow(() -> new IllegalArgumentException("Người dùng không thuộc nhóm này"));

        ConversationMember actorMember = conversationMemberRepository.findByConversationAndUser(conv, actor)
                .orElseThrow(() -> new IllegalArgumentException("Bạn không thuộc nhóm này"));

        if ("MEMBER".equals(actorMember.getRole())) {
            throw new org.springframework.security.access.AccessDeniedException("Thành viên thường không có quyền xóa người khác khỏi nhóm.");
        }

        if ("CO_ADMIN".equals(actorMember.getRole()) && !"MEMBER".equals(targetMember.getRole())) {
            throw new org.springframework.security.access.AccessDeniedException("Phó nhóm chỉ có quyền xóa thành viên thường khỏi nhóm.");
        }

        conversationMemberRepository.delete(targetMember);
        sendSystemMessage(conv, actor, "đã xóa " + targetUser.getFullName() + " khỏi nhóm");
    }

    @Transactional
    public void disbandGroup(Conversation conv, User actor) {
        // Send a final message to everyone if we wanted, but we just delete
        conversationMemberRepository.deleteAll(conversationMemberRepository.findByConversation(conv));

        com.ptit.socialchat.dto.MessageDTO dto = new com.ptit.socialchat.dto.MessageDTO();
        dto.setType("GROUP_DISBANDED");
        dto.setConversationId(conv.getId());
        messagingTemplate.convertAndSend("/topic/conversation/" + conv.getId(), dto);

        conversationRepository.delete(conv);
    }

    @Transactional
    public void sendSystemMessage(Conversation conversation, User sender, String content) {
        Message systemMsg = new Message();
        systemMsg.setConversation(conversation);
        systemMsg.setSender(sender);
        systemMsg.setContent(sender.getFullName() + " " + content);
        systemMsg.setTimestamp(LocalDateTime.now());
        messageRepository.save(systemMsg);

        com.ptit.socialchat.dto.MessageDTO dto = new com.ptit.socialchat.dto.MessageDTO();
        dto.setId(systemMsg.getId());
        dto.setContent(systemMsg.getContent());
        dto.setTimestamp(systemMsg.getTimestamp());
        dto.setSenderUsername(systemMsg.getSender().getUsername());
        dto.setConversationId(conversation.getId());

        messagingTemplate.convertAndSend("/topic/conversation/" + conversation.getId(), dto);
    }
}



