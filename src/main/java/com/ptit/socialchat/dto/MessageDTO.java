package com.ptit.socialchat.dto;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MessageDTO {
    private Long id;
    private String content;
    private LocalDateTime timestamp;
    private String senderUsername;
    private String receiverUsername;
    private String imageUrl;
    private Boolean isRevoked;
    private String type; // Thêm type để phân loại event trong WebSocket (như REVOKE, UNFRIEND, NEW_MESSAGE)
    private Long conversationId;
}



