package com.example.ptitsocialchat.repository;

import com.example.ptitsocialchat.entity.Message;
import com.example.ptitsocialchat.entity.User;
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

    List<Message> findByConversationOrderByTimestampAsc(com.example.ptitsocialchat.entity.Conversation conversation);

    @Query("SELECT m FROM Message m WHERE m.id IN (SELECT MAX(m2.id) FROM Message m2 WHERE m2.conversation IN :conversations GROUP BY m2.conversation.id)")
    List<Message> findLastMessagesForConversations(@org.springframework.data.repository.query.Param("conversations") List<com.example.ptitsocialchat.entity.Conversation> conversations);
}
