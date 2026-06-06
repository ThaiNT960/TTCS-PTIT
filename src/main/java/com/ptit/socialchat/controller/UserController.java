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
                return ResponseEntity.status(403).body("Tài khoản này đã bị khóa.");
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
            return ResponseEntity.status(401).body("Unauthorized");
        }
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
    }
}



