package com.ptit.socialchat.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Entity
@Table(name = "group_documents")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GroupDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "file_url", nullable = false, length = 500)
    private String fileUrl;

    @Column(name = "file_type", length = 255)
    private String fileType;

    @Builder.Default
    @Column(length = 50)
    private String category = "Khác";

    @Builder.Default
    @Column(name = "size_bytes")
    private Long sizeBytes = 0L;

    @Builder.Default
    private Integer downloads = 0;

    @Builder.Default
    @Column(name = "is_pinned")
    private Boolean isPinned = false;

    @ManyToOne
    @JoinColumn(name = "uploader_id", nullable = false)
    private User uploader;

    @ManyToOne
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;

    @Builder.Default
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    // Custom getter/setter to preserve compatibility with existing controller and service code
    public Boolean getPinned() {
        return isPinned;
    }

    public void setPinned(Boolean pinned) {
        this.isPinned = pinned;
    }
}



