package com.example.ptitsocialchat.controller;

import com.example.ptitsocialchat.dto.MessageDTO;
import com.example.ptitsocialchat.entity.User;
import com.example.ptitsocialchat.service.ChatService;
import com.example.ptitsocialchat.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.security.Principal;

@RestController
@RequestMapping("/api/chat")
public class ChatController {
    @Autowired
    private com.example.ptitsocialchat.repository.FriendRepository friendRepository;
    @Autowired
    private ChatService chatService;
    @Autowired
    private UserService userService;
    @Autowired
    private com.example.ptitsocialchat.repository.MessageRepository messageRepository;
    @Autowired
    private com.example.ptitsocialchat.repository.ConversationMemberRepository conversationMemberRepository;

    @GetMapping("/contacts")
    public List<Map<String, Object>> getContacts(Principal principal) {
        if (principal == null) return List.of();
        User user = userService.findByUsername(principal.getName()).orElseThrow();
        List<User> friends = friendRepository.findByUser(user).stream()
                .map(com.example.ptitsocialchat.entity.Friend::getFriend).collect(java.util.stream.Collectors.toList());
        List<User> chattedReceivers = messageRepository.findReceiversByUser(user);
        List<User> chattedSenders = messageRepository.findSendersByUser(user);
        
        java.util.Set<User> contacts = new java.util.LinkedHashSet<>(friends);
        contacts.addAll(chattedReceivers);
        contacts.addAll(chattedSenders);
        
        return contacts.stream().map(u -> {
            Map<String, Object> map = new java.util.HashMap<>();
            map.put("id", u.getId());
            map.put("username", u.getUsername());
            map.put("fullName", u.getFullName());
            map.put("avatar", u.getAvatar());
            return map;
        }).collect(java.util.stream.Collectors.toList());
    }

    @GetMapping("/history")
    public ResponseEntity<?> getChatHistory(@RequestParam String targetUsername, Principal principal) {
        if (principal == null) return ResponseEntity.status(401).build();
        User u1 = userService.findByUsername(principal.getName()).orElseThrow();
        User u2 = userService.findByUsername(targetUsername).orElseThrow();
        List<MessageDTO> history = chatService.getChatHistory(u1, u2);
        boolean isFriend = friendRepository.findByUserAndFriend(u1, u2).isPresent();
        return ResponseEntity.ok(Map.of(
            "isFriend", isFriend,
            "messages", history
        ));
    }

    @PostMapping("/send")
    public ResponseEntity<?> sendMessage(@RequestBody Map<String, String> request, Principal principal) {
        if (principal == null) return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        String senderUsername = principal.getName();
        String receiverUsername = request.get("receiverUsername");
        String content = request.get("content");
        String imageUrl = request.get("imageUrl"); // có thể null nếu không gửi ảnh

        User sender = userService.findByUsername(senderUsername).orElseThrow();
        User receiver = userService.findByUsername(receiverUsername).orElseThrow();

        if (friendRepository.findByUserAndFriend(sender, receiver).isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Cannot send message to non-friend"));
        }

        chatService.saveMessage(sender, receiver, content, imageUrl);
        return ResponseEntity.ok(Map.of("status", "ok"));
    }

    @PutMapping("/revoke/{messageId}")
    public ResponseEntity<?> revokeMessage(@PathVariable Long messageId, Principal principal) {
        if (principal == null) return ResponseEntity.status(401).build();
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
        if (principal == null) return ResponseEntity.status(401).build();
        User user = userService.findByUsername(principal.getName()).orElseThrow();
        User other = userService.findByUsername(otherUsername).orElseThrow();
        chatService.clearChatHistory(user, other);
        return ResponseEntity.ok(Map.of("status", "ok"));
    }

    @PostMapping("/group")
    public ResponseEntity<?> createGroup(@RequestBody Map<String, Object> request, Principal principal) {
        if (principal == null) return ResponseEntity.status(401).build();
        String name = (String) request.get("name");
        List<String> usernames = (List<String>) request.get("usernames");
        String creator = principal.getName();
        if (!usernames.contains(creator)) {
            usernames.add(creator);
        }
        com.example.ptitsocialchat.entity.Conversation conv = chatService.createGroupConversation(name, usernames);
        return ResponseEntity.ok(Map.of("id", conv.getId(), "name", conv.getName(), "isGroupChat", conv.isGroupChat()));
    }

    @GetMapping("/groups")
    public List<Map<String, Object>> getGroups(Principal principal) {
        if (principal == null) return List.of();
        User user = userService.findByUsername(principal.getName()).orElseThrow();
        return chatService.getUserGroupConversations(user);
    }

    @GetMapping("/group-history")
    public ResponseEntity<?> getGroupHistory(@RequestParam Long conversationId, Principal principal) {
        if (principal == null) return ResponseEntity.status(401).build();
        User user = userService.findByUsername(principal.getName()).orElseThrow();
        com.example.ptitsocialchat.entity.Conversation conv = chatService.getConversation(conversationId).orElseThrow();
        
        if (conversationMemberRepository.findByConversationAndUser(conv, user).isEmpty()) {
            return ResponseEntity.status(403).body(Map.of("error", "Not a member of this group"));
        }
        
        List<MessageDTO> history = chatService.getGroupChatHistory(conv);
        return ResponseEntity.ok(Map.of(
            "isGroup", true,
            "messages", history
        ));
    }
    @GetMapping("/group/{conversationId}/members")
    public ResponseEntity<?> getGroupMembers(@PathVariable Long conversationId, Principal principal) {
        if (principal == null) return ResponseEntity.status(401).build();
        User user = userService.findByUsername(principal.getName()).orElseThrow();
        com.example.ptitsocialchat.entity.Conversation conv = chatService.getConversation(conversationId).orElseThrow();
        
        if (conversationMemberRepository.findByConversationAndUser(conv, user).isEmpty()) {
            return ResponseEntity.status(403).body(Map.of("error", "Not a member of this group"));
        }
        
        List<com.example.ptitsocialchat.entity.ConversationMember> members = conversationMemberRepository.findByConversation(conv);
        List<Map<String, Object>> result = members.stream().map(m -> {
            Map<String, Object> map = new java.util.HashMap<>();
            map.put("username", m.getUser().getUsername());
            map.put("fullName", m.getUser().getFullName());
            map.put("avatar", m.getUser().getAvatar());
            map.put("joinedAt", m.getJoinedAt());
            return map;
        }).collect(java.util.stream.Collectors.toList());
        
        return ResponseEntity.ok(result);
    }
}
