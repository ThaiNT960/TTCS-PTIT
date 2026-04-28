package com.example.ptitsocialchat.repository;

import com.example.ptitsocialchat.entity.ModerationSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ModerationSettingsRepository extends JpaRepository<ModerationSettings, Long> {
}
