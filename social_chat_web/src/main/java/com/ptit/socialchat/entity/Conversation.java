package com.ptit.socialchat.entity;

import jakarta.persistence.*;
import java.util.List;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Entity
@Table(name = "conversations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Conversation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name; // Null for 1-1 chats
    private boolean isGroupChat;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String category;

    @Builder.Default
    private String privacy = "PRIVATE"; // PRIVATE, PUBLIC, REQUIRES_APPROVAL

    @OneToMany(mappedBy = "conversation", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ConversationMember> members;

    @OneToMany(mappedBy = "conversation", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Message> messages;

    @Column(length = 500)
    private String avatar;
}



