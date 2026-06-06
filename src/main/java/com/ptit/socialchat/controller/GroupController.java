package com.ptit.socialchat.controller;

import com.ptit.socialchat.entity.*;
import com.ptit.socialchat.service.GroupService;
import com.ptit.socialchat.service.UserService;
import com.ptit.socialchat.repository.GroupJoinRequestRepository;
import com.ptit.socialchat.repository.ConversationRepository;
import com.ptit.socialchat.repository.ConversationMemberRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/group")
public class GroupController {

    @Autowired
    private GroupService groupService;

    @Autowired
    private UserService userService;
    
    @Autowired
    private GroupJoinRequestRepository joinRequestRepository;
    
    @Autowired
    private ConversationRepository conversationRepository;
    
    @Autowired
    private ConversationMemberRepository memberRepository;

    @Autowired
    private org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate;

    @Autowired
    private com.ptit.socialchat.service.FileUploadService fileUploadService;

    @PostMapping("/create")
    public ResponseEntity<?> createGroup(@RequestBody Map<String, Object> request, Principal principal) {
        if (principal == null) return ResponseEntity.status(401).body("Unauthorized");
        User creator = userService.findByUsername(principal.getName()).orElseThrow();
        
        String name = (String) request.get("name");
        String description = (String) request.get("description");
        String category = (String) request.get("category");
        String privacy = (String) request.getOrDefault("privacy", "PRIVATE");
        List<String> usernames = (List<String>) request.get("usernames");
        
        if (name == null || name.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Tên nhóm không được để trống"));
        }
        
        Conversation conv = groupService.createCommunityGroup(name, description, category, privacy, usernames, creator);
        return ResponseEntity.ok(conv);
    }

    @GetMapping("/search")
    public ResponseEntity<?> searchPublicGroups(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String name,
            Principal principal) {
        List<Conversation> groups = groupService.searchPublicCommunities(category, name);
        
        User user = null;
        if (principal != null) {
            user = userService.findByUsername(principal.getName()).orElse(null);
        }
        
        final User currentUser = user;
        
        List<Map<String, Object>> result = groups.stream().map(g -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", g.getId());
            map.put("name", g.getName());
            map.put("description", g.getDescription());
            map.put("category", g.getCategory());
            map.put("privacy", g.getPrivacy());
            map.put("memberCount", g.getMembers().size());
            
            boolean isMember = false;
            boolean isPending = false;
            if (currentUser != null) {
                isMember = memberRepository.findByConversationAndUser(g, currentUser).isPresent();
                if (!isMember) {
                    isPending = joinRequestRepository.findByConversationIdAndUserId(g.getId(), currentUser.getId()).isPresent();
                }
            }
            map.put("isMember", isMember);
            map.put("isPending", isPending);
            return map;
        }).collect(Collectors.toList());
        
