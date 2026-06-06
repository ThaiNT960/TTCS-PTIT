package com.ptit.socialchat.controller;

import com.ptit.socialchat.entity.Announcement;
import com.ptit.socialchat.service.AnnouncementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/announcements")
public class AnnouncementController {

    @Autowired
    private AnnouncementService announcementService;

    @GetMapping
    public List<Announcement> getAnnouncements(@RequestParam(required = false) String search) {
        if (search != null && !search.trim().isEmpty()) {
            return announcementService.search(search.trim());
        }
        return announcementService.findAll();
    }

    @PostMapping
    public ResponseEntity<?> createAnnouncement(
            Principal principal,
            @RequestBody java.util.Map<String, String> request) {
        if (principal == null) {
            return ResponseEntity.status(401).body("Unauthorized");
        }
        String title = request.get("title");
        String content = request.get("content");
        
        announcementService.save(title, content, principal.getName());
        return ResponseEntity.ok(java.util.Map.of("status", "ok"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteAnnouncement(@PathVariable Long id, Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body("Unauthorized");
        }
        announcementService.deleteById(id, principal.getName());
        return ResponseEntity.ok(java.util.Map.of("status", "ok"));
    }
}



