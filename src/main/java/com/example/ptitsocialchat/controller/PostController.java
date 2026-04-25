package com.example.ptitsocialchat.controller;

import com.example.ptitsocialchat.dto.PostDTO;
import com.example.ptitsocialchat.entity.User;
import com.example.ptitsocialchat.service.PostService;
import com.example.ptitsocialchat.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
    public List<PostDTO> getAllPosts(@RequestParam(required = false) String username) {
        if (username != null) {
            User currentUser = userService.findByUsername(username).orElse(null);
            return postService.getAllPosts(currentUser);
        }
        return postService.getAllPosts();
    }

    @GetMapping("/user/{targetUsername}")
    public ResponseEntity<?> getUserPosts(@PathVariable String targetUsername, @RequestParam(required = false) String viewer) {
        User targetUser = userService.findByUsername(targetUsername).orElse(null);
        if (targetUser == null) {
            return ResponseEntity.notFound().build();
        }
        User viewerUser = viewer != null ? userService.findByUsername(viewer).orElse(null) : null;
        return ResponseEntity.ok(postService.getUserPosts(targetUser, viewerUser));
    }

    @GetMapping("/search")
    public ResponseEntity<?> searchPosts(@RequestParam String keyword, @RequestParam(required = false) String viewer) {
        User viewerUser = viewer != null ? userService.findByUsername(viewer).orElse(null) : null;
        return ResponseEntity.ok(postService.searchPosts(keyword, viewerUser));
    }

    @PostMapping
    public ResponseEntity<?> createPost(@RequestBody Map<String, Object> request) {
        String username = (String) request.get("username");
        String content = (String) request.get("content");
        List<String> mediaUrls = (List<String>) request.get("mediaUrls");
        
        User user = userService.findByUsername(username).orElseThrow();
        postService.createPost(content, mediaUrls, user);
        return ResponseEntity.ok("Post created");
    }

    @PostMapping("/{postId}/comments")
    public ResponseEntity<?> addComment(@PathVariable Long postId, @RequestBody Map<String, Object> request) {
        String username = (String) request.get("username");
        String content = (String) request.get("content");
        Long parentCommentId = null;
        if (request.get("parentCommentId") != null) {
            parentCommentId = Long.valueOf(request.get("parentCommentId").toString());
        }
        User user = userService.findByUsername(username).orElseThrow();
        postService.addComment(postId, content, parentCommentId, user);
        return ResponseEntity.ok("Comment added");
    }

    @PostMapping("/{postId}/reaction")
    public ResponseEntity<?> reactToPost(@PathVariable Long postId, @RequestBody Map<String, String> request) {
        String username = request.get("username");
        String reactionType = request.get("reactionType");
        if (reactionType == null) reactionType = "LIKE"; // fallback
        User user = userService.findByUsername(username).orElseThrow();
        boolean reacted = postService.reactToPost(postId, user, reactionType);

        Map<String, Object> response = new HashMap<>();
        response.put("reacted", reacted);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/comments/{commentId}/reaction")
    public ResponseEntity<?> reactToComment(@PathVariable Long commentId, @RequestBody Map<String, String> request) {
        String username = request.get("username");
        String reactionType = request.get("reactionType");
        if (reactionType == null) reactionType = "LIKE"; // fallback
        User user = userService.findByUsername(username).orElseThrow();
        boolean reacted = postService.reactToComment(commentId, user, reactionType);

        Map<String, Object> response = new HashMap<>();
        response.put("reacted", reacted);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{postId}/reactions")
    public ResponseEntity<?> getPostReactions(@PathVariable Long postId) {
        return ResponseEntity.ok(postService.getPostReactions(postId));
    }

    @DeleteMapping("/{postId}")
    public ResponseEntity<?> deletePost(@PathVariable Long postId, @RequestParam String username) {
        User user = userService.findByUsername(username).orElseThrow();
        try {
            postService.deletePost(postId, user);
            return ResponseEntity.ok("Post deleted");
        } catch (RuntimeException e) {
            return ResponseEntity.status(403).body(e.getMessage());
        }
    }
}
