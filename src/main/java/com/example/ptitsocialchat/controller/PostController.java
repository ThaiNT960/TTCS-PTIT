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

    @PostMapping
    public ResponseEntity<?> createPost(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        String content = request.get("content");
        String imageUrl = request.get("imageUrl");
        User user = userService.findByUsername(username).orElseThrow();
        postService.createPost(content, imageUrl, user);
        return ResponseEntity.ok("Post created");
    }

    @PostMapping("/{postId}/comments")
    public ResponseEntity<?> addComment(@PathVariable Long postId, @RequestBody Map<String, String> request) {
        String username = request.get("username");
        String content = request.get("content");
        User user = userService.findByUsername(username).orElseThrow();
        postService.addComment(postId, content, user);
        return ResponseEntity.ok("Comment added");
    }

    @PostMapping("/{postId}/like")
    public ResponseEntity<?> toggleLike(@PathVariable Long postId, @RequestBody Map<String, String> request) {
        String username = request.get("username");
        User user = userService.findByUsername(username).orElseThrow();
        boolean liked = postService.toggleLike(postId, user);
        long likeCount = postService.getLikeCount(postId);

        Map<String, Object> response = new HashMap<>();
        response.put("liked", liked);
        response.put("likeCount", likeCount);
        return ResponseEntity.ok(response);
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
