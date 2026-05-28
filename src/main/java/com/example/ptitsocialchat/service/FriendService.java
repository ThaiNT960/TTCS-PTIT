package com.example.ptitsocialchat.service;

import com.example.ptitsocialchat.entity.Friend;
import com.example.ptitsocialchat.entity.FriendRequest;
import com.example.ptitsocialchat.entity.User;
import com.example.ptitsocialchat.repository.FriendRepository;
import com.example.ptitsocialchat.repository.FriendRequestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import java.util.HashMap;
import java.util.Map;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.AccessDeniedException;

@Service
public class FriendService {
    @Autowired
    private FriendRepository friendRepository;
    @Autowired
    private FriendRequestRepository friendRequestRepository;
    @Autowired
    private NotificationService notificationService;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Transactional
    public FriendRequest sendRequest(User sender, User receiver) {
        if (sender.getId().equals(receiver.getId())) {
            throw new IllegalArgumentException("Không thể gửi yêu cầu kết bạn cho chính mình");
        }
        if (friendRepository.findByUserAndFriend(sender, receiver).isPresent()
                || friendRepository.findByUserAndFriend(receiver, sender).isPresent()) {
            throw new IllegalArgumentException("Hai người đã là bạn bè");
        }
        if (friendRequestRepository.findBySenderAndReceiverAndStatus(sender, receiver, "PENDING").isPresent()) {
            throw new IllegalArgumentException("Bạn đã gửi yêu cầu kết bạn cho người này rồi");
        }
        if (friendRequestRepository.findBySenderAndReceiverAndStatus(receiver, sender, "PENDING").isPresent()) {
            throw new IllegalArgumentException("Người này đã gửi yêu cầu kết bạn cho bạn rồi");
        }

        java.util.Optional<FriendRequest> existingRequest = friendRequestRepository.findBySenderAndReceiverAndStatus(sender, receiver, "REJECTED");
        if (existingRequest.isEmpty()) {
            existingRequest = friendRequestRepository.findBySenderAndReceiverAndStatus(sender, receiver, "ACCEPTED");
        }

        FriendRequest request;
        if (existingRequest.isPresent()) {
            request = existingRequest.get();
            request.setStatus("PENDING");
            request.setCreatedAt(LocalDateTime.now());
        } else {
            request = new FriendRequest();
            request.setSender(sender);
            request.setReceiver(receiver);
            request.setStatus("PENDING");
            request.setCreatedAt(LocalDateTime.now());
        }
        
        FriendRequest savedRequest = friendRequestRepository.save(request);
        notificationService.createNotification(receiver, sender, com.example.ptitsocialchat.enums.NotificationType.FRIEND_REQUEST, "friend.html");
        return savedRequest;
    }

    @Transactional
    public void acceptRequest(Long requestId, User currentUser) {
        FriendRequest request = friendRequestRepository.findById(requestId).orElseThrow();
        if (!request.getReceiver().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("Bạn không có quyền thao tác trên lời mời này");
        }
        if (!"PENDING".equals(request.getStatus())) {
            throw new IllegalArgumentException("Yêu cầu này không còn hiệu lực");
        }

        User sender = request.getSender();
        User receiver = request.getReceiver();

        if (friendRepository.findByUserAndFriend(sender, receiver).isEmpty()) {
            Friend f1 = new Friend();
            f1.setUser(sender);
            f1.setFriend(receiver);
            f1.setCreatedAt(LocalDateTime.now());
            friendRepository.save(f1);
        }

        if (friendRepository.findByUserAndFriend(receiver, sender).isEmpty()) {
            Friend f2 = new Friend();
            f2.setUser(receiver);
            f2.setFriend(sender);
            f2.setCreatedAt(LocalDateTime.now());
            friendRepository.save(f2);
        }

        request.setStatus("ACCEPTED");
        friendRequestRepository.save(request);

        notificationService.createNotification(sender, receiver, com.example.ptitsocialchat.enums.NotificationType.FRIEND_ACCEPT, "profile.html?username=" + receiver.getUsername());
    }

    public List<User> getFriends(User user) {
        return friendRepository.findByUser(user).stream()
                .map(Friend::getFriend)
                .collect(Collectors.toList());
    }

    public List<FriendRequest> getPendingRequests(User user) {
        return friendRequestRepository.findByReceiverAndStatus(user, "PENDING");
    }

    @Transactional
    public void rejectRequest(Long requestId, User currentUser) {
        FriendRequest request = friendRequestRepository.findById(requestId).orElseThrow();
        if (!request.getReceiver().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("Bạn không có quyền thao tác trên lời mời này");
        }
        if (!"PENDING".equals(request.getStatus())) {
            throw new IllegalArgumentException("Yêu cầu này không còn hiệu lực");
        }
        request.setStatus("REJECTED");
        friendRequestRepository.save(request);
    }

    @Transactional
    public void unfriend(User current, User target) {
        friendRepository.findByUserAndFriend(current, target).ifPresent(friendRepository::delete);
        friendRepository.findByUserAndFriend(target, current).ifPresent(friendRepository::delete);

        // Notify target user
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "UNFRIENDED");
        payload.put("partnerUsername", current.getUsername());
        messagingTemplate.convertAndSend("/topic/messages/" + target.getUsername(), payload);

        // Notify current user
        Map<String, Object> payloadSelf = new HashMap<>();
        payloadSelf.put("type", "UNFRIENDED_SELF");
        payloadSelf.put("partnerUsername", target.getUsername());
        messagingTemplate.convertAndSend("/topic/messages/" + current.getUsername(), payloadSelf);
    }

    public String getFriendshipStatus(User viewer, User target) {
        if (friendRepository.findByUserAndFriend(viewer, target).isPresent()) {
            return "FRIEND";
        }
        if (friendRequestRepository.findBySenderAndReceiverAndStatus(viewer, target, "PENDING").isPresent()) {
            return "REQUEST_SENT";
        }
        if (friendRequestRepository.findBySenderAndReceiverAndStatus(target, viewer, "PENDING").isPresent()) {
            return "REQUEST_RECEIVED";
        }
        return "NONE";
    }
}
