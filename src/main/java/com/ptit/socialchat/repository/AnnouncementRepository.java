package com.ptit.socialchat.repository;

import com.ptit.socialchat.entity.Announcement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {
    List<Announcement> findAllByOrderByCreatedAtDesc();

    @org.springframework.data.jpa.repository.Query("SELECT a FROM Announcement a WHERE LOWER(a.title) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(a.content) LIKE LOWER(CONCAT('%', :search, '%')) ORDER BY a.createdAt DESC")
    List<Announcement> searchAnnouncements(@org.springframework.data.repository.query.Param("search") String search);
}



