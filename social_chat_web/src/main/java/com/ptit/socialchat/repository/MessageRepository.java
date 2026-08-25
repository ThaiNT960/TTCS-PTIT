package com.ptit.socialchat.repository;

import com.ptit.socialchat.entity.Message;
import com.ptit.socialchat.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {
    @Query("SELECT m FROM Message m WHERE " +
           "((m.sender = ?1 AND m.receiver = ?2 AND (m.deletedBySender IS NULL OR m.deletedBySender = false)) OR " +
           "(m.sender = ?2 AND m.receiver = ?1 AND (m.deletedByReceiver IS NULL OR m.deletedByReceiver = false))) " +
           "ORDER BY m.timestamp ASC")
    List<Message> findChatHistory(User currentUser, User otherUser);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.transaction.annotation.Transactional
    @Query("UPDATE Message m SET m.deletedBySender = true WHERE m.sender = ?1 AND m.receiver = ?2")
    void clearChatHistoryAsSender(User sender, User receiver);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.transaction.annotation.Transactional
    @Query("UPDATE Message m SET m.deletedByReceiver = true WHERE m.receiver = ?1 AND m.sender = ?2")
    void clearChatHistoryAsReceiver(User receiver, User sender);

    @Query("SELECT DISTINCT m.receiver FROM Message m WHERE m.sender = ?1 AND (m.deletedBySender IS NULL OR m.deletedBySender = false)")
    List<User> findReceiversByUser(User user);

    @Query("SELECT DISTINCT m.sender FROM Message m WHERE m.receiver = ?1 AND (m.deletedByReceiver IS NULL OR m.deletedByReceiver = false)")
    List<User> findSendersByUser(User user);

    @Query("SELECT m FROM Message m WHERE " +
           "((m.sender = ?1 AND m.receiver = ?2 AND (m.deletedBySender IS NULL OR m.deletedBySender = false)) OR " +
           "(m.sender = ?2 AND m.receiver = ?1 AND (m.deletedByReceiver IS NULL OR m.deletedByReceiver = false))) " +
           "ORDER BY m.timestamp DESC")
    List<Message> findLatestMessageBetween(User u1, User u2, org.springframework.data.domain.Pageable pageable);

    List<Message> findByConversationOrderByTimestampAsc(com.ptit.socialchat.entity.Conversation conversation);

    java.util.Optional<Message> findFirstByConversationOrderByTimestampDesc(com.ptit.socialchat.entity.Conversation conversation);
}



