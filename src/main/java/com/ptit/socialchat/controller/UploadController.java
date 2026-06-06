package com.ptit.socialchat.controller;

import com.ptit.socialchat.service.FileUploadService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * Controller xử lý các API liên quan đến upload file (ảnh, video).
 */
@RestController
@RequestMapping("/api/upload")
public class UploadController {

    @Autowired
    private FileUploadService fileUploadService;

    /**
     * Upload ảnh cho bài viết.
     * POST /api/upload/post-image
     * Body: multipart/form-data với field "imageFile"
     * Trả về: { "status": "ok", "imageUrl": "/uploads/posts/uuid-filename.jpg" }
     */
    @PostMapping("/post-image")
    public ResponseEntity<?> uploadPostImage(@RequestParam("imageFile") MultipartFile imageFile) throws Exception {
        String imageUrl = fileUploadService.saveFile(imageFile, "posts");
        return ResponseEntity.ok(Map.of("status", "ok", "imageUrl", imageUrl));
    }

    /**
     * Upload ảnh cho tin nhắn chat.
     * POST /api/upload/chat-image
     * Body: multipart/form-data với field "imageFile"
     * Trả về: { "status": "ok", "imageUrl": "/uploads/chats/uuid-filename.jpg" }
     */
    @PostMapping("/chat-image")
    public ResponseEntity<?> uploadChatImage(@RequestParam("imageFile") MultipartFile imageFile) throws Exception {
        String imageUrl = fileUploadService.saveFile(imageFile, "chats");
        return ResponseEntity.ok(Map.of("status", "ok", "imageUrl", imageUrl));
    }

    @PostMapping("/avatar")
    public ResponseEntity<?> uploadAvatar(@RequestParam("imageFile") MultipartFile imageFile) throws Exception {
        String imageUrl = fileUploadService.saveFile(imageFile, "avatars");
        return ResponseEntity.ok(Map.of("status", "ok", "imageUrl", imageUrl));
    }

    @PostMapping("/cover")
    public ResponseEntity<?> uploadCover(@RequestParam("imageFile") MultipartFile imageFile) throws Exception {
        String imageUrl = fileUploadService.saveFile(imageFile, "covers");
        return ResponseEntity.ok(Map.of("status", "ok", "imageUrl", imageUrl));
    }
}



