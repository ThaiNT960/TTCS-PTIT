package com.example.ptitsocialchat.controller;

import com.example.ptitsocialchat.dto.PostDTO;
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
    public List<PostDTO> getAllPosts(Principal principal,
            @RequestParam(required = false) String search) {
        User currentUser = null;
        if (principal != null) {
            currentUser = userService.findByUsername(principal.getName()).orElse(null);
        }
        return postService.getAllPosts(currentUser, search);
    }

    @GetMapping("/user/{targetUsername}")
    public List<PostDTO> getPostsByUser(@PathVariable String targetUsername,
            Principal principal) {
        User targetUser = userService.findByUsername(targetUsername).orElse(null);
        if (targetUser == null)
            return List.of();
        User viewerUser = null;
        if (principal != null) {
            viewerUser = userService.findByUsername(principal.getName()).orElse(null);
        }
        return postService.getPostsByUser(targetUser, viewerUser);
    }

    @PostMapping
    public ResponseEntity<?> createPost(@RequestBody Map<String, String> request, Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body("Unauthorized");
        }
        String content = request.get("content");
        String imageUrl = request.get("imageUrl");
        User user = userService.findByUsername(principal.getName()).orElseThrow();
        PostService.CreatePostResult result = postService.createPost(content, imageUrl, user);

        Map<String, String> response = new HashMap<>();
        response.put("status", result.getStatus());
        response.put("message", result.getMessage());
        if (result.getLabel() != null) {
            response.put("label", result.getLabel());
        }
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{postId}/comments")
    public ResponseEntity<?> addComment(@PathVariable Long postId, @RequestBody Map<String, String> request, Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body("Unauthorized");
        }
        String content = request.get("content");
        String parentIdStr = request.get("parentId");
        Long parentId = (parentIdStr != null && !parentIdStr.isEmpty() && !parentIdStr.equals("null"))
                ? Long.parseLong(parentIdStr)
                : null;
        User user = userService.findByUsername(principal.getName()).orElseThrow();
        postService.addComment(postId, content, user, parentId);
        return ResponseEntity.ok("Comment added");
    }

    @PostMapping("/comments/{commentId}/reaction")
    public ResponseEntity<?> reactToComment(@PathVariable Long commentId, @RequestBody Map<String, String> request, Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body("Unauthorized");
        }
        String reactionType = request.get("reactionType");
        User user = userService.findByUsername(principal.getName()).orElseThrow();
        boolean success = postService.reactToComment(commentId, user, reactionType != null ? reactionType : "LIKE");
        return ResponseEntity.ok(Map.of("success", success, "liked", success));
    }

    @PostMapping("/{postId}/like")
    public ResponseEntity<?> reactToPost(@PathVariable Long postId, @RequestBody Map<String, String> request, Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body("Unauthorized");
        }
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
        if (principal == null) {
            return ResponseEntity.status(401).body("Unauthorized");
        }
        User user = userService.findByUsername(principal.getName()).orElseThrow();
        try {
            postService.deletePost(postId, user);
            return ResponseEntity.ok("Post deleted");
        } catch (RuntimeException e) {
            return ResponseEntity.status(403).body(e.getMessage());
        }
    }
}
