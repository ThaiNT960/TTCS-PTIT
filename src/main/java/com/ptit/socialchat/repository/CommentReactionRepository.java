package com.ptit.socialchat.repository;

import com.ptit.socialchat.entity.Comment;
import com.ptit.socialchat.entity.CommentReaction;
import com.ptit.socialchat.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CommentReactionRepository extends JpaRepository<CommentReaction, Long> {
    Optional<CommentReaction> findByCommentAndUser(Comment comment, User user);
    void deleteByCommentAndUser(Comment comment, User user);
    long countByComment(Comment comment);
}



