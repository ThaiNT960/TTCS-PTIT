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

    public FriendRequest sendRequest(User sender, User receiver) {
        FriendRequest request = new FriendRequest();
        request.setSender(sender);
        request.setReceiver(receiver);
        request.setStatus("PENDING");
        request.setCreatedAt(LocalDateTime.now());
        FriendRequest savedRequest = friendRequestRepository.save(request);
        notificationService.createNotification(receiver, sender, com.example.ptitsocialchat.enums.NotificationType.FRIEND_REQUEST, "friend.html");
        return savedRequest;
    }

    public void acceptRequest(Long requestId) {
        FriendRequest request = friendRequestRepository.findById(requestId).orElseThrow();
        request.setStatus("ACCEPTED");
        friendRequestRepository.save(request);

        notificationService.createNotification(request.getSender(), request.getReceiver(), com.example.ptitsocialchat.enums.NotificationType.FRIEND_ACCEPT, "profile.html?username=" + request.getReceiver().getUsername());

        Friend f1 = new Friend();
        f1.setUser(request.getSender());
        f1.setFriend(request.getReceiver());
        f1.setCreatedAt(LocalDateTime.now());
        friendRepository.save(f1);

        Friend f2 = new Friend();
        f2.setUser(request.getReceiver());
        f2.setFriend(request.getSender());
        f2.setCreatedAt(LocalDateTime.now());
        friendRepository.save(f2);
    }

    public List<User> getFriends(User user) {
        return friendRepository.findByUser(user).stream()
                .map(Friend::getFriend)
                .collect(Collectors.toList());
    }

    public List<FriendRequest> getPendingRequests(User user) {
        return friendRequestRepository.findByReceiverAndStatus(user, "PENDING");
    }

    public void rejectRequest(Long requestId) {
        FriendRequest request = friendRequestRepository.findById(requestId).orElseThrow();
        request.setStatus("REJECTED");
        friendRequestRepository.save(request);
    }

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
