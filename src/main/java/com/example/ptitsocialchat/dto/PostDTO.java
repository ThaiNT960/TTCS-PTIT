package com.example.ptitsocialchat.dto;

import java.time.LocalDateTime;
import java.util.List;

public class PostDTO {
    private Long id;
    private String content;
    private String imageUrl;
    private LocalDateTime createdAt;
    private String username;
    private String userFullName;
    private String userAvatar;
    private List<CommentDTO> comments;
    private String currentReaction;
    private java.util.Map<String, Long> reactionCounts;
    private String status;
    private String moderationLabel;
    private Double moderationConfidence;

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getUserFullName() { return userFullName; }
    public void setUserFullName(String userFullName) { this.userFullName = userFullName; }

    public String getUserAvatar() { return userAvatar; }
    public void setUserAvatar(String userAvatar) { this.userAvatar = userAvatar; }

    public List<CommentDTO> getComments() { return comments; }
    public void setComments(List<CommentDTO> comments) { this.comments = comments; }

    public String getCurrentReaction() { return currentReaction; }
    public void setCurrentReaction(String currentReaction) { this.currentReaction = currentReaction; }

    public java.util.Map<String, Long> getReactionCounts() { return reactionCounts; }
    public void setReactionCounts(java.util.Map<String, Long> reactionCounts) { this.reactionCounts = reactionCounts; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getModerationLabel() { return moderationLabel; }
    public void setModerationLabel(String moderationLabel) { this.moderationLabel = moderationLabel; }

    public Double getModerationConfidence() { return moderationConfidence; }
    public void setModerationConfidence(Double moderationConfidence) { this.moderationConfidence = moderationConfidence; }

    public static class CommentDTO {
        private Long id;
        private String content;
        private LocalDateTime createdAt;
        private String username;
        private String fullName;
        private Long parentCommentId;
        private int likeCount;
        private String currentReaction;

        // Getters and Setters
        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public LocalDateTime getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getFullName() {
            return fullName;
        }

        public void setFullName(String fullName) {
            this.fullName = fullName;
        }

        public Long getParentCommentId() { return parentCommentId; }
        public void setParentCommentId(Long parentCommentId) { this.parentCommentId = parentCommentId; }

        public int getLikeCount() { return likeCount; }
        public void setLikeCount(int likeCount) { this.likeCount = likeCount; }

        public String getCurrentReaction() { return currentReaction; }
        public void setCurrentReaction(String currentReaction) { this.currentReaction = currentReaction; }
    }

    public static class ReactionUserDTO {
    private String username;
    private String fullName;
    private String avatar;
    private String reactionType;

    public ReactionUserDTO() {
    }

    public ReactionUserDTO(String username, String fullName, String avatar, String reactionType) {
        this.username = username;
        this.fullName = fullName;
        this.avatar = avatar;
        this.reactionType = reactionType;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public String getReactionType() {
        return reactionType;
    }

    public void setReactionType(String reactionType) {
        this.reactionType = reactionType;
    }
}
}
