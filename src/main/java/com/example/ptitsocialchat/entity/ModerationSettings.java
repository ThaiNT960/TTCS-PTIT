package com.example.ptitsocialchat.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "moderation_settings")
public class ModerationSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String mode = "NONE"; // NONE, MANUAL, AUTO_AI
    
    @Column(name = "ai_service_url")
    private String aiServiceUrl = "http://localhost:8000";

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }

    public String getAiServiceUrl() { return aiServiceUrl; }
    public void setAiServiceUrl(String aiServiceUrl) { this.aiServiceUrl = aiServiceUrl; }
}
