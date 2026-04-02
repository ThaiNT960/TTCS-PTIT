package com.example.ptitsocialchat.repository;

import com.example.ptitsocialchat.entity.Post;
import com.example.ptitsocialchat.entity.PostLike;
import com.example.ptitsocialchat.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PostLikeRepository extends JpaRepository<PostLike, Long> {
    Optional<PostLike> findByPostAndUser(Post post, User user);

    long countByPost(Post post);

    void deleteByPostAndUser(Post post, User user);
}
