package com.ptit.socialchat.entity;

import jakarta.persistence.*;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Entity
@Table(name = "moderation_settings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ModerationSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Builder.Default
    @Column(nullable = false)
    private String mode = "NONE"; // NONE, MANUAL, AUTO_AI
    
    @Builder.Default
    @Column(name = "ai_service_url")
    private String aiServiceUrl = "http://localhost:8000"; // Sẽ được Admin thiết lập hoặc lấy từ mặc định
}



