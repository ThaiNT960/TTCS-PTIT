package com.ptit.socialchat.controller;

import com.ptit.socialchat.dto.MessageDTO;
import com.ptit.socialchat.entity.User;
import com.ptit.socialchat.service.ChatService;
import com.ptit.socialchat.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.ptit.socialchat.repository.ConversationMemberRepository;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/chat")
public class ChatController {
    @Autowired
    private ConversationMemberRepository conversationMemberRepository;
    @Autowired
    private com.ptit.socialchat.repository.FriendRepository friendRepository;
    @Autowired
    private ChatService chatService;
    @Autowired
    private UserService userService;
    @Autowired
    private com.ptit.socialchat.repository.MessageRepository messageRepository;

    @GetMapping("/contacts")
    public List<Map<String, Object>> getContacts(Principal principal) {
        if (principal == null) {
            return List.of();
        }
        User user = userService.findByUsername(principal.getName()).orElseThrow();
        List<User> friends = friendRepository.findByUser(user).stream()
                .map(com.ptit.socialchat.entity.Friend::getFriend)
                .filter(f -> !f.isLocked())
                .collect(java.util.stream.Collectors.toList());
        List<User> chattedReceivers = messageRepository.findReceiversByUser(user).stream()
                .filter(f -> !f.isLocked())
                .collect(java.util.stream.Collectors.toList());
        List<User> chattedSenders = messageRepository.findSendersByUser(user).stream()
                .filter(f -> !f.isLocked())
                .collect(java.util.stream.Collectors.toList());
        
        java.util.Set<User> contacts = new java.util.LinkedHashSet<>(friends);
        contacts.addAll(chattedReceivers);
        contacts.addAll(chattedSenders);
        
        List<Map<String, Object>> result = new java.util.ArrayList<>();
        for (User contact : contacts) {
            Map<String, Object> map = new java.util.HashMap<>();
            map.put("username", contact.getUsername());
            map.put("fullName", contact.getFullName());
            map.put("avatar", contact.getAvatar());
            map.put("isOnline", com.ptit.socialchat.config.WebSocketEventListener.isUserOnline(contact.getUsername()));

            List<com.ptit.socialchat.entity.Message> lastMsgs = messageRepository.findLatestMessageBetween(
                user, contact, org.springframework.data.domain.PageRequest.of(0, 1)
            );
            if (!lastMsgs.isEmpty()) {
                com.ptit.socialchat.entity.Message last = lastMsgs.get(0);
                if (Boolean.TRUE.equals(last.getIsRevoked())) {
                    map.put("lastMessage", "[Tin nhắn đã bị thu hồi]");
                } else if (last.getFileUrl() != null && !last.getFileUrl().trim().isEmpty() && (last.getContent() == null || last.getContent().trim().isEmpty())) {
                    String url = last.getFileUrl().toLowerCase();
                    if (url.endsWith(".jpg") || url.endsWith(".jpeg") || url.endsWith(".png") || url.endsWith(".gif") || url.endsWith(".webp")) {
                        map.put("lastMessage", "[Hình ảnh]");
                    } else {
                        map.put("lastMessage", "[Tài liệu]");
                    }
                } else {
                    map.put("lastMessage", last.getContent());
                }
                map.put("lastTimestamp", last.getTimestamp());
            } else {
                map.put("lastMessage", "@" + contact.getUsername());
                map.put("lastTimestamp", java.time.LocalDateTime.of(1970, 1, 1, 0, 0));
            }
            result.add(map);
        }
        return result;
    }

