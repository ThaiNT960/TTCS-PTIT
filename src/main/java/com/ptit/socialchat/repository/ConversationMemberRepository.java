package com.ptit.socialchat.repository;

import com.ptit.socialchat.entity.Conversation;
import com.ptit.socialchat.entity.ConversationMember;
import com.ptit.socialchat.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ConversationMemberRepository extends JpaRepository<ConversationMember, Long> {
    List<ConversationMember> findByUser(User user);
    List<ConversationMember> findByConversation(Conversation conversation);
    java.util.Optional<ConversationMember> findByConversationAndUser(Conversation conversation, User user);
}



