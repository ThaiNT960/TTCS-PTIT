package com.ptit.socialchat.repository;

import com.ptit.socialchat.entity.Post;
import com.ptit.socialchat.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface PostRepository extends JpaRepository<Post, Long> {
    List<Post> findAllByOrderByCreatedAtDesc();
    
    List<Post> findByStatusOrderByCreatedAtDesc(String status);
    
    List<Post> findByStatusAndContentContainingIgnoreCaseOrderByCreatedAtDesc(String status, String content);

    List<Post> findByUserAndStatusOrderByCreatedAtDesc(User user, String status);

    List<Post> findByStatus(String status);

    long countByStatus(String status);

    @EntityGraph(attributePaths = {"user", "comments", "comments.user"})
    Page<Post> findByUserAndStatusOrderByCreatedAtDesc(User user, String status, Pageable pageable);

    @EntityGraph(attributePaths = {"user", "comments", "comments.user"})
    @Query("SELECT p FROM Post p WHERE p.status = :status AND p.user.locked = false AND LOWER(p.content) LIKE LOWER(CONCAT('%', :content, '%')) ORDER BY p.createdAt DESC")
    Page<Post> findByStatusAndContentContainingIgnoreCaseOrderByCreatedAtDesc(@Param("status") String status, @Param("content") String content, Pageable pageable);

    @EntityGraph(attributePaths = {"user", "comments", "comments.user"})
    @Query("SELECT p FROM Post p WHERE p.status = :status AND p.user.locked = false ORDER BY p.createdAt DESC")
    Page<Post> findByStatusOrderByCreatedAtDesc(@Param("status") String status, Pageable pageable);

    @Query("SELECT p FROM Post p WHERE LOWER(p.content) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(p.user.fullName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(p.user.username) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(p.user.email) LIKE LOWER(CONCAT('%', :search, '%')) ORDER BY p.createdAt DESC")
    List<Post> searchPostsAdmin(@Param("search") String search);
}



