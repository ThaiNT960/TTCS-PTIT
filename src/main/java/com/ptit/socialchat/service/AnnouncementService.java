package com.ptit.socialchat.service;

import com.ptit.socialchat.entity.Announcement;
import com.ptit.socialchat.entity.User;
import com.ptit.socialchat.repository.AnnouncementRepository;
import com.ptit.socialchat.repository.UserRepository;
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

    public List<Announcement> search(String search) {
        return announcementRepository.searchAnnouncements(search);
    }

    public Announcement save(String title, String content, String adminUsername) {
        User admin = userRepository.findByUsername(adminUsername)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản quản trị viên."));

        Announcement ann = new Announcement();
        ann.setTitle(title);
        ann.setContent(content);
        ann.setAdmin(admin);
        return announcementRepository.save(ann);
    }

    public void deleteById(Long id) {
        announcementRepository.deleteById(id);
    }
}



