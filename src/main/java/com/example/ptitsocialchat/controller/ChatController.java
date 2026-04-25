package com.example.ptitsocialchat.controller;

import com.example.ptitsocialchat.dto.MessageDTO;
import com.example.ptitsocialchat.entity.Conversation;
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
    private ChatService chatService;
    @Autowired
    private UserService userService;

    @GetMapping("/conversations")
    public List<Map<String, Object>> getConversations(@RequestParam String username) {
        User user = userService.findByUsername(username).orElseThrow();
        return chatService.getUserConversations(user);
    }

    @PostMapping("/group")
    public ResponseEntity<?> createGroup(@RequestBody Map<String, Object> request) {
        String name = (String) request.get("name");
        List<String> usernames = (List<String>) request.get("usernames");
        Conversation conv = chatService.createGroupConversation(name, usernames);
        return ResponseEntity.ok(conv);
    }

    @GetMapping("/conversation-with")
    public ResponseEntity<?> getConversationWith(@RequestParam String user1, @RequestParam String user2) {
        User u1 = userService.findByUsername(user1).orElseThrow();
        User u2 = userService.findByUsername(user2).orElseThrow();
        Conversation conv = chatService.getOrCreateConversation(u1, u2);
        
        Map<String, Object> map = new java.util.HashMap<>();
        map.put("id", conv.getId());
        map.put("isGroupChat", conv.isGroupChat());
        map.put("name", u2.getFullName());
        map.put("otherUsername", u2.getUsername());
        map.put("avatar", u2.getAvatar());
        
        return ResponseEntity.ok(map);
    }

    @GetMapping("/history")
    public List<MessageDTO> getChatHistory(
            @RequestParam(required = false) String user1, 
            @RequestParam(required = false) String user2,
            @RequestParam(required = false) Long conversationId) {
        
        Conversation conversation;
        if (conversationId != null) {
            conversation = chatService.getConversation(conversationId).orElseThrow();
        } else {
            User u1 = userService.findByUsername(user1).orElseThrow();
            User u2 = userService.findByUsername(user2).orElseThrow();
            conversation = chatService.getOrCreateConversation(u1, u2);
        }
        return chatService.getChatHistory(conversation);
    }
}
