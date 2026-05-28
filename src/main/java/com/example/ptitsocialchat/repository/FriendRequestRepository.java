package com.example.ptitsocialchat.repository;

import com.example.ptitsocialchat.entity.FriendRequest;
import com.example.ptitsocialchat.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface FriendRequestRepository extends JpaRepository<FriendRequest, Long> {
    List<FriendRequest> findByReceiverAndStatus(User receiver, String status);
    Optional<FriendRequest> findBySenderAndReceiverAndStatus(User sender, User receiver, String status);
    Optional<FriendRequest> findBySenderAndReceiver(User sender, User receiver);

    @Query("""
        SELECT fr.sender.id
        FROM FriendRequest fr
        WHERE fr.receiver = :user
          AND fr.status = 'PENDING'
    """)
    List<Long> findPendingSenderIds(@Param("user") User user);

    @Query("""
        SELECT fr.receiver.id
        FROM FriendRequest fr
        WHERE fr.sender = :user
          AND fr.status = 'PENDING'
    """)
    List<Long> findPendingReceiverIds(@Param("user") User user);
}
