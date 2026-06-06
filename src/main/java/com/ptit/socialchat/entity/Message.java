package com.ptit.socialchat.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Entity
@Table(name = "messages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
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

    @Builder.Default
    @Column(name = "is_revoked")
    private Boolean isRevoked = false;

    @Builder.Default
    @Column(name = "deleted_by_sender")
    private Boolean deletedBySender = false;

    @Builder.Default
    @Column(name = "deleted_by_receiver")
    private Boolean deletedByReceiver = false;
}



