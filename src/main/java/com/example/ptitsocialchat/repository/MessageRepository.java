package com.example.ptitsocialchat.repository;

import com.example.ptitsocialchat.entity.Conversation;
import com.example.ptitsocialchat.entity.Message;
import com.example.ptitsocialchat.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {
    List<Message> findByConversationOrderByTimestampAsc(Conversation conversation);
}
