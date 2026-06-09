package com.ptit.socialchat.controller;

import com.ptit.socialchat.dto.UpdateProfileRequest;
import com.ptit.socialchat.entity.User;
import com.ptit.socialchat.service.FriendService;
import com.ptit.socialchat.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
public class UserController {
    @Autowired
    private UserService userService;
    @Autowired
    private FriendService friendService;

    @GetMapping("/profile/{targetUsername}")
    public ResponseEntity<?> getUserProfile(@PathVariable String targetUsername, Principal principal) {
        User targetUser = userService.findByUsername(targetUsername).orElse(null);
        if (targetUser == null) {
            return ResponseEntity.notFound().build();
        }
        
        User currentUser = null;
        if (principal != null) {
            currentUser = userService.findByUsername(principal.getName()).orElse(null);
        }

        if (targetUser.isLocked() || "ROLE_ADMIN".equals(targetUser.getRole())) {
            boolean isAdmin = false;
            if (currentUser != null && "ROLE_ADMIN".equals(currentUser.getRole())) {
                isAdmin = true;
            }
            if (!isAdmin) {
                if ("ROLE_ADMIN".equals(targetUser.getRole())) {
                    return ResponseEntity.notFound().build();
                }
                return ResponseEntity.status(403).body(Map.of("error", "Tài khoản này đã bị khóa."));
            }
        }
        
        User viewerUser = currentUser;
        String friendshipStatus = "NONE";
        if (viewerUser != null) {
            friendshipStatus = friendService.getFriendshipStatus(viewerUser, targetUser);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("id", targetUser.getId());
        response.put("username", targetUser.getUsername());
        response.put("email", targetUser.getEmail());
        response.put("fullName", targetUser.getFullName());
        response.put("avatar", targetUser.getAvatar());
        response.put("coverPhoto", targetUser.getCoverPhoto());
        response.put("bio", targetUser.getBio());
        response.put("studentId", targetUser.getStudentId());
        response.put("major", targetUser.getMajor());
        response.put("campus", targetUser.getCampus());

        response.put("friendshipStatus", friendshipStatus);

        return ResponseEntity.ok(response);
    }

    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(Principal principal, @RequestBody UpdateProfileRequest request) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }
        try {
            User updatedUser = userService.updateProfile(principal.getName(), request);
            
            Map<String, Object> response = new HashMap<>();
            response.put("id", updatedUser.getId());
            response.put("username", updatedUser.getUsername());
            response.put("email", updatedUser.getEmail());
            response.put("fullName", updatedUser.getFullName());
            response.put("avatar", updatedUser.getAvatar());
            response.put("coverPhoto", updatedUser.getCoverPhoto());
            response.put("bio", updatedUser.getBio());
            response.put("studentId", updatedUser.getStudentId());
            response.put("major", updatedUser.getMajor());
            response.put("campus", updatedUser.getCampus());
            
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(Principal principal, @RequestBody Map<String, String> request) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }
        try {
            String oldPassword = request.get("oldPassword");
            String newPassword = request.get("newPassword");
            userService.changePassword(principal.getName(), oldPassword, newPassword);
            return ResponseEntity.ok(Map.of("status", "ok", "message", "Đổi mật khẩu thành công."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // Tìm kiếm user theo keyword (username hoặc fullName)
    @GetMapping("/search")
    public List<Map<String, Object>> searchUsers(@RequestParam String keyword) {
        return userService.searchUsers(keyword).stream()
                .map(u -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", u.getId());
                    map.put("username", u.getUsername());
                    map.put("fullName", u.getFullName());
                    map.put("avatar", u.getAvatar());
                    return map;
                })
                .collect(Collectors.toList());
    }
}



