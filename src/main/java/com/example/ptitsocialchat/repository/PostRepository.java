package com.example.ptitsocialchat.repository;

import com.example.ptitsocialchat.entity.Post;
import com.example.ptitsocialchat.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.List;

public interface PostRepository extends JpaRepository<Post, Long> {

    @EntityGraph(attributePaths = {"user", "comments", "comments.user"})
    Page<Post> findByUserAndStatusOrderByCreatedAtDesc(User user, String status, Pageable pageable);

    long countByStatus(String status);

    @EntityGraph(attributePaths = {"user", "comments", "comments.user"})
    Page<Post> findByStatusAndContentContainingIgnoreCaseOrderByCreatedAtDesc(String status, String content, Pageable pageable);

    @EntityGraph(attributePaths = {"user", "comments", "comments.user"})
    Page<Post> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);

    @EntityGraph(attributePaths = {"user", "comments", "comments.user"})
    List<Post> findByStatus(String status);
}