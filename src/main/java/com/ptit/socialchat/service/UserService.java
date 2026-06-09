package com.ptit.socialchat.service;

import com.ptit.socialchat.entity.User;
import com.ptit.socialchat.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private jakarta.persistence.EntityManager entityManager;

    @Autowired
    @org.springframework.context.annotation.Lazy
    private PasswordEncoder passwordEncoder;

    public User save(User user) {
        if (user.getPassword() != null && !user.getPassword().startsWith("$2a$")
                && !user.getPassword().startsWith("$2b$")) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }
        return userRepository.save(user);
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    public List<User> findAll() {
        return userRepository.findAll();
    }

    @org.springframework.transaction.annotation.Transactional
    public void deleteUserCompletely(Long userId) {
        entityManager.createQuery("DELETE FROM User u WHERE u.id = :uid").setParameter("uid", userId).executeUpdate();
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public Optional<User> findByStudentId(String studentId) {
        return userRepository.findByStudentId(studentId);
    }

    public List<User> searchUsersAdmin(String search) {
        return userRepository.searchUsersAdmin(search);
    }

    public List<User> findAllNonAdmin() {
        return userRepository.findAllNonAdmin();
    }

    public User updateProfile(String username, com.ptit.socialchat.dto.UpdateProfileRequest request) {
        User user = userRepository.findByUsername(username).orElseThrow();
        if (request.getFullName() != null)
            user.setFullName(request.getFullName());
        if (request.getEmail() != null) {
            String newEmail = request.getEmail().trim();
            if (!newEmail.isEmpty() && !newEmail.equalsIgnoreCase(user.getEmail())) {
                if (userRepository.findByEmail(newEmail).isPresent()) {
                    throw new IllegalArgumentException("Email đã được sử dụng bởi người dùng khác.");
                }
                user.setEmail(newEmail);
            }
        }
        if (request.getAvatar() != null)
            user.setAvatar(request.getAvatar());
        if (request.getCoverPhoto() != null)
            user.setCoverPhoto(request.getCoverPhoto());
        if (request.getBio() != null)
            user.setBio(request.getBio());
        if (request.getStudentId() != null)
            user.setStudentId(request.getStudentId());
        if (request.getMajor() != null)
            user.setMajor(request.getMajor());
        if (request.getCampus() != null)
            user.setCampus(request.getCampus());

        return userRepository.save(user);
    }

    public List<User> searchUsers(String keyword) {
        return userRepository.searchUsers(keyword);
    }

    public void changePassword(String username, String oldPassword, String newPassword) {
        User user = userRepository.findByUsername(username).orElseThrow();
        if (oldPassword == null || oldPassword.trim().isEmpty() || newPassword == null || newPassword.trim().isEmpty()) {
            throw new IllegalArgumentException("Mật khẩu không được để trống.");
        }
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new IllegalArgumentException("Mật khẩu hiện tại không chính xác.");
        }
        if (newPassword.trim().length() < 6) {
            throw new IllegalArgumentException("Mật khẩu mới phải có ít nhất 6 ký tự.");
        }
        user.setPassword(passwordEncoder.encode(newPassword.trim()));
        userRepository.save(user);
    }
}



