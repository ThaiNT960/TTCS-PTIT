package com.example.ptitsocialchat.controller;

import com.example.ptitsocialchat.service.FileUploadService;
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
    public ResponseEntity<?> uploadPostImage(@RequestParam("imageFile") MultipartFile imageFile) {
        try {
            String imageUrl = fileUploadService.saveFile(imageFile, "posts");
            return ResponseEntity.ok(Map.of("status", "ok", "imageUrl", imageUrl));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Lỗi server khi upload ảnh: " + e.getMessage()));
        }
    }

    /**
     * Upload ảnh cho tin nhắn chat.
     * POST /api/upload/chat-image
     * Body: multipart/form-data với field "imageFile"
     * Trả về: { "status": "ok", "imageUrl": "/uploads/chats/uuid-filename.jpg" }
     */
    @PostMapping("/chat-image")
    public ResponseEntity<?> uploadChatImage(@RequestParam("imageFile") MultipartFile imageFile) {
        try {
            String imageUrl = fileUploadService.saveFile(imageFile, "chats");
            return ResponseEntity.ok(Map.of("status", "ok", "imageUrl", imageUrl));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Lỗi server khi upload ảnh: " + e.getMessage()));
        }
    }

    @PostMapping("/avatar")
    public ResponseEntity<?> uploadAvatar(@RequestParam("imageFile") MultipartFile imageFile) {
        try {
            String imageUrl = fileUploadService.saveFile(imageFile, "avatars");
            return ResponseEntity.ok(Map.of("status", "ok", "imageUrl", imageUrl));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of("error", "Lỗi server: " + e.getMessage()));
        }
    }

    @PostMapping("/cover")
    public ResponseEntity<?> uploadCover(@RequestParam("imageFile") MultipartFile imageFile) {
        try {
            String imageUrl = fileUploadService.saveFile(imageFile, "covers");
            return ResponseEntity.ok(Map.of("status", "ok", "imageUrl", imageUrl));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of("error", "Lỗi server: " + e.getMessage()));
        }
    }
}
