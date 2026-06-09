package com.ptit.socialchat.controller;

import com.ptit.socialchat.entity.Announcement;
import com.ptit.socialchat.service.AnnouncementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/announcements")
public class AnnouncementController {

    @Autowired
    private AnnouncementService announcementService;

    @GetMapping
    public List<Announcement> getAnnouncements(@RequestParam(required = false) String search) {
        if (search != null && !search.trim().isEmpty()) {
            return announcementService.search(search.trim());
        }
        return announcementService.findAll();
    }


}



