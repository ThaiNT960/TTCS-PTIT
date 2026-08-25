package com.ptit.socialchat.dto;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FriendRequestDTO {
    private Long id;
    private String senderUsername;
    private String senderFullName;
    private String status;
    private LocalDateTime createdAt;
}



