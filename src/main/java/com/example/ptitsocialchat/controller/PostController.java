package com.example.ptitsocialchat.controller;

import com.example.ptitsocialchat.dto.PostDTO;
import com.example.ptitsocialchat.entity.Post;
import com.example.ptitsocialchat.entity.User;
import com.example.ptitsocialchat.service.PostService;
import com.example.ptitsocialchat.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/posts")
public class PostController {
    @Autowired
    private PostService postService;
    @Autowired
    private UserService userService;

    @GetMapping
    public Map<String, Object> getAllPosts(Principal principal, 
                                           @RequestParam(required = false) String search,
                                           @RequestParam(defaultValue = "0") int page,
                                           @RequestParam(defaultValue = "10") int size) {
        User currentUser = null;
        if (principal != null) {
            currentUser = userService.findByUsername(principal.getName()).orElse(null);
        }
        return postService.getAllPosts(currentUser, search, page, size);
    }

    @GetMapping("/user/{targetUsername}")
    public Map<String, Object> getPostsByUser(@PathVariable String targetUsername, Principal principal,
                                              @RequestParam(defaultValue = "0") int page,
                                              @RequestParam(defaultValue = "10") int size) {
        User targetUser = userService.findByUsername(targetUsername).orElse(null);
        if (targetUser == null)
            return Map.of("content", List.of(), "currentPage", 0, "totalPages", 0);
        User viewerUser = null;
        if (principal != null) {
            viewerUser = userService.findByUsername(principal.getName()).orElse(null);
        }
        return postService.getPostsByUser(targetUser, viewerUser, page, size);
    }

    @PostMapping
    public ResponseEntity<?> createPost(@RequestBody Map<String, String> request, Principal principal) {
        if (principal == null) return ResponseEntity.status(401).build();
        String content = request.get("content");
        String imageUrl = request.get("imageUrl");
        User user = userService.findByUsername(principal.getName()).orElseThrow();
        Object post = postService.createPost(content, imageUrl, user);
        return ResponseEntity.ok(post);
    }

    @PostMapping("/{postId}/comments")
    public ResponseEntity<?> addComment(@PathVariable Long postId, @RequestBody Map<String, String> request, Principal principal) {
        if (principal == null) return ResponseEntity.status(401).build();
        String content = request.get("content");
        String parentIdStr = request.get("parentId");
        Long parentId = (parentIdStr != null && !parentIdStr.isEmpty() && !parentIdStr.equals("null"))
                ? Long.parseLong(parentIdStr)
                : null;
        User user = userService.findByUsername(principal.getName()).orElseThrow();
        postService.addComment(postId, content, user, parentId);
        return ResponseEntity.ok(Map.of("message", "Comment added"));
    }

    @PostMapping("/comments/{commentId}/reaction")
    public ResponseEntity<?> reactToComment(@PathVariable Long commentId, @RequestBody Map<String, String> request, Principal principal) {
        if (principal == null) return ResponseEntity.status(401).build();
        String reactionType = request.getOrDefault("reactionType", "LIKE");
        User user = userService.findByUsername(principal.getName()).orElseThrow();
        boolean success = postService.reactToComment(commentId, user, reactionType);
        return ResponseEntity.ok(Map.of("success", success, "liked", success));
    }

    @PostMapping("/{postId}/like")
    public ResponseEntity<?> reactToPost(@PathVariable Long postId, @RequestBody Map<String, String> request, Principal principal) {
        if (principal == null) return ResponseEntity.status(401).build();
        String reactionType = request.get("reactionType");
        User user = userService.findByUsername(principal.getName()).orElseThrow();
        boolean success = postService.reactToPost(postId, user, reactionType != null ? reactionType : "LIKE");

        Map<String, Object> response = new HashMap<>();
        response.put("success", success);
        response.put("liked", success);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{postId}/reactions")
    public ResponseEntity<List<PostDTO.ReactionUserDTO>> getPostReactions(@PathVariable Long postId) {
        return ResponseEntity.ok(postService.getPostReactions(postId));
    }

    @DeleteMapping("/{postId}")
    public ResponseEntity<?> deletePost(@PathVariable Long postId, Principal principal) {
        if (principal == null) return ResponseEntity.status(401).build();
        User user = userService.findByUsername(principal.getName()).orElseThrow();
        try {
            postService.deletePost(postId, user);
            return ResponseEntity.ok(Map.of("message", "Post deleted"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(403).body(Map.of("error", e.getMessage()));
        }
    }
}
