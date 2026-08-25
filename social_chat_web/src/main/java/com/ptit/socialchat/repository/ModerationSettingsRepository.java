package com.ptit.socialchat.repository;

import com.ptit.socialchat.entity.ModerationSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ModerationSettingsRepository extends JpaRepository<ModerationSettings, Long> {
}



