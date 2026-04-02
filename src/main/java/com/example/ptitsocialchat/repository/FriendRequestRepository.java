package com.example.ptitsocialchat.repository;

import com.example.ptitsocialchat.entity.FriendRequest;
import com.example.ptitsocialchat.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface FriendRequestRepository extends JpaRepository<FriendRequest, Long> {
    List<FriendRequest> findByReceiverAndStatus(User receiver, String status);
    Optional<FriendRequest> findBySenderAndReceiverAndStatus(User sender, User receiver, String status);
}
