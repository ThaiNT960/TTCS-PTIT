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
        // Delete all dependent entities to avoid ConstraintViolationException
        entityManager.createQuery("DELETE FROM CommentReaction cr WHERE cr.user.id = :uid").setParameter("uid", userId).executeUpdate();
        entityManager.createQuery("DELETE FROM PostLike pl WHERE pl.user.id = :uid").setParameter("uid", userId).executeUpdate();
        
        entityManager.createQuery("DELETE FROM CommentReaction cr WHERE cr.comment.id IN (SELECT c.id FROM Comment c WHERE c.user.id = :uid)").setParameter("uid", userId).executeUpdate();
        entityManager.createQuery("DELETE FROM Comment c WHERE c.parentCommentId IN (SELECT c2.id FROM Comment c2 WHERE c2.user.id = :uid)").setParameter("uid", userId).executeUpdate();
        entityManager.createQuery("DELETE FROM Comment c WHERE c.user.id = :uid").setParameter("uid", userId).executeUpdate();
        
        entityManager.createQuery("DELETE FROM CommentReaction cr WHERE cr.comment.id IN (SELECT c.id FROM Comment c WHERE c.post.id IN (SELECT p.id FROM Post p WHERE p.user.id = :uid))").setParameter("uid", userId).executeUpdate();
        entityManager.createQuery("DELETE FROM Comment c WHERE c.post.id IN (SELECT p.id FROM Post p WHERE p.user.id = :uid)").setParameter("uid", userId).executeUpdate();
        entityManager.createQuery("DELETE FROM PostLike pl WHERE pl.post.id IN (SELECT p.id FROM Post p WHERE p.user.id = :uid)").setParameter("uid", userId).executeUpdate();
        entityManager.createQuery("DELETE FROM Post p WHERE p.user.id = :uid").setParameter("uid", userId).executeUpdate();
        
        entityManager.createQuery("DELETE FROM Friend f WHERE f.user.id = :uid OR f.friend.id = :uid").setParameter("uid", userId).executeUpdate();
        entityManager.createQuery("DELETE FROM FriendRequest fr WHERE fr.sender.id = :uid OR fr.receiver.id = :uid").setParameter("uid", userId).executeUpdate();
        
        entityManager.createQuery("DELETE FROM Notification n WHERE n.user.id = :uid OR n.sender.id = :uid").setParameter("uid", userId).executeUpdate();
        
        entityManager.createQuery("DELETE FROM Message m WHERE m.sender.id = :uid OR m.receiver.id = :uid").setParameter("uid", userId).executeUpdate();
        entityManager.createQuery("DELETE FROM ConversationMember cm WHERE cm.user.id = :uid").setParameter("uid", userId).executeUpdate();
        
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
}



