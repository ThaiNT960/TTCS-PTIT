package com.example.ptitsocialchat.controller;

import com.example.ptitsocialchat.dto.UpdateProfileRequest;
import com.example.ptitsocialchat.entity.User;
import com.example.ptitsocialchat.service.FriendService;
import com.example.ptitsocialchat.service.UserService;
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
        
        User viewerUser = null;
        if (principal != null) {
            viewerUser = userService.findByUsername(principal.getName()).orElse(null);
        }
        String friendshipStatus = "NONE";
        if (viewerUser != null) {
            friendshipStatus = friendService.getFriendshipStatus(viewerUser, targetUser);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("id", targetUser.getId());
        response.put("username", targetUser.getUsername());
        response.put("fullName", targetUser.getFullName());
        response.put("avatar", targetUser.getAvatar());
        response.put("coverPhoto", targetUser.getCoverPhoto());
        response.put("bio", targetUser.getBio());
        response.put("workplace", targetUser.getWorkplace());
        response.put("education", targetUser.getEducation());
        response.put("location", targetUser.getLocation());
        response.put("privacySetting", targetUser.getPrivacySetting());
        response.put("friendshipStatus", friendshipStatus);

        return ResponseEntity.ok(response);
    }

    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(Principal principal, @RequestBody UpdateProfileRequest request) {
        if (principal == null) {
            return ResponseEntity.status(401).body("Unauthorized");
        }
        try {
            User updatedUser = userService.updateProfile(principal.getName(), request);
            
            Map<String, Object> response = new HashMap<>();
            response.put("id", updatedUser.getId());
            response.put("username", updatedUser.getUsername());
            response.put("fullName", updatedUser.getFullName());
            response.put("avatar", updatedUser.getAvatar());
            response.put("coverPhoto", updatedUser.getCoverPhoto());
            response.put("bio", updatedUser.getBio());
            response.put("workplace", updatedUser.getWorkplace());
            response.put("education", updatedUser.getEducation());
            response.put("location", updatedUser.getLocation());
            response.put("privacySetting", updatedUser.getPrivacySetting());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
