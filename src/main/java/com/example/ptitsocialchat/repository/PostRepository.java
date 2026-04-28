package com.example.ptitsocialchat.repository;

import com.example.ptitsocialchat.entity.Post;
import com.example.ptitsocialchat.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PostRepository extends JpaRepository<Post, Long> {
    List<Post> findAllByOrderByCreatedAtDesc();
    
    List<Post> findByStatusOrderByCreatedAtDesc(String status);
    
    List<Post> findByStatusAndContentContainingIgnoreCaseOrderByCreatedAtDesc(String status, String content);

    List<Post> findByUserAndStatusOrderByCreatedAtDesc(User user, String status);

    List<Post> findByStatus(String status);

    long countByStatus(String status);
}
