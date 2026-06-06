package com.ptit.socialchat.repository;

import com.ptit.socialchat.entity.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {
    
    @Query("SELECT c FROM Conversation c WHERE c.privacy IN ('PUBLIC', 'REQUIRES_APPROVAL') AND (:category IS NULL OR :category = '' OR c.category = :category) AND (:name IS NULL OR :name = '' OR LOWER(c.name) LIKE LOWER(CONCAT('%', :name, '%')))")
    List<Conversation> searchPublicCommunities(@Param("category") String category, @Param("name") String name);
}



