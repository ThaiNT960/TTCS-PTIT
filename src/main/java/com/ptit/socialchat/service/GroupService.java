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
    private UserService userService;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Transactional
    public Conversation createCommunityGroup(String name, String description, String category, String privacy,
            List<String> usernames, User creator) {
        Conversation conv = new Conversation();
        conv.setName(name);
        conv.setDescription(description);
        conv.setCategory(category);
        conv.setPrivacy(privacy);
        conv.setGroupChat(true);
        conv = conversationRepository.save(conv);

        // Add creator as ADMIN
        ConversationMember admin = new ConversationMember();
        admin.setConversation(conv);
        admin.setUser(creator);
        admin.setRole("ADMIN");
        admin.setJoinedAt(LocalDateTime.now());
        conversationMemberRepository.save(admin);

        // Add initial members as MEMBER
        if (usernames != null) {
            for (String username : usernames) {
                if (username.equals(creator.getUsername()))
                    continue;
                User user = userService.findByUsername(username).orElse(null);
                if (user != null) {
                    ConversationMember member = new ConversationMember();
                    member.setConversation(conv);
                    member.setUser(user);
                    member.setRole("MEMBER");
                    member.setJoinedAt(LocalDateTime.now());
                    conversationMemberRepository.save(member);
                }
            }
        }
        return conv;
    }

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
            throw new IllegalArgumentException("Chỉ Trưởng nhóm mới có quyền thay đổi vai trò");
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
            throw new IllegalArgumentException("Thành viên thường không có quyền xóa người khác");
        }

        if ("CO_ADMIN".equals(actorMember.getRole()) && !"MEMBER".equals(targetMember.getRole())) {
            throw new IllegalArgumentException("Phó nhóm chỉ có thể xóa thành viên thường");
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



