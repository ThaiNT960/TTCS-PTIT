package com.ptit.socialchat.repository;

import com.ptit.socialchat.entity.FriendRequest;
import com.ptit.socialchat.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FriendRequestRepository extends JpaRepository<FriendRequest, Long> {
    @Query("SELECT fr FROM FriendRequest fr WHERE fr.receiver = :receiver AND fr.status = :status AND fr.sender.locked = false")
    List<FriendRequest> findByReceiverAndStatus(@Param("receiver") User receiver, @Param("status") String status);
    Optional<FriendRequest> findBySenderAndReceiverAndStatus(User sender, User receiver, String status);
    Optional<FriendRequest> findBySenderAndReceiver(User sender, User receiver);

    @Query("SELECT fr.sender.id FROM FriendRequest fr WHERE fr.receiver = :user AND fr.status = 'PENDING'")
    List<Long> findPendingSenderIds(@Param("user") com.ptit.socialchat.entity.User user);

    @Query("SELECT fr.receiver.id FROM FriendRequest fr WHERE fr.sender = :user AND fr.status = 'PENDING'")
    List<Long> findPendingReceiverIds(@Param("user") com.ptit.socialchat.entity.User user);

}



