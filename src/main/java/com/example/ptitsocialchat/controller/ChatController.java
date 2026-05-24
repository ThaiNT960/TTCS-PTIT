package com.example.ptitsocialchat.controller;

import com.example.ptitsocialchat.dto.MessageDTO;
import com.example.ptitsocialchat.entity.User;
import com.example.ptitsocialchat.service.ChatService;
import com.example.ptitsocialchat.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

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

    @GetMapping("/contacts")
    public List<User> getContacts(Principal principal) {
        if (principal == null) {
            return List.of();
        }
        User user = userService.findByUsername(principal.getName()).orElseThrow();
        List<User> friends = friendRepository.findByUser(user).stream()
                .map(com.example.ptitsocialchat.entity.Friend::getFriend).collect(java.util.stream.Collectors.toList());
        List<User> chattedReceivers = messageRepository.findReceiversByUser(user);
        List<User> chattedSenders = messageRepository.findSendersByUser(user);
        
        java.util.Set<User> contacts = new java.util.LinkedHashSet<>(friends);
        contacts.addAll(chattedReceivers);
        contacts.addAll(chattedSenders);
        return new java.util.ArrayList<>(contacts);
    }

    @GetMapping("/history")
    public ResponseEntity<?> getChatHistory(Principal principal, @RequestParam String user2) {
        if (principal == null) {
            return ResponseEntity.status(401).body("Unauthorized");
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

    @PostMapping("/send")
    public ResponseEntity<?> sendMessage(@RequestBody Map<String, String> request, Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body("Unauthorized");
        }
        String receiverUsername = request.get("receiverUsername");
        String content = request.get("content");
        String imageUrl = request.get("imageUrl"); // có thể null nếu không gửi ảnh

        User sender = userService.findByUsername(principal.getName()).orElseThrow();
        User receiver = userService.findByUsername(receiverUsername).orElseThrow();

        chatService.saveMessage(sender, receiver, content, imageUrl);
        return ResponseEntity.ok(Map.of("status", "ok"));
    }

    @PutMapping("/revoke/{messageId}")
    public ResponseEntity<?> revokeMessage(@PathVariable Long messageId, Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body("Unauthorized");
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
            return ResponseEntity.status(401).body("Unauthorized");
        }
        User user = userService.findByUsername(principal.getName()).orElseThrow();
        User other = userService.findByUsername(otherUsername).orElseThrow();
        chatService.clearChatHistory(user, other);
        return ResponseEntity.ok(Map.of("status", "ok"));
    }

    @PostMapping("/group")
    public ResponseEntity<?> createGroup(@RequestBody Map<String, Object> request) {
        String name = (String) request.get("name");
        List<String> usernames = (List<String>) request.get("usernames");
        if (usernames == null || usernames.size() < 3) {
            return ResponseEntity.badRequest().body(Map.of("error", "Nhóm chat phải có từ 3 người trở lên."));
        }
        com.example.ptitsocialchat.entity.Conversation conv = chatService.createGroupConversation(name, usernames);
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
            return ResponseEntity.status(401).body("Unauthorized");
        }
        User user = userService.findByUsername(principal.getName()).orElseThrow();
        com.example.ptitsocialchat.entity.Conversation conv = chatService.getConversation(conversationId).orElseThrow();
        List<MessageDTO> history = chatService.getGroupChatHistory(conv, user);
        return ResponseEntity.ok(Map.of(
            "isGroup", true,
            "messages", history
        ));
    }

    @DeleteMapping("/group-history/{conversationId}")
    public ResponseEntity<?> clearGroupHistory(@PathVariable Long conversationId, Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body("Unauthorized");
        }
        User user = userService.findByUsername(principal.getName()).orElseThrow();
        com.example.ptitsocialchat.entity.Conversation conv = chatService.getConversation(conversationId).orElseThrow();
        chatService.clearGroupChatHistory(user, conv);
        return ResponseEntity.ok(Map.of("status", "ok"));
    }

    @PostMapping("/group/{conversationId}/leave")
    public ResponseEntity<?> leaveGroup(@PathVariable Long conversationId, Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body("Unauthorized");
        }
        User user = userService.findByUsername(principal.getName()).orElseThrow();
        com.example.ptitsocialchat.entity.Conversation conv = chatService.getConversation(conversationId).orElseThrow();
        chatService.leaveGroupConversation(user, conv);
        return ResponseEntity.ok(Map.of("status", "ok"));
    }

    @GetMapping("/group/{conversationId}/non-members")
    public List<User> getGroupNonMembers(@PathVariable Long conversationId, Principal principal) {
        if (principal == null) {
            return List.of();
        }
        User user = userService.findByUsername(principal.getName()).orElseThrow();
        com.example.ptitsocialchat.entity.Conversation conv = chatService.getConversation(conversationId).orElseThrow();
        return chatService.getGroupNonMembers(conv, user);
    }

    @PostMapping("/group/{conversationId}/add-members")
    public ResponseEntity<?> addMembersToGroup(@PathVariable Long conversationId, @RequestBody Map<String, Object> request, Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body("Unauthorized");
        }
        User currentUser = userService.findByUsername(principal.getName()).orElseThrow();
        List<String> usernames = (List<String>) request.get("usernames");
        com.example.ptitsocialchat.entity.Conversation conv = chatService.getConversation(conversationId).orElseThrow();
        chatService.addMembersToGroupConversation(conv, usernames, currentUser);
        return ResponseEntity.ok(Map.of("status", "ok"));
    }
}
