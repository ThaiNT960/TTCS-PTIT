package com.ptit.socialchat.repository;

import com.ptit.socialchat.entity.GroupJoinRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GroupJoinRequestRepository extends JpaRepository<GroupJoinRequest, Long> {
    List<GroupJoinRequest> findByConversationIdAndStatus(Long conversationId, String status);
    Optional<GroupJoinRequest> findByConversationIdAndUserId(Long conversationId, Long userId);
}



