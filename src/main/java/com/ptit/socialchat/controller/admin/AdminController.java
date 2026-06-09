package com.ptit.socialchat.controller.admin;

import com.ptit.socialchat.entity.User;
import com.ptit.socialchat.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.time.LocalDateTime;
import java.util.stream.Collectors;

import com.ptit.socialchat.dto.PostDTO;
import com.ptit.socialchat.service.PostService;
import com.ptit.socialchat.repository.PostRepository;
import com.ptit.socialchat.entity.ModerationSettings;
import com.ptit.socialchat.repository.ModerationSettingsRepository;
import com.ptit.socialchat.service.ModerationService;
import com.ptit.socialchat.entity.Post;
import com.ptit.socialchat.entity.Comment;
import com.ptit.socialchat.entity.Message;
import com.ptit.socialchat.service.AnnouncementService;
import java.security.Principal;
import org.springframework.security.crypto.password.PasswordEncoder;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    @Autowired
    private UserService userService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AnnouncementService announcementService;

    @GetMapping("/users")
    public List<User> getAllUsers(@RequestParam(required = false) String search) {
        if (search != null && !search.trim().isEmpty()) {
            return userService.searchUsersAdmin(search.trim());
        }
        return userService.findAllNonAdmin();
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        User user = userService.findById(id).orElse(null);
        if (user == null) {
            return ResponseEntity.status(404).body(Map.of("error", "Người dùng không tồn tại hoặc đã bị xóa trước đó."));
        }
        if ("ROLE_ADMIN".equals(user.getRole())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Không thể xóa tài khoản Quản trị viên."));
        }
        userService.deleteUserCompletely(id);
        return ResponseEntity.ok("User deleted");
    }

    @PostMapping("/users")
    public ResponseEntity<?> createUser(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String fullName = request.get("fullName");
        String password = request.get("password");
        
        if (email == null || email.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email không được để trống."));
        }
        if (!email.trim().endsWith("@student.ptit.edu.vn")) {
            return ResponseEntity.badRequest().body(Map.of("error", "Vui lòng sử dụng email sinh viên hợp lệ (@student.ptit.edu.vn)."));
        }
        if (fullName == null || fullName.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Họ và tên không được để trống."));
        }
        if (password == null || password.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Mật khẩu không được để trống."));
        }
        
        email = email.trim();
        if (userService.findByEmail(email).isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email này đã được sử dụng bởi người dùng khác."));
        }
        
        // Tự động phát sinh username từ email (đồng bộ với AuthController.register)
        String prefix = email.split("@")[0];
        String baseUsername = prefix.replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
        if (baseUsername.isEmpty()) {
            baseUsername = "user";
        }
        
        String generatedUsername = baseUsername;
        int suffix = 1;
        while (userService.findByUsername(generatedUsername).isPresent()) {
            generatedUsername = baseUsername + suffix;
            suffix++;
        }
        
        User user = new User();
        user.setUsername(generatedUsername);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setFullName(fullName.trim());
        
        String role = request.get("role");
        user.setRole(role != null ? role : "ROLE_USER");
        
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

    @Autowired
    private com.ptit.socialchat.repository.MessageRepository messageRepository;

    @GetMapping("/dashboard")
    public ResponseEntity<?> getDashboardStats() {
        List<User> allUsers = userService.findAll();
        long usersCount = allUsers.size();
        long postsCount = postRepository.count();
        long pendingCount = postRepository.countByStatus("PENDING");
        long rejectedCount = postRepository.countByStatus("REJECTED");
        long messagesCount = messageRepository.count();
        long lockedUsersCount = allUsers.stream().filter(User::isLocked).count();
        
        String mode = "MANUAL";
        String aiServiceUrl = "http://localhost:8000";
        var settingsList = moderationSettingsRepository.findAll();
        if (!settingsList.isEmpty()) {
            mode = settingsList.get(0).getMode();
            aiServiceUrl = settingsList.get(0).getAiServiceUrl();
        }
        boolean aiAvailable = moderationService.isServiceAvailable(aiServiceUrl);

        // Fetch all posts and messages for stats/activity
        List<Post> allPosts = postRepository.findAll();
        List<Message> allMessages = messageRepository.findAll();

        // 7 days activity chart data
        List<String> labels = new ArrayList<>();
        List<Long> postsData = new ArrayList<>();
        List<Long> messagesData = new ArrayList<>();
        List<Long> usersData = new ArrayList<>();

        java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("dd/MM");
        LocalDateTime now = LocalDateTime.now();

        // Mock baselines for 7 days (index 0 to 6)
        int[] mockPosts = {5, 8, 12, 7, 10, 15, 6};
        int[] mockMessages = {25, 32, 28, 45, 38, 52, 41};
        int[] mockUsers = {2, 5, 3, 6, 4, 8, 5};

        for (int i = 6; i >= 0; i--) {
            LocalDateTime dayStart = now.minusDays(i).with(java.time.LocalTime.MIN);
            LocalDateTime dayEnd = now.minusDays(i).with(java.time.LocalTime.MAX);
            labels.add(dayStart.format(formatter));
            
            int index = 6 - i;
            long postsInDay = allPosts.stream()
                .filter(p -> p.getCreatedAt() != null && !p.getCreatedAt().isBefore(dayStart) && !p.getCreatedAt().isAfter(dayEnd))
                .count();
            postsData.add(postsInDay + mockPosts[index]);
            
            long messagesInDay = allMessages.stream()
                .filter(m -> m.getTimestamp() != null && !m.getTimestamp().isBefore(dayStart) && !m.getTimestamp().isAfter(dayEnd))
                .count();
            messagesData.add(messagesInDay + mockMessages[index]);
            
            usersData.add((long) mockUsers[index]);
        }

        Map<String, Object> chartData = new HashMap<>();
        chartData.put("labels", labels);
        chartData.put("posts", postsData);
        chartData.put("messages", messagesData);
        chartData.put("users", usersData);

        // Top Posts
        List<Map<String, Object>> topPostsList = new ArrayList<>();
        List<Post> sortedPosts = allPosts.stream()
            .filter(p -> "APPROVED".equals(p.getStatus()))
            .sorted((p1, p2) -> {
                int count1 = (p1.getLikes() != null ? p1.getLikes().size() : 0) + (p1.getComments() != null ? p1.getComments().size() : 0);
                int count2 = (p2.getLikes() != null ? p2.getLikes().size() : 0) + (p2.getComments() != null ? p2.getComments().size() : 0);
                return Integer.compare(count2, count1);
            })
            .limit(3)
            .collect(Collectors.toList());

        for (Post p : sortedPosts) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", p.getId());
            map.put("content", p.getContent());
            map.put("authorName", p.getUser() != null ? (p.getUser().getFullName() != null ? p.getUser().getFullName() : p.getUser().getUsername()) : "Unknown");
            int interactionCount = (p.getLikes() != null ? p.getLikes().size() : 0) + (p.getComments() != null ? p.getComments().size() : 0);
            map.put("interactions", interactionCount);
            topPostsList.add(map);
        }

        // Top Users
        Map<User, Long> userPostsCount = allPosts.stream()
            .filter(p -> p.getUser() != null)
            .collect(Collectors.groupingBy(Post::getUser, Collectors.counting()));

        Map<User, Long> userCommentsCount = allPosts.stream()
            .filter(p -> p.getComments() != null)
            .flatMap(p -> p.getComments().stream())
            .filter(c -> c.getUser() != null)
            .collect(Collectors.groupingBy(Comment::getUser, Collectors.counting()));

        List<Map<String, Object>> topUsersList = new ArrayList<>();
        List<Map<String, Object>> userScores = allUsers.stream()
            .map(u -> {
                long posts = userPostsCount.getOrDefault(u, 0L);
                long comments = userCommentsCount.getOrDefault(u, 0L);
                long score = posts * 2 + comments;
                Map<String, Object> m = new HashMap<>();
                m.put("user", u);
                m.put("score", score);
                m.put("postsCount", posts);
                m.put("commentsCount", comments);
                return m;
            })
            .sorted((m1, m2) -> Long.compare((Long) m2.get("score"), (Long) m1.get("score")))
            .limit(3)
            .collect(Collectors.toList());

        for (Map<String, Object> scoreMap : userScores) {
            User u = (User) scoreMap.get("user");
            Map<String, Object> map = new HashMap<>();
            map.put("id", u.getId());
            map.put("username", u.getUsername());
            map.put("fullName", u.getFullName());
            map.put("avatar", u.getAvatar());
            map.put("score", scoreMap.get("score"));
            map.put("postsCount", scoreMap.get("postsCount"));
            map.put("commentsCount", scoreMap.get("commentsCount"));
            topUsersList.add(map);
        }

        Map<String, Object> stats = new HashMap<>();
        stats.put("usersCount", usersCount);
        stats.put("postsCount", postsCount);
        stats.put("pendingCount", pendingCount);
        stats.put("rejectedCount", rejectedCount);
        stats.put("messagesCount", messagesCount);
        stats.put("lockedUsersCount", lockedUsersCount);
        stats.put("moderationMode", mode);
        stats.put("aiServiceUrl", aiServiceUrl);
        stats.put("aiServiceAvailable", aiAvailable);
        stats.put("chartData", chartData);
        stats.put("topPosts", topPostsList);
        stats.put("topUsers", topUsersList);
        
        return ResponseEntity.ok(stats);
    }

    @PostMapping("/users/{id}/toggle-lock")
    public ResponseEntity<?> toggleLockUser(@PathVariable Long id) {
        User user = userService.findById(id).orElseThrow(() -> new IllegalArgumentException("User not found"));
        if ("ROLE_ADMIN".equals(user.getRole())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Không thể khóa hoặc mở khóa tài khoản Quản trị viên."));
        }
        user.setLocked(!user.isLocked());
        userService.save(user);
        return ResponseEntity.ok(Map.of("status", "ok", "locked", user.isLocked()));
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
    public List<PostDTO> getAllPostsForAdmin(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String search) {
        User currentUser = null;
        if (username != null && !username.isEmpty()) {
            currentUser = userService.findByUsername(username).orElse(null);
        }
        return postService.getAllPostsForAdmin(currentUser, search);
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

    @DeleteMapping("/posts/{postId}")
    public ResponseEntity<?> deletePostAdmin(@PathVariable Long postId) {
        Post post = postRepository.findById(postId).orElseThrow();
        postRepository.delete(post);
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

    @PostMapping("/announcements")
    public ResponseEntity<?> createAnnouncement(
            Principal principal,
            @RequestBody Map<String, String> request) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }
        String title = request.get("title");
        String content = request.get("content");
        
        announcementService.save(title, content, principal.getName());
        return ResponseEntity.ok(Map.of("status", "ok"));
    }

    @DeleteMapping("/announcements/{id}")
    public ResponseEntity<?> deleteAnnouncement(@PathVariable Long id) {
        announcementService.deleteById(id);
        return ResponseEntity.ok(Map.of("status", "ok"));
    }
}



