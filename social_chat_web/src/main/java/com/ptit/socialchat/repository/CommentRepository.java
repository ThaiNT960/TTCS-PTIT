package com.ptit.socialchat.repository;

import com.ptit.socialchat.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findByPostIdOrderByCreatedAtAsc(Long postId);
    List<Comment> findByParentCommentId(Long parentCommentId);

    @Query("SELECT c2.user.id, COUNT(DISTINCT c2.post.id) FROM Comment c1 JOIN Comment c2 ON c1.post.id = c2.post.id WHERE c1.user = :currentUser AND c2.user.id IN :candidateIds AND c2.user <> :currentUser AND c1.post.status = 'APPROVED' GROUP BY c2.user.id")
    List<Object[]> countCommonCommentedPosts(@Param("currentUser") com.ptit.socialchat.entity.User currentUser, @Param("candidateIds") List<Long> candidateIds);

    @Query("SELECT DISTINCT c2.user FROM Comment c1 JOIN Comment c2 ON c1.post.id = c2.post.id WHERE c1.user = :currentUser AND c2.user <> :currentUser AND c1.post.status = 'APPROVED'")
    List<com.ptit.socialchat.entity.User> findUsersWithCommonComments(@Param("currentUser") com.ptit.socialchat.entity.User currentUser);

}



