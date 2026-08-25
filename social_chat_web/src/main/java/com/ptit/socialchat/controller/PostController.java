package com.ptit.socialchat.controller;

import com.ptit.socialchat.dto.PostDTO;
import com.ptit.socialchat.entity.User;
import com.ptit.socialchat.service.PostService;
import com.ptit.socialchat.service.UserService;
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
    public Map<String, Object> getPostsByUser(@PathVariable String targetUsername,
            Principal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        User targetUser = userService.findByUsername(targetUsername).orElse(null);
        if (targetUser == null)
            return Map.of("content", List.of(), "currentPage", 0, "totalPages", 0);
        User viewerUser = null;
        if (principal != null) {
            viewerUser = userService.findByUsername(principal.getName()).orElse(null);
        }
        if (targetUser.isLocked()) {
            boolean isAdmin = viewerUser != null && "ROLE_ADMIN".equals(viewerUser.getRole());
            if (!isAdmin) {
                throw new IllegalArgumentException("Tài khoản của người dùng này đã bị khóa.");
            }
        }
        return postService.getPostsByUser(targetUser, viewerUser, page, size);
    }

    @GetMapping("/{postId}")
    public ResponseEntity<PostDTO> getPost(@PathVariable Long postId, Principal principal) {
        User currentUser = null;
        if (principal != null) {
            currentUser = userService.findByUsername(principal.getName()).orElse(null);
        }
        try {
            PostDTO postDTO = postService.getPost(postId, currentUser);
            return ResponseEntity.ok(postDTO);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    public ResponseEntity<?> createPost(@RequestBody Map<String, String> request, Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
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
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
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
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }
        String reactionType = request.get("reactionType");
        User user = userService.findByUsername(principal.getName()).orElseThrow();
        boolean success = postService.reactToComment(commentId, user, reactionType != null ? reactionType : "LIKE");
        return ResponseEntity.ok(Map.of("success", success, "liked", success));
    }

    @PostMapping("/{postId}/like")
    public ResponseEntity<?> reactToPost(@PathVariable Long postId, @RequestBody Map<String, String> request, Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
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
        if (principal == null) return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        User user = userService.findByUsername(principal.getName()).orElseThrow();
        try {
            postService.deletePost(postId, user);
            return ResponseEntity.ok(Map.of("status", "ok"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<?> deleteComment(@PathVariable Long commentId, Principal principal) {
        if (principal == null) return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        User user = userService.findByUsername(principal.getName()).orElseThrow();
        try {
            postService.deleteComment(commentId, user);
            return ResponseEntity.ok(Map.of("status", "ok"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}



