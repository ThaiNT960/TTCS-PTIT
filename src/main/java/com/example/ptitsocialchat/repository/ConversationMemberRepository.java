package com.example.ptitsocialchat.repository;

import com.example.ptitsocialchat.entity.Conversation;
import com.example.ptitsocialchat.entity.ConversationMember;
import com.example.ptitsocialchat.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ConversationMemberRepository extends JpaRepository<ConversationMember, Long> {
    List<ConversationMember> findByUser(User user);
    List<ConversationMember> findByConversation(Conversation conversation);
    java.util.Optional<ConversationMember> findByConversationAndUser(Conversation conversation, User user);
}
