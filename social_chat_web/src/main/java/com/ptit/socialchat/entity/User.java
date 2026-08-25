package com.ptit.socialchat.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Entity
@Table(name = "users")
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @JsonIgnore
    @Column(nullable = false)
    private String password;

    @Column(unique = true)
    private String email;

    private String fullName;
    private String avatar;
    private String coverPhoto;
    private String bio;

    @Column(name = "workplace")
    private String studentId;

    @Column(name = "education")
    private String major;

    @Column(name = "location")
    private String campus;

    private String role; // ROLE_USER, ROLE_ADMIN

    @Builder.Default
    private boolean locked = false;
}
