package com.example.ptitsocialchat.service;

import com.example.ptitsocialchat.entity.Announcement;
import com.example.ptitsocialchat.entity.User;
import com.example.ptitsocialchat.repository.AnnouncementRepository;
import com.example.ptitsocialchat.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AnnouncementService {

    @Autowired
    private AnnouncementRepository announcementRepository;

    @Autowired
    private UserRepository userRepository;

    public List<Announcement> findAll() {
        return announcementRepository.findAllByOrderByCreatedAtDesc();
    }

    public Announcement save(String title, String content, String adminUsername) {
        User admin = userRepository.findByUsername(adminUsername).orElse(null);
        if (admin == null || !"ROLE_ADMIN".equals(admin.getRole())) {
            throw new RuntimeException("Unauthorized or User not found");
        }

        Announcement ann = new Announcement();
        ann.setTitle(title);
        ann.setContent(content);
        ann.setAdmin(admin);
        return announcementRepository.save(ann);
    }

    public void deleteById(Long id, String adminUsername) {
        User admin = userRepository.findByUsername(adminUsername).orElse(null);
        if (admin == null || !"ROLE_ADMIN".equals(admin.getRole())) {
            throw new RuntimeException("Unauthorized or User not found");
        }
        announcementRepository.deleteById(id);
    }
}
