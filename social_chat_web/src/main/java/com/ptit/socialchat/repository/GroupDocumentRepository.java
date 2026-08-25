package com.ptit.socialchat.repository;

import com.ptit.socialchat.entity.GroupDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GroupDocumentRepository extends JpaRepository<GroupDocument, Long> {
    List<GroupDocument> findByConversationIdOrderByIsPinnedDescCreatedAtDesc(Long conversationId);
}



