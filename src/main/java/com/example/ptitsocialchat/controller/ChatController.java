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
    public List<User> getContacts(@RequestParam String username) {
        User user = userService.findByUsername(username).orElseThrow();
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
    public ResponseEntity<?> getChatHistory(@RequestParam String user1, @RequestParam String user2) {
        User u1 = userService.findByUsername(user1).orElseThrow();
        User u2 = userService.findByUsername(user2).orElseThrow();
        List<MessageDTO> history = chatService.getChatHistory(u1, u2);
        boolean isFriend = friendRepository.findByUserAndFriend(u1, u2).isPresent();
        return ResponseEntity.ok(Map.of(
            "isFriend", isFriend,
            "messages", history
        ));
    }

    @PostMapping("/send")
    public ResponseEntity<?> sendMessage(@RequestBody Map<String, String> request) {
        String senderUsername = request.get("senderUsername");
        String receiverUsername = request.get("receiverUsername");
        String content = request.get("content");
        String imageUrl = request.get("imageUrl"); // có thể null nếu không gửi ảnh

        User sender = userService.findByUsername(senderUsername).orElseThrow();
        User receiver = userService.findByUsername(receiverUsername).orElseThrow();

        chatService.saveMessage(sender, receiver, content, imageUrl);
        return ResponseEntity.ok(Map.of("status", "ok"));
    }

    @PutMapping("/revoke/{messageId}")
    public ResponseEntity<?> revokeMessage(@PathVariable Long messageId, @RequestParam String username) {
        User user = userService.findByUsername(username).orElseThrow();
        try {
            chatService.revokeMessage(messageId, user);
            return ResponseEntity.ok(Map.of("status", "ok"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/history/{otherUsername}")
    public ResponseEntity<?> clearHistory(@PathVariable String otherUsername, @RequestParam String username) {
        User user = userService.findByUsername(username).orElseThrow();
        User other = userService.findByUsername(otherUsername).orElseThrow();
        chatService.clearChatHistory(user, other);
        return ResponseEntity.ok(Map.of("status", "ok"));
    }

    @PostMapping("/group")
    public ResponseEntity<?> createGroup(@RequestBody Map<String, Object> request) {
        String name = (String) request.get("name");
        List<String> usernames = (List<String>) request.get("usernames");
        com.example.ptitsocialchat.entity.Conversation conv = chatService.createGroupConversation(name, usernames);
        return ResponseEntity.ok(conv);
    }

    @GetMapping("/groups")
    public List<Map<String, Object>> getGroups(@RequestParam String username) {
        User user = userService.findByUsername(username).orElseThrow();
        return chatService.getUserGroupConversations(user);
    }

    @GetMapping("/group-history")
    public ResponseEntity<?> getGroupHistory(@RequestParam Long conversationId) {
        com.example.ptitsocialchat.entity.Conversation conv = chatService.getConversation(conversationId).orElseThrow();
        List<MessageDTO> history = chatService.getGroupChatHistory(conv);
        return ResponseEntity.ok(Map.of(
            "isGroup", true,
            "messages", history
        ));
    }
}
