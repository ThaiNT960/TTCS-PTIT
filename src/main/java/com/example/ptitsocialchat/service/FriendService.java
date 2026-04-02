package com.example.ptitsocialchat.service;

import com.example.ptitsocialchat.entity.Friend;
import com.example.ptitsocialchat.entity.FriendRequest;
import com.example.ptitsocialchat.entity.User;
import com.example.ptitsocialchat.repository.FriendRepository;
import com.example.ptitsocialchat.repository.FriendRequestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class FriendService {
    @Autowired
    private FriendRepository friendRepository;
    @Autowired
    private FriendRequestRepository friendRequestRepository;

    public FriendRequest sendRequest(User sender, User receiver) {
        FriendRequest request = new FriendRequest();
        request.setSender(sender);
        request.setReceiver(receiver);
        request.setStatus("PENDING");
        request.setCreatedAt(LocalDateTime.now());
        return friendRequestRepository.save(request);
    }

    public void acceptRequest(Long requestId) {
        FriendRequest request = friendRequestRepository.findById(requestId).orElseThrow();
        request.setStatus("ACCEPTED");
        friendRequestRepository.save(request);

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
}
