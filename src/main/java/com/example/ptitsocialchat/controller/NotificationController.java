package com.example.ptitsocialchat.controller;

import com.example.ptitsocialchat.entity.Notification;
import com.example.ptitsocialchat.entity.User;
import com.example.ptitsocialchat.service.NotificationService;
import com.example.ptitsocialchat.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
    public List<Map<String, Object>> getNotifications(@RequestParam String username) {
        User user = userService.findByUsername(username).orElseThrow();
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
    public ResponseEntity<?> markAsRead(@PathVariable Long id) {
        notificationService.markAsRead(id);
        return ResponseEntity.ok("Notification marked as read");
    }

    @PutMapping("/read-all")
    public ResponseEntity<?> markAllAsRead(@RequestParam String username) {
        User user = userService.findByUsername(username).orElseThrow();
        notificationService.markAllAsRead(user);
        return ResponseEntity.ok("All notifications marked as read");
    }
}
