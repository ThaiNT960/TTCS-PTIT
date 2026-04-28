package com.example.ptitsocialchat.controller.admin;

import com.example.ptitsocialchat.entity.User;
import com.example.ptitsocialchat.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

import com.example.ptitsocialchat.dto.PostDTO;
import com.example.ptitsocialchat.service.PostService;
import com.example.ptitsocialchat.repository.PostRepository;
import com.example.ptitsocialchat.entity.ModerationSettings;
import com.example.ptitsocialchat.repository.ModerationSettingsRepository;
import com.example.ptitsocialchat.service.ModerationService;
import com.example.ptitsocialchat.entity.Post;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    @Autowired
    private UserService userService;

    @GetMapping("/users")
    public List<User> getAllUsers() {
        return userService.findAll();
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        userService.deleteById(id);
        return ResponseEntity.ok("User deleted");
    }

    @PostMapping("/users")
    public ResponseEntity<?> createUser(@RequestBody User user) {
        userService.save(user);
        return ResponseEntity.ok(Map.of("status", "ok"));
    }

    @Autowired
    private PostService postService;
    @Autowired
    private PostRepository postRepository;
    @Autowired
    private ModerationSettingsRepository moderationSettingsRepository;
    @Autowired
    private ModerationService moderationService;

    @GetMapping("/dashboard")
    public ResponseEntity<?> getDashboardStats() {
        long usersCount = userService.findAll().size();
        long postsCount = postRepository.count();
        long pendingCount = postRepository.countByStatus("PENDING");
        long rejectedCount = postRepository.countByStatus("REJECTED");
        
        String mode = "MANUAL";
        String aiServiceUrl = "http://localhost:8000";
        var settingsList = moderationSettingsRepository.findAll();
        if (!settingsList.isEmpty()) {
            mode = settingsList.get(0).getMode();
            aiServiceUrl = settingsList.get(0).getAiServiceUrl();
        }
        boolean aiAvailable = moderationService.isServiceAvailable(aiServiceUrl);

        Map<String, Object> stats = new HashMap<>();
        stats.put("usersCount", usersCount);
        stats.put("postsCount", postsCount);
        stats.put("pendingCount", pendingCount);
        stats.put("rejectedCount", rejectedCount);
        stats.put("moderationMode", mode);
        stats.put("aiServiceUrl", aiServiceUrl);
        stats.put("aiServiceAvailable", aiAvailable);
        
        return ResponseEntity.ok(stats);
    }

    @PostMapping("/moderation/mode")
    public ResponseEntity<?> setModerationMode(@RequestBody Map<String, String> request) {
        String mode = request.get("mode");
        if (mode == null || (!mode.equals("NONE") && !mode.equals("MANUAL") && !mode.equals("AUTO_AI"))) {
            return ResponseEntity.badRequest().body(Map.of("error", "Chế độ không hợp lệ."));
        }
        var settingsList = moderationSettingsRepository.findAll();
        ModerationSettings settings;
        if (settingsList.isEmpty()) {
            settings = new ModerationSettings();
        } else {
            settings = settingsList.get(0);
        }
        settings.setMode(mode);
        moderationSettingsRepository.save(settings);
        return ResponseEntity.ok(Map.of("status", "ok", "mode", mode));
    }

    @GetMapping("/moderation/ai-status")
    public ResponseEntity<?> checkAiStatus() {
        var settingsList = moderationSettingsRepository.findAll();
        String aiServiceUrl = "http://localhost:8000";
        if (!settingsList.isEmpty()) {
            aiServiceUrl = settingsList.get(0).getAiServiceUrl();
        }
        boolean available = moderationService.isServiceAvailable(aiServiceUrl);
        return ResponseEntity.ok(Map.of("available", available, "url", aiServiceUrl));
    }

    @GetMapping("/posts")
    public List<PostDTO> getAllPostsForAdmin(@RequestParam(required = false) String username) {
        User currentUser = null;
        if (username != null && !username.isEmpty()) {
            currentUser = userService.findByUsername(username).orElse(null);
        }
        return postService.getAllPostsForAdmin(currentUser);
    }

    @PostMapping("/posts/{postId}/approve")
    public ResponseEntity<?> approvePost(@PathVariable Long postId) {
        Post post = postRepository.findById(postId).orElseThrow();
        post.setStatus("APPROVED");
        postRepository.save(post);
        return ResponseEntity.ok(Map.of("status", "ok"));
    }

    @PostMapping("/posts/{postId}/reject")
    public ResponseEntity<?> rejectPost(@PathVariable Long postId) {
        Post post = postRepository.findById(postId).orElseThrow();
        post.setStatus("REJECTED");
        postRepository.save(post);
        return ResponseEntity.ok(Map.of("status", "ok"));
    }

    @PostMapping("/posts/approve-all")
    public ResponseEntity<?> approveAll() {
        List<Post> pendingPosts = postRepository.findByStatus("PENDING");
        int count = pendingPosts.size();
        for (Post p : pendingPosts) {
            p.setStatus("APPROVED");
        }
        postRepository.saveAll(pendingPosts);
        return ResponseEntity.ok(Map.of("status", "ok", "count", count));
    }
}
