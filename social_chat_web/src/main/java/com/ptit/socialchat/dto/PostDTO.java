package com.ptit.socialchat.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PostDTO {
    private Long id;
    private String content;
    private String imageUrl;
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
    private String status;
    private String moderationLabel;
    private Double moderationConfidence;

    @Getter
    @Setter
    public static class ReactionUserDTO {
        private String username;
        private String fullName;
        private String avatar;
        private String reactionType;
    }

    @Getter
    @Setter
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
    }
}



