package com.example.ptitsocialchat.controller;

import com.example.ptitsocialchat.dto.FriendRequestDTO;
import com.example.ptitsocialchat.entity.User;
import com.example.ptitsocialchat.service.FriendService;
import com.example.ptitsocialchat.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/friends")
public class FriendController {
    @Autowired
    private FriendService friendService;
    @Autowired
    private UserService userService;

    @PostMapping("/request")
    public ResponseEntity<?> sendFriendRequest(@RequestBody Map<String, String> request, Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body("Unauthorized");
        }
        String receiverUsername = request.get("receiverUsername");
        User sender = userService.findByUsername(principal.getName()).orElseThrow();
        User receiver = userService.findByUsername(receiverUsername).orElseThrow();
        friendService.sendRequest(sender, receiver);
        return ResponseEntity.ok("Friend request sent");
    }

    @PostMapping("/accept/{requestId}")
    public ResponseEntity<?> acceptFriendRequest(@PathVariable Long requestId) {
        friendService.acceptRequest(requestId);
        return ResponseEntity.ok("Friend request accepted");
    }

    @PostMapping("/reject/{requestId}")
    public ResponseEntity<?> rejectFriendRequest(@PathVariable Long requestId) {
        friendService.rejectRequest(requestId);
        return ResponseEntity.ok("Friend request rejected");
    }

    @GetMapping
    public List<User> getFriends(Principal principal, @RequestParam(required = false) String username) {
        if (principal == null) {
            return List.of();
        }
        String targetUsername = (username != null && !username.isEmpty()) ? username : principal.getName();
        User user = userService.findByUsername(targetUsername).orElseThrow();
        return friendService.getFriends(user);
    }

    @GetMapping("/requests")
    public List<FriendRequestDTO> getPendingRequests(Principal principal) {
        if (principal == null) {
            return List.of();
        }
        User user = userService.findByUsername(principal.getName()).orElseThrow();
        return friendService.getPendingRequests(user).stream()
                .map(req -> {
                    FriendRequestDTO dto = new FriendRequestDTO();
                    dto.setId(req.getId());
                    dto.setSenderUsername(req.getSender().getUsername());
                    dto.setSenderFullName(req.getSender().getFullName());
                    dto.setStatus(req.getStatus());
                    dto.setCreatedAt(req.getCreatedAt());
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @DeleteMapping("/unfriend/{targetUsername}")
    public ResponseEntity<?> unfriend(@PathVariable String targetUsername, Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body("Unauthorized");
        }
        User current = userService.findByUsername(principal.getName()).orElseThrow();
        User target = userService.findByUsername(targetUsername).orElseThrow();
        friendService.unfriend(current, target);
        return ResponseEntity.ok(Map.of("status", "ok"));
    }
}
