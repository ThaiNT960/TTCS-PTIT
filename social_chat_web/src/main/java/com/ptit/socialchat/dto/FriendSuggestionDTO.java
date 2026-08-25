package com.ptit.socialchat.dto;

import java.util.List;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
public class FriendSuggestionDTO {
    private String username;
    private String fullName;
    private String avatar;

    private int mutualFriendCount;
    private int commonLikedPosts;
    private int commonCommentedPosts;

    private int score;
    private List<String> reasons;
}



