package com.example.ptitsocialchat.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class PostDTO {
    private Long id;
    private String content;
    private String imageUrl;
    private String videoUrl;
    private String checkInLocation;
    private String privacy;
    private LocalDateTime createdAt;
    private String username;
    private String fullName;
    private String avatar;
    private List<CommentDTO> comments;
    private int likeCount;
    private boolean liked;
    private String currentReaction;
    private Map<String, Long> reactionCounts;
    private List<String> mediaUrls;
    private String status;
    private String moderationLabel;
    private Double moderationConfidence;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public String getVideoUrl() { return videoUrl; }
    public void setVideoUrl(String videoUrl) { this.videoUrl = videoUrl; }
    public String getCheckInLocation() { return checkInLocation; }
    public void setCheckInLocation(String checkInLocation) { this.checkInLocation = checkInLocation; }
    public String getPrivacy() { return privacy; }
    public void setPrivacy(String privacy) { this.privacy = privacy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getAvatar() { return avatar; }
    public void setAvatar(String avatar) { this.avatar = avatar; }
    public List<CommentDTO> getComments() { return comments; }
    public void setComments(List<CommentDTO> comments) { this.comments = comments; }
    public int getLikeCount() { return likeCount; }
    public void setLikeCount(int likeCount) { this.likeCount = likeCount; }
    public boolean isLiked() { return liked; }
    public void setLiked(boolean liked) { this.liked = liked; }
    public String getCurrentReaction() { return currentReaction; }
    public void setCurrentReaction(String currentReaction) { this.currentReaction = currentReaction; }
    public Map<String, Long> getReactionCounts() { return reactionCounts; }
    public void setReactionCounts(Map<String, Long> reactionCounts) { this.reactionCounts = reactionCounts; }
    public List<String> getMediaUrls() { return mediaUrls; }
    public void setMediaUrls(List<String> mediaUrls) { this.mediaUrls = mediaUrls; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getModerationLabel() { return moderationLabel; }
    public void setModerationLabel(String moderationLabel) { this.moderationLabel = moderationLabel; }
    public Double getModerationConfidence() { return moderationConfidence; }
    public void setModerationConfidence(Double moderationConfidence) { this.moderationConfidence = moderationConfidence; }

    public static class ReactionUserDTO {
        private String username;
        private String fullName;
        private String avatar;
        private String reactionType;

        // Getters and Setters
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getFullName() { return fullName; }
        public void setFullName(String fullName) { this.fullName = fullName; }
        public String getAvatar() { return avatar; }
        public void setAvatar(String avatar) { this.avatar = avatar; }
        public String getReactionType() { return reactionType; }
        public void setReactionType(String reactionType) { this.reactionType = reactionType; }
    }

    public static class CommentDTO {
        private Long id;
        private String content;
        private String imageUrl;
        private LocalDateTime createdAt;
        private String username;
        private String fullName;
        private Long parentCommentId;
        private int likeCount;
        private String currentReaction;
        private Map<String, Long> reactionCounts;

        // Getters and Setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        public String getImageUrl() { return imageUrl; }
        public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getFullName() { return fullName; }
        public void setFullName(String fullName) { this.fullName = fullName; }
        public Long getParentCommentId() { return parentCommentId; }
        public void setParentCommentId(Long parentCommentId) { this.parentCommentId = parentCommentId; }
        public int getLikeCount() { return likeCount; }
        public void setLikeCount(int likeCount) { this.likeCount = likeCount; }
        public String getCurrentReaction() { return currentReaction; }
        public void setCurrentReaction(String currentReaction) { this.currentReaction = currentReaction; }
        public Map<String, Long> getReactionCounts() { return reactionCounts; }
        public void setReactionCounts(Map<String, Long> reactionCounts) { this.reactionCounts = reactionCounts; }
    }
}
