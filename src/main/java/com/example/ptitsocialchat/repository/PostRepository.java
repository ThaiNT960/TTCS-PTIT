package com.example.ptitsocialchat.repository;

import com.example.ptitsocialchat.entity.Post;
import com.example.ptitsocialchat.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PostRepository extends JpaRepository<Post, Long> {
    List<Post> findAllByOrderByCreatedAtDesc();
    List<Post> findByUserInOrderByCreatedAtDesc(List<User> users);
    List<Post> findByUserOrderByCreatedAtDesc(User user);
    List<Post> findByContentContainingIgnoreCaseOrderByCreatedAtDesc(String keyword);
}
