package com.example.ptitsocialchat.controller;

import com.example.ptitsocialchat.dto.LoginRequest;
import com.example.ptitsocialchat.dto.RegisterUserRequest;
import com.example.ptitsocialchat.entity.User;
import com.example.ptitsocialchat.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    @Autowired
    private UserService userService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        Optional<User> userOpt = userService.findByUsername(request.getUsername());
        if (userOpt.isPresent() && userOpt.get().getPassword().equals(request.getPassword())) {
            Map<String, Object> response = new HashMap<>();
            response.put("id", userOpt.get().getId());
            response.put("username", userOpt.get().getUsername());
            response.put("role", userOpt.get().getRole());
            response.put("fullName", userOpt.get().getFullName());
            response.put("avatar", userOpt.get().getAvatar());
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.status(401).body("Invalid credentials");
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterUserRequest request) {
        if (userService.findByUsername(request.getUsername()).isPresent()) {
            return ResponseEntity.badRequest().body("Username already exists");
        }
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(request.getPassword());
        user.setFullName(request.getFullName());
        user.setRole("ROLE_USER");
        userService.save(user);
        return ResponseEntity.ok("User registered successfully");
    }

    // Tìm kiếm user theo keyword (username hoặc fullName)
    @GetMapping("/users/search")
    public List<Map<String, Object>> searchUsers(@RequestParam String keyword) {
        return userService.findAll().stream()
                .filter(u -> u.getUsername().toLowerCase().contains(keyword.toLowerCase())
                        || (u.getFullName() != null && u.getFullName().toLowerCase().contains(keyword.toLowerCase())))
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

    // Cập nhật hồ sơ cá nhân
    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        User user = userService.findByUsername(username).orElse(null);
        if (user == null) {
            return ResponseEntity.badRequest().body("User not found");
        }
        if (request.containsKey("fullName") && request.get("fullName") != null) {
            user.setFullName(request.get("fullName"));
        }
        if (request.containsKey("avatar") && request.get("avatar") != null) {
            user.setAvatar(request.get("avatar"));
        }
        userService.save(user);

        // Trả về thông tin mới để frontend cập nhật localStorage
        Map<String, Object> response = new HashMap<>();
        response.put("id", user.getId());
        response.put("username", user.getUsername());
        response.put("role", user.getRole());
        response.put("fullName", user.getFullName());
        response.put("avatar", user.getAvatar());
        return ResponseEntity.ok(response);
    }
}
