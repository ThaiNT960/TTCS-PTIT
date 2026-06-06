package com.ptit.socialchat.controller;

import com.ptit.socialchat.entity.*;
import com.ptit.socialchat.service.DocumentService;
import com.ptit.socialchat.service.UserService;
import com.ptit.socialchat.repository.ConversationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/document")
public class DocumentController {

    @Autowired
    private DocumentService documentService;

    @Autowired
    private UserService userService;
    
    @Autowired
    private ConversationRepository conversationRepository;

    @PostMapping("/upload")
    public ResponseEntity<?> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam("name") String name,
            @RequestParam("description") String description,
            @RequestParam("fileType") String fileType,
            @RequestParam(value = "category", required = false, defaultValue = "Khác") String category,
            @RequestParam("conversationId") Long conversationId,
            Principal principal) {
            
        if (principal == null) return ResponseEntity.status(401).body("Unauthorized");
        User user = userService.findByUsername(principal.getName()).orElseThrow();
        Conversation conv = conversationRepository.findById(conversationId).orElseThrow();
        
        // Giới hạn 50MB
        if (file.getSize() > 50 * 1024 * 1024) {
            return ResponseEntity.badRequest().body(Map.of("error", "Kích thước file vượt quá 50MB"));
        }
        
        try {
            GroupDocument doc = documentService.uploadDocument(file, name, description, fileType, category, conv, user);
            return ResponseEntity.ok(doc);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/group/{id}")
    public ResponseEntity<?> getGroupDocuments(@PathVariable Long id, Principal principal) {
        if (principal == null) return ResponseEntity.status(401).body("Unauthorized");
        
        List<GroupDocument> docs = documentService.getDocumentsByGroup(id);
        List<Map<String, Object>> result = docs.stream().map(d -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", d.getId());
            map.put("name", d.getName());
            map.put("description", d.getDescription());
            map.put("fileUrl", d.getFileUrl());
            map.put("fileType", d.getFileType());
            map.put("category", d.getCategory());
            map.put("sizeBytes", d.getSizeBytes());
            map.put("downloads", d.getDownloads());
            map.put("isPinned", d.getPinned());
            map.put("createdAt", d.getCreatedAt());
            map.put("uploader", Map.of("username", d.getUploader().getUsername(), "fullName", d.getUploader().getFullName()));
            return map;
        }).collect(Collectors.toList());
        
        return ResponseEntity.ok(result);
    }
    
    @PostMapping("/{id}/download")
    public ResponseEntity<?> incrementDownload(@PathVariable Long id) {
        documentService.incrementDownload(id);
        return ResponseEntity.ok(Map.of("status", "ok"));
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteDocument(@PathVariable Long id, Principal principal) {
        if (principal == null) return ResponseEntity.status(401).body("Unauthorized");
        User user = userService.findByUsername(principal.getName()).orElseThrow();
        
        try {
            documentService.deleteDocument(id, user);
            return ResponseEntity.ok(Map.of("status", "ok"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    @PostMapping("/{id}/pin")
    public ResponseEntity<?> togglePin(@PathVariable Long id, Principal principal) {
        if (principal == null) return ResponseEntity.status(401).body("Unauthorized");
        User user = userService.findByUsername(principal.getName()).orElseThrow();
        
        try {
            documentService.togglePinDocument(id, user);
            return ResponseEntity.ok(Map.of("status", "ok"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}



