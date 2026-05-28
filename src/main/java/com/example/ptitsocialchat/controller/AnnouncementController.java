package com.example.ptitsocialchat.controller;

import com.example.ptitsocialchat.entity.Announcement;
import com.example.ptitsocialchat.service.AnnouncementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.security.Principal;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/api/announcements")
public class AnnouncementController {

    @Autowired
    private AnnouncementService announcementService;

    @GetMapping
    public List<Announcement> getAnnouncements() {
        return announcementService.findAll();
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createAnnouncement(
            Principal principal,
            @RequestBody java.util.Map<String, String> request) {
        if (principal == null) return ResponseEntity.status(401).build();
        String username = principal.getName();
        String title = request.get("title");
        String content = request.get("content");
        
        try {
            announcementService.save(title, content, username);
            return ResponseEntity.ok(java.util.Map.of("status", "ok"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteAnnouncement(@PathVariable Long id, Principal principal) {
        if (principal == null) return ResponseEntity.status(401).build();
        String username = principal.getName();
        try {
            announcementService.deleteById(id, username);
            return ResponseEntity.ok(java.util.Map.of("status", "ok"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", e.getMessage()));
        }
    }
}
