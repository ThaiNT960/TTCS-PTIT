package com.example.ptitsocialchat.controller;

import com.example.ptitsocialchat.entity.Notification;
import com.example.ptitsocialchat.entity.User;
import com.example.ptitsocialchat.service.NotificationService;
import com.example.ptitsocialchat.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
    @Autowired
    private NotificationService notificationService;
    @Autowired
    private UserService userService;

    @GetMapping
    public List<Map<String, Object>> getNotifications(Principal principal) {
        if (principal == null) return List.of();
        User user = userService.findByUsername(principal.getName()).orElseThrow();
        return notificationService.getNotificationsForUser(user).stream()
                .map(n -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", n.getId());
                    map.put("senderUsername", n.getSender().getUsername());
                    map.put("senderFullName", n.getSender().getFullName());
                    map.put("senderAvatar", n.getSender().getAvatar());
                    map.put("type", n.getType());
                    map.put("link", n.getLink());
                    map.put("isRead", n.isRead());
                    map.put("createdAt", n.getCreatedAt());
                    return map;
                })
                .collect(Collectors.toList());
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<?> markAsRead(@PathVariable Long id, Principal principal) {
        if (principal == null) return ResponseEntity.status(401).build();
        User currentUser = userService.findByUsername(principal.getName()).orElseThrow();
        notificationService.markAsRead(id, currentUser);
        return ResponseEntity.ok(Map.of("message", "Notification marked as read"));
    }

    @PutMapping("/read-all")
    public ResponseEntity<?> markAllAsRead(Principal principal) {
        if (principal == null) return ResponseEntity.status(401).build();
        User user = userService.findByUsername(principal.getName()).orElseThrow();
        notificationService.markAllAsRead(user);
        return ResponseEntity.ok(Map.of("message", "All notifications marked as read"));
    }
}
