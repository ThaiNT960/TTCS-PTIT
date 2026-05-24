package com.example.ptitsocialchat.controller;

import com.example.ptitsocialchat.dto.LoginRequest;
import com.example.ptitsocialchat.dto.RegisterUserRequest;
import com.example.ptitsocialchat.entity.User;
import com.example.ptitsocialchat.service.UserService;
import com.example.ptitsocialchat.config.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;

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

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request, HttpServletResponse response) {
        Optional<User> userOpt = userService.findByUsername(request.getUsername());
        if (userOpt.isPresent() && passwordEncoder.matches(request.getPassword(), userOpt.get().getPassword())) {
            // Sinh JWT Token
            String token = jwtTokenProvider.generateToken(userOpt.get().getUsername());

            // Thiết lập JWT Token vào HTTP-Only Cookie
            Cookie cookie = new Cookie(JwtTokenProvider.COOKIE_NAME, token);
            cookie.setHttpOnly(true);
            cookie.setSecure(false); // set true khi chạy https thực tế
            cookie.setPath("/");
            cookie.setMaxAge(86400); // 1 ngày
            response.addCookie(cookie);

            Map<String, Object> resBody = new HashMap<>();
            resBody.put("id", userOpt.get().getId());
            resBody.put("username", userOpt.get().getUsername());
            resBody.put("role", userOpt.get().getRole());
            resBody.put("fullName", userOpt.get().getFullName());
            resBody.put("avatar", userOpt.get().getAvatar());
            return ResponseEntity.ok(resBody);
        }
        return ResponseEntity.status(401).body("Invalid credentials");
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletResponse response) {
        // Ghi đè cookie accessToken với thời gian sống bằng 0 để xóa cookie trên client
        Cookie cookie = new Cookie(JwtTokenProvider.COOKIE_NAME, null);
        cookie.setHttpOnly(true);
        cookie.setSecure(false);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
        return ResponseEntity.ok(Map.of("status", "ok", "message", "Logged out successfully"));
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
}
