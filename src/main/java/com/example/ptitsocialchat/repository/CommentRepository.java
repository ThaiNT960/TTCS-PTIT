package com.example.ptitsocialchat.repository;

import com.example.ptitsocialchat.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.example.ptitsocialchat.entity.User;
import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findByPostIdOrderByCreatedAtAsc(Long postId);

    @Query("""
        SELECT c2.user.id, COUNT(DISTINCT c2.post.id)
        FROM Comment c1
        JOIN Comment c2 ON c1.post.id = c2.post.id
        WHERE c1.user = :currentUser
          AND c2.user.id IN :candidateIds
          AND c2.user <> :currentUser
          AND c1.post.status = 'APPROVED'
        GROUP BY c2.user.id
    """)
    List<Object[]> countCommonCommentedPosts(
            @Param("currentUser") User currentUser,
            @Param("candidateIds") List<Long> candidateIds
    );

    @Query("""
        SELECT DISTINCT c2.user
        FROM Comment c1
        JOIN Comment c2 ON c1.post.id = c2.post.id
        WHERE c1.user = :currentUser
          AND c2.user <> :currentUser
          AND c1.post.status = 'APPROVED'
    """)
    List<User> findUsersWithCommonComments(@Param("currentUser") User currentUser);
}