        return ResponseEntity.ok(result);
    }
    
    @PostMapping("/{id}/role")
    public ResponseEntity<?> changeRole(@PathVariable Long id, @RequestBody Map<String, String> request, Principal principal) {
        if (principal == null) return ResponseEntity.status(401).body("Unauthorized");
        User actor = userService.findByUsername(principal.getName()).orElseThrow();
        User targetUser = userService.findByUsername(request.get("targetUsername")).orElseThrow();
        String newRole = request.get("role"); // ADMIN, CO_ADMIN, MEMBER
        
        Conversation conv = conversationRepository.findById(id).orElseThrow();
        try {
            groupService.changeMemberRole(conv, targetUser, newRole, actor);
            return ResponseEntity.ok(Map.of("status", "ok"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    @PostMapping("/{id}/kick")
    public ResponseEntity<?> kickMember(@PathVariable Long id, @RequestBody Map<String, String> request, Principal principal) {
        if (principal == null) return ResponseEntity.status(401).body("Unauthorized");
        User actor = userService.findByUsername(principal.getName()).orElseThrow();
        User targetUser = userService.findByUsername(request.get("targetUsername")).orElseThrow();
        
        Conversation conv = conversationRepository.findById(id).orElseThrow();
        try {
            groupService.removeMember(conv, targetUser, actor);
            return ResponseEntity.ok(Map.of("status", "ok"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    @PostMapping("/{id}/join")
    public ResponseEntity<?> joinGroup(@PathVariable Long id, Principal principal) {
        if (principal == null) return ResponseEntity.status(401).body("Unauthorized");
        User user = userService.findByUsername(principal.getName()).orElseThrow();
        Conversation conv = conversationRepository.findById(id).orElseThrow();
        
        if (memberRepository.findByConversationAndUser(conv, user).isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Bạn đã là thành viên của nhóm này"));
        }
        
        if ("PUBLIC".equals(conv.getPrivacy())) {
            ConversationMember member = new ConversationMember();
            member.setConversation(conv);
            member.setUser(user);
            member.setRole("MEMBER");
            member.setJoinedAt(LocalDateTime.now());
            memberRepository.save(member);
            return ResponseEntity.ok(Map.of("status", "joined"));
        } else if ("REQUIRES_APPROVAL".equals(conv.getPrivacy())) {
            if (joinRequestRepository.findByConversationIdAndUserId(id, user.getId()).isPresent()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Bạn đã gửi yêu cầu tham gia rồi"));
            }
            GroupJoinRequest req = new GroupJoinRequest();
            req.setConversation(conv);
            req.setUser(user);
            joinRequestRepository.save(req);
            return ResponseEntity.ok(Map.of("status", "requested"));
        }
        
        return ResponseEntity.badRequest().body(Map.of("error", "Không thể tham gia nhóm riêng tư"));
    }
    
    @GetMapping("/{id}/requests")
    public ResponseEntity<?> getJoinRequests(@PathVariable Long id, Principal principal) {
        if (principal == null) return ResponseEntity.status(401).body("Unauthorized");
        User user = userService.findByUsername(principal.getName()).orElseThrow();
        Conversation conv = conversationRepository.findById(id).orElseThrow();
        
        ConversationMember member = memberRepository.findByConversationAndUser(conv, user).orElseThrow();
        if (!"ADMIN".equals(member.getRole()) && !"CO_ADMIN".equals(member.getRole())) {
            return ResponseEntity.status(403).body(Map.of("error", "Chỉ quản trị viên mới xem được yêu cầu"));
        }
        
        List<GroupJoinRequest> reqs = joinRequestRepository.findByConversationIdAndStatus(id, "PENDING");
        List<Map<String, Object>> result = reqs.stream().map(r -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", r.getId());
            Map<String, Object> userMap = new HashMap<>();
            userMap.put("username", r.getUser().getUsername());
            userMap.put("fullName", r.getUser().getFullName());
            userMap.put("avatar", r.getUser().getAvatar());
            map.put("user", userMap);
            map.put("createdAt", r.getCreatedAt());
            return map;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }
    
    @PostMapping("/{id}/requests/{reqId}")
    public ResponseEntity<?> processJoinRequest(@PathVariable Long id, @PathVariable Long reqId, @RequestBody Map<String, String> request, Principal principal) {
        if (principal == null) return ResponseEntity.status(401).body("Unauthorized");
        User user = userService.findByUsername(principal.getName()).orElseThrow();
        Conversation conv = conversationRepository.findById(id).orElseThrow();
        
        ConversationMember member = memberRepository.findByConversationAndUser(conv, user).orElseThrow();
        if (!"ADMIN".equals(member.getRole()) && !"CO_ADMIN".equals(member.getRole())) {
            return ResponseEntity.status(403).body(Map.of("error", "Chỉ quản trị viên mới được duyệt"));
        }
        
        GroupJoinRequest req = joinRequestRepository.findById(reqId).orElseThrow();
        String action = request.get("action"); // APPROVE or REJECT
        
        if ("APPROVE".equals(action)) {
            ConversationMember newMember = new ConversationMember();
            newMember.setConversation(conv);
            newMember.setUser(req.getUser());
            newMember.setRole("MEMBER");
            newMember.setJoinedAt(LocalDateTime.now());
            memberRepository.save(newMember);
            req.setStatus("APPROVED");
        } else {
            req.setStatus("REJECTED");
        }
        joinRequestRepository.save(req);
        
        return ResponseEntity.ok(Map.of("status", "ok"));
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<?> disbandGroup(@PathVariable Long id, Principal principal) {
        if (principal == null) return ResponseEntity.status(401).body("Unauthorized");
        User user = userService.findByUsername(principal.getName()).orElseThrow();
        Conversation conv = conversationRepository.findById(id).orElseThrow();
        
        ConversationMember member = memberRepository.findByConversationAndUser(conv, user).orElseThrow();
        if (!"ADMIN".equals(member.getRole())) {
            return ResponseEntity.status(403).body(Map.of("error", "Chỉ trưởng nhóm mới có quyền giải tán nhóm"));
        }
        
        groupService.disbandGroup(conv, user);
        return ResponseEntity.ok(Map.of("status", "ok"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getGroupDetails(@PathVariable Long id, Principal principal) {
        if (principal == null) return ResponseEntity.status(401).body("Unauthorized");
        User user = userService.findByUsername(principal.getName()).orElseThrow();
        Conversation conv = conversationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Nhóm không tồn tại"));
                
        if (memberRepository.findByConversationAndUser(conv, user).isEmpty()) {
            return ResponseEntity.status(403).body(Map.of("error", "Bạn không phải là thành viên của nhóm này"));
        }
        
        Map<String, Object> map = new HashMap<>();
        map.put("id", conv.getId());
        map.put("name", conv.getName());
        map.put("description", conv.getDescription());
        map.put("category", conv.getCategory());
        map.put("privacy", conv.getPrivacy());
        map.put("avatar", conv.getAvatar());
        map.put("memberCount", conv.getMembers().size());
        
        return ResponseEntity.ok(map);
    }

    @PostMapping("/{id}/rename")
    public ResponseEntity<?> renameGroup(@PathVariable Long id, @RequestBody Map<String, String> request, Principal principal) {
        if (principal == null) return ResponseEntity.status(401).body("Unauthorized");
        User actor = userService.findByUsername(principal.getName()).orElseThrow();
        String newName = request.get("name");
        
        if (newName == null || newName.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Tên nhóm không được để trống"));
        }
        
        Conversation conv = conversationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Nhóm không tồn tại"));
                
        ConversationMember member = memberRepository.findByConversationAndUser(conv, actor)
                .orElseThrow(() -> new IllegalArgumentException("Bạn không thuộc nhóm này"));
                
        if (!"ADMIN".equals(member.getRole()) && !"CO_ADMIN".equals(member.getRole())) {
            return ResponseEntity.status(403).body(Map.of("error", "Chỉ quản trị viên mới có quyền đổi tên nhóm"));
        }
        
        conv.setName(newName.trim());
        conversationRepository.save(conv);
        
        // Gửi tin nhắn hệ thống thông báo đổi tên
        groupService.sendSystemMessage(conv, actor, "đã đổi tên nhóm thành \"" + conv.getName() + "\"");
        
        // Phát tín hiệu đổi thông tin nhóm qua WebSocket
        com.ptit.socialchat.dto.MessageDTO wsMsg = new com.ptit.socialchat.dto.MessageDTO();
        wsMsg.setType("GROUP_UPDATED");
        wsMsg.setConversationId(conv.getId());
        wsMsg.setContent(conv.getName());
        wsMsg.setImageUrl(conv.getAvatar());
        messagingTemplate.convertAndSend("/topic/conversation/" + conv.getId(), wsMsg);
        
        return ResponseEntity.ok(Map.of("status", "ok", "name", conv.getName()));
    }

    @PostMapping("/{id}/avatar")
    public ResponseEntity<?> uploadGroupAvatar(
            @PathVariable Long id,
            @RequestParam("imageFile") MultipartFile imageFile,
            Principal principal) throws Exception {
            
        if (principal == null) return ResponseEntity.status(401).body("Unauthorized");
        User actor = userService.findByUsername(principal.getName()).orElseThrow();
        Conversation conv = conversationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Nhóm không tồn tại"));
                
        ConversationMember member = memberRepository.findByConversationAndUser(conv, actor)
                .orElseThrow(() -> new IllegalArgumentException("Bạn không thuộc nhóm này"));
                
        if (!"ADMIN".equals(member.getRole()) && !"CO_ADMIN".equals(member.getRole())) {
            return ResponseEntity.status(403).body(Map.of("error", "Chỉ quản trị viên mới được đổi ảnh nhóm"));
        }
        
        if (imageFile.getSize() > 5 * 1024 * 1024) {
            return ResponseEntity.badRequest().body(Map.of("error", "Kích thước ảnh đại diện vượt quá 5MB"));
        }
        
        String imageUrl = fileUploadService.saveFile(imageFile, "groups");
        conv.setAvatar(imageUrl);
        conversationRepository.save(conv);
        
        // Gửi tin nhắn hệ thống thông báo đổi ảnh nhóm
        groupService.sendSystemMessage(conv, actor, "đã thay đổi ảnh đại diện của nhóm");
        
        // Phát tín hiệu đổi thông tin nhóm qua WebSocket
        com.ptit.socialchat.dto.MessageDTO wsMsg = new com.ptit.socialchat.dto.MessageDTO();
        wsMsg.setType("GROUP_UPDATED");
        wsMsg.setConversationId(conv.getId());
        wsMsg.setContent(conv.getName());
        wsMsg.setImageUrl(imageUrl);
        messagingTemplate.convertAndSend("/topic/conversation/" + conv.getId(), wsMsg);
        
        return ResponseEntity.ok(Map.of("status", "ok", "avatar", imageUrl));
    }
}



