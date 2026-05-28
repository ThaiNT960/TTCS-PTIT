package com.example.ptitsocialchat.dto;

import java.util.List;

public class FriendSuggestionDTO {
    private String username;
    private String fullName;
    private String avatar;

    private int mutualFriendCount;
    private int commonLikedPosts;
    private int commonCommentedPosts;

    private int score;
    private List<String> reasons;

    public FriendSuggestionDTO() {}

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getAvatar() { return avatar; }
    public void setAvatar(String avatar) { this.avatar = avatar; }

    public int getMutualFriendCount() { return mutualFriendCount; }
    public void setMutualFriendCount(int mutualFriendCount) { this.mutualFriendCount = mutualFriendCount; }

    public int getCommonLikedPosts() { return commonLikedPosts; }
    public void setCommonLikedPosts(int commonLikedPosts) { this.commonLikedPosts = commonLikedPosts; }

    public int getCommonCommentedPosts() { return commonCommentedPosts; }
    public void setCommonCommentedPosts(int commonCommentedPosts) { this.commonCommentedPosts = commonCommentedPosts; }

    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }

    public List<String> getReasons() { return reasons; }
    public void setReasons(List<String> reasons) { this.reasons = reasons; }
}
