package com.example.ptitsocialchat.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "messages", indexes = {
    @Index(name = "idx_message_sender_id", columnList = "sender_id"),
    @Index(name = "idx_message_receiver_id", columnList = "receiver_id"),
    @Index(name = "idx_message_conversation_id", columnList = "conversation_id"),
    @Index(name = "idx_message_timestamp", columnList = "timestamp")
})
public class Message {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "TEXT")
    private String content;

    private LocalDateTime timestamp;

    @ManyToOne
    @JoinColumn(name = "sender_id")
    private User sender;

    @ManyToOne
    @JoinColumn(name = "receiver_id")
    private User receiver;

    @ManyToOne
    @JoinColumn(name = "conversation_id")
    private Conversation conversation;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "is_revoked")
    private Boolean isRevoked = false;

    @Column(name = "deleted_by_sender")
    private Boolean deletedBySender = false;

    @Column(name = "deleted_by_receiver")
    private Boolean deletedByReceiver = false;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    public User getSender() { return sender; }
    public void setSender(User sender) { this.sender = sender; }
    public User getReceiver() { return receiver; }
    public void setReceiver(User receiver) { this.receiver = receiver; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    
    public Boolean getIsRevoked() { return isRevoked; }
    public void setIsRevoked(Boolean isRevoked) { this.isRevoked = isRevoked; }

    public Boolean getDeletedBySender() { return deletedBySender; }
    public void setDeletedBySender(Boolean deletedBySender) { this.deletedBySender = deletedBySender; }

    public Boolean getDeletedByReceiver() { return deletedByReceiver; }
    public void setDeletedByReceiver(Boolean deletedByReceiver) { this.deletedByReceiver = deletedByReceiver; }

    public Conversation getConversation() { return conversation; }
    public void setConversation(Conversation conversation) { this.conversation = conversation; }
}