    @GetMapping("/history")
    public ResponseEntity<?> getChatHistory(Principal principal, @RequestParam String user2) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }
        User u1 = userService.findByUsername(principal.getName()).orElseThrow();
        User u2 = userService.findByUsername(user2).orElseThrow();
        List<MessageDTO> history = chatService.getChatHistory(u1, u2);
        boolean isFriend = friendRepository.findByUserAndFriend(u1, u2).isPresent();
        return ResponseEntity.ok(Map.of(
            "isFriend", isFriend,
            "messages", history
        ));
    }



    @PutMapping("/revoke/{messageId}")
    public ResponseEntity<?> revokeMessage(@PathVariable Long messageId, Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }
        User user = userService.findByUsername(principal.getName()).orElseThrow();
        try {
            chatService.revokeMessage(messageId, user);
            return ResponseEntity.ok(Map.of("status", "ok"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/history/{otherUsername}")
    public ResponseEntity<?> clearHistory(@PathVariable String otherUsername, Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }
        User user = userService.findByUsername(principal.getName()).orElseThrow();
        User other = userService.findByUsername(otherUsername).orElseThrow();
        chatService.clearChatHistory(user, other);
        return ResponseEntity.ok(Map.of("status", "ok"));
    }

    @PostMapping("/group")
    public ResponseEntity<?> createGroup(@RequestBody Map<String, Object> request, Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }
        String name = (String) request.get("name");
        List<String> usernames = (List<String>) request.get("usernames");
        if (usernames == null || usernames.size() < 3) {
            return ResponseEntity.badRequest().body(Map.of("error", "Nhóm chat phải có từ 3 người trở lên."));
        }
        String creatorUsername = principal.getName();
        String privacy = (String) request.get("privacy");
        String category = (String) request.get("category");
        String description = (String) request.get("description");
        com.ptit.socialchat.entity.Conversation conv = chatService.createGroupConversation(
                name, usernames, creatorUsername, privacy, category, description
        );
        return ResponseEntity.ok(conv);
    }

    @GetMapping("/groups")
    public List<Map<String, Object>> getGroups(Principal principal) {
        if (principal == null) {
            return List.of();
        }
        User user = userService.findByUsername(principal.getName()).orElseThrow();
        return chatService.getUserGroupConversations(user);
    }

    @GetMapping("/group-history")
    public ResponseEntity<?> getGroupHistory(Principal principal, @RequestParam Long conversationId) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }
        User user = userService.findByUsername(principal.getName()).orElseThrow();
        com.ptit.socialchat.entity.Conversation conv = chatService.getConversation(conversationId).orElseThrow();
        List<MessageDTO> history = chatService.getGroupChatHistory(conv, user);
        return ResponseEntity.ok(Map.of(
            "isGroup", true,
            "messages", history
        ));
    }

    @DeleteMapping("/group-history/{conversationId}")
    public ResponseEntity<?> clearGroupHistory(@PathVariable Long conversationId, Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }
        User user = userService.findByUsername(principal.getName()).orElseThrow();
        com.ptit.socialchat.entity.Conversation conv = chatService.getConversation(conversationId).orElseThrow();
        chatService.clearGroupChatHistory(user, conv);
        return ResponseEntity.ok(Map.of("status", "ok"));
    }

    @PostMapping("/group/{conversationId}/leave")
    public ResponseEntity<?> leaveGroup(@PathVariable Long conversationId, Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }
        User user = userService.findByUsername(principal.getName()).orElseThrow();
        com.ptit.socialchat.entity.Conversation conv = chatService.getConversation(conversationId).orElseThrow();
        chatService.leaveGroupConversation(user, conv);
        return ResponseEntity.ok(Map.of("status", "ok"));
    }

    @GetMapping("/group/{conversationId}/non-members")
    public List<User> getGroupNonMembers(@PathVariable Long conversationId, Principal principal) {
        if (principal == null) {
            return List.of();
        }
        User user = userService.findByUsername(principal.getName()).orElseThrow();
        com.ptit.socialchat.entity.Conversation conv = chatService.getConversation(conversationId).orElseThrow();
        return chatService.getGroupNonMembers(conv, user);
    }

    @PostMapping("/group/{conversationId}/add-members")
    public ResponseEntity<?> addMembersToGroup(@PathVariable Long conversationId, @RequestBody Map<String, Object> request, Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }
        User currentUser = userService.findByUsername(principal.getName()).orElseThrow();
        List<String> usernames = (List<String>) request.get("usernames");
        com.ptit.socialchat.entity.Conversation conv = chatService.getConversation(conversationId).orElseThrow();
        chatService.addMembersToGroupConversation(conv, usernames, currentUser);
        return ResponseEntity.ok(Map.of("status", "ok"));
    }

    @GetMapping("/group/{conversationId}/members")
    public ResponseEntity<?> getGroupMembers(@PathVariable Long conversationId, Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }
        User user = userService.findByUsername(principal.getName()).orElseThrow();
        com.ptit.socialchat.entity.Conversation conv = chatService.getConversation(conversationId)
                .orElseThrow(() -> new IllegalArgumentException("Nhóm chat không tồn tại"));
        
        if (conversationMemberRepository.findByConversationAndUser(conv, user).isEmpty()) {
            return ResponseEntity.status(403).body(Map.of("error", "Bạn không phải là thành viên của nhóm này."));
        }
        
        List<com.ptit.socialchat.entity.ConversationMember> members = conversationMemberRepository.findByConversation(conv);
        List<Map<String, Object>> result = members.stream().map(m -> {
            Map<String, Object> map = new java.util.HashMap<>();
            map.put("username", m.getUser().getUsername());
            map.put("fullName", m.getUser().getFullName());
            map.put("avatar", m.getUser().getAvatar());
            map.put("role", m.getRole());
            map.put("joinedAt", m.getJoinedAt());
            return map;
        }).collect(Collectors.toList());
        
        return ResponseEntity.ok(result);
    }
}



