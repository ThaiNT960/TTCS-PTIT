package com.ptit.socialchat.service;

import com.ptit.socialchat.entity.Friend;
import com.ptit.socialchat.entity.FriendRequest;
import com.ptit.socialchat.entity.User;
import com.ptit.socialchat.repository.FriendRepository;
import com.ptit.socialchat.repository.FriendRequestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
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
        if (sender.getId().equals(receiver.getId())) {
            throw new IllegalArgumentException("Bạn không thể gửi lời mời kết bạn cho chính mình.");
        }

        // 1. Kiểm tra xem đã là bạn bè chưa
        if (friendRepository.findByUserAndFriend(sender, receiver).isPresent()) {
            throw new IllegalArgumentException("Hai người đã là bạn bè từ trước.");
        }

        // 2. Kiểm tra xem đối phương đã gửi yêu cầu cho mình trước đó và đang PENDING hay không
        Optional<FriendRequest> incomingRequest = friendRequestRepository
                .findBySenderAndReceiverAndStatus(receiver, sender, "PENDING");
        if (incomingRequest.isPresent()) {
            throw new IllegalArgumentException("Người dùng này đã gửi lời mời kết bạn cho bạn. Vui lòng chấp nhận lời mời từ họ.");
        }

        // 3. Kiểm tra yêu cầu gửi đi từ sender -> receiver cũ
        Optional<FriendRequest> existingRequestOpt = friendRequestRepository.findBySenderAndReceiver(sender, receiver);
        FriendRequest request;
        if (existingRequestOpt.isPresent()) {
            request = existingRequestOpt.get();
            if ("PENDING".equals(request.getStatus())) {
                throw new IllegalArgumentException("Yêu cầu kết bạn đã được gửi và đang chờ phản hồi.");
            }
            if ("ACCEPTED".equals(request.getStatus())) {
                throw new IllegalArgumentException("Hai người đã là bạn bè từ trước.");
            }
            // Nếu là REJECTED, cập nhật lại trạng thái thành PENDING để gửi lại lời mời
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
        notificationService.createNotification(receiver, sender, com.ptit.socialchat.enums.NotificationType.FRIEND_REQUEST, "friend.html");
        return savedRequest;
    }

    public void acceptRequest(Long requestId, User currentUser) {
        FriendRequest request = friendRequestRepository.findById(requestId).orElseThrow();
        if (!request.getReceiver().getId().equals(currentUser.getId())) {
            throw new org.springframework.security.access.AccessDeniedException("Bạn không có quyền chấp nhận yêu cầu này.");
        }
        if (!"PENDING".equals(request.getStatus())) {
            throw new IllegalArgumentException("Yêu cầu kết bạn này đã được xử lý từ trước.");
        }
        request.setStatus("ACCEPTED");
        friendRequestRepository.save(request);

        notificationService.createNotification(request.getSender(), request.getReceiver(), com.ptit.socialchat.enums.NotificationType.FRIEND_ACCEPT, "profile.html?username=" + request.getReceiver().getUsername());

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
                .filter(f -> !f.isLocked())
                .collect(Collectors.toList());
    }

    public List<FriendRequest> getPendingRequests(User user) {
        return friendRequestRepository.findByReceiverAndStatus(user, "PENDING");
    }

    public void rejectRequest(Long requestId, User currentUser) {
        FriendRequest request = friendRequestRepository.findById(requestId).orElseThrow();
        if (!request.getReceiver().getId().equals(currentUser.getId())) {
            throw new org.springframework.security.access.AccessDeniedException("Bạn không có quyền từ chối yêu cầu này.");
        }
        if (!"PENDING".equals(request.getStatus())) {
            throw new IllegalArgumentException("Yêu cầu kết bạn này đã được xử lý từ trước.");
        }
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



