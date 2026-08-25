package com.ptit.socialchat.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterUserRequest {
    private String email;
    private String password;
    private String fullName;
    private String studentId;
    private String major;
}



