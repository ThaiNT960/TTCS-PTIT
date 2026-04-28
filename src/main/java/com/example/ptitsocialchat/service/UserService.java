package com.example.ptitsocialchat.service;

import com.example.ptitsocialchat.entity.User;
import com.example.ptitsocialchat.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;

    public User save(User user) {
        return userRepository.save(user);
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public List<User> findAll() {
        return userRepository.findAll();
    }

    public void deleteById(Long id) {
        userRepository.deleteById(id);
    }

    public User updateProfile(String username, com.example.ptitsocialchat.dto.UpdateProfileRequest request) {
        User user = userRepository.findByUsername(username).orElseThrow();
        if (request.getFullName() != null) user.setFullName(request.getFullName());
        if (request.getAvatar() != null) user.setAvatar(request.getAvatar());
        if (request.getCoverPhoto() != null) user.setCoverPhoto(request.getCoverPhoto());
        if (request.getBio() != null) user.setBio(request.getBio());
        if (request.getWorkplace() != null) user.setWorkplace(request.getWorkplace());
        if (request.getEducation() != null) user.setEducation(request.getEducation());
        if (request.getLocation() != null) user.setLocation(request.getLocation());
        if (request.getPrivacySetting() != null) {
            user.setPrivacySetting(com.example.ptitsocialchat.enums.PrivacySetting.valueOf(request.getPrivacySetting()));
        }
        return userRepository.save(user);
    }
}
