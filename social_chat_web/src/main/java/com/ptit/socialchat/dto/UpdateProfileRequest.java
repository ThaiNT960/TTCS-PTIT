package com.ptit.socialchat.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateProfileRequest {
    private String fullName;
    private String email;
    private String bio;
    private String studentId;
    private String major;
    private String campus;
    private String avatar;
    private String coverPhoto;
}



