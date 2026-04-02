package com.example.ptitsocialchat.controller;

import com.example.ptitsocialchat.dto.MessageDTO;
import com.example.ptitsocialchat.entity.User;
import com.example.ptitsocialchat.service.ChatService;
import com.example.ptitsocialchat.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
public class ChatController {
    @Autowired
    private ChatService chatService;
    @Autowired
    private UserService userService;

    @GetMapping("/history")
    public List<MessageDTO> getChatHistory(@RequestParam String user1, @RequestParam String user2) {
        User u1 = userService.findByUsername(user1).orElseThrow();
        User u2 = userService.findByUsername(user2).orElseThrow();
        return chatService.getChatHistory(u1, u2);
    }
}
