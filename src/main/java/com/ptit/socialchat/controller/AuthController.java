package com.ptit.socialchat.controller;

import com.ptit.socialchat.dto.LoginRequest;
import com.ptit.socialchat.dto.RegisterUserRequest;
import com.ptit.socialchat.entity.User;
import com.ptit.socialchat.service.UserService;
import com.ptit.socialchat.config.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

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
        Optional<User> userOpt = userService.findByEmail(request.getEmail());
        if (userOpt.isEmpty()) {
            userOpt = userService.findByUsername(request.getEmail());
        }
        if (userOpt.isPresent() && passwordEncoder.matches(request.getPassword(), userOpt.get().getPassword())) {
            if (userOpt.get().isLocked()) {
                return ResponseEntity.status(403).body(Map.of("error", "Tài khoản của bạn đã bị khóa."));
            }
            // Sinh JWT Token
            String token = jwtTokenProvider.generateToken(userOpt.get().getUsername(), userOpt.get().getRole());

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
        return ResponseEntity.status(401).body(Map.of("error", "Email hoặc mật khẩu không chính xác."));
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
        if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email không được để trống"));
        }
        if (!request.getEmail().trim().endsWith("@student.ptit.edu.vn")) {
            return ResponseEntity.badRequest().body(Map.of("error", "Vui lòng sử dụng email sinh viên hợp lệ (@student.ptit.edu.vn)."));
        }
        if (userService.findByEmail(request.getEmail()).isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email này đã được sử dụng."));
        }
        if (request.getStudentId() != null && !request.getStudentId().trim().isEmpty()) {
            if (userService.findByStudentId(request.getStudentId().trim()).isPresent()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Mã sinh viên này đã được đăng ký."));
            }
        }
        
        String email = request.getEmail().trim();
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
        user.setPassword(request.getPassword());
        user.setFullName(request.getFullName());
        user.setStudentId(request.getStudentId());
        user.setMajor(request.getMajor());
        user.setRole("ROLE_USER");
        userService.save(user);
        return ResponseEntity.ok("User registered successfully");
    }


}



