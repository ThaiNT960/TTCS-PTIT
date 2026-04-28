package com.example.ptitsocialchat.repository;

import com.example.ptitsocialchat.entity.Comment;
import com.example.ptitsocialchat.entity.CommentReaction;
import com.example.ptitsocialchat.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CommentReactionRepository extends JpaRepository<CommentReaction, Long> {
    Optional<CommentReaction> findByCommentAndUser(Comment comment, User user);
    void deleteByCommentAndUser(Comment comment, User user);
    long countByComment(Comment comment);
}
