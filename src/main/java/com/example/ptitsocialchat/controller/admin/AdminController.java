package com.example.ptitsocialchat.controller.admin;

import com.example.ptitsocialchat.entity.User;
import com.example.ptitsocialchat.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
        return ResponseEntity.ok("User created/updated");
    }
}
