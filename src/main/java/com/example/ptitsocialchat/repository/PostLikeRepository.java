package com.example.ptitsocialchat.repository;

import com.example.ptitsocialchat.entity.Post;
import com.example.ptitsocialchat.entity.PostLike;
import com.example.ptitsocialchat.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PostLikeRepository extends JpaRepository<PostLike, Long> {
    Optional<PostLike> findByPostAndUser(Post post, User user);

    long countByPost(Post post);

    void deleteByPostAndUser(Post post, User user);

    List<PostLike> findByPost(Post post);

    @Query("SELECT l.post.id, COUNT(l) FROM PostLike l WHERE l.post IN :posts GROUP BY l.post.id")
    List<Object[]> countLikesForPosts(@Param("posts") List<Post> posts);

    @Query("SELECT l.post.id FROM PostLike l WHERE l.post IN :posts AND l.user = :user")
    List<Long> findLikedPostIdsByUser(@Param("posts") List<Post> posts, @Param("user") User user);

    @Query("""
        SELECT l2.user.id, COUNT(DISTINCT l2.post.id)
        FROM PostLike l1
        JOIN PostLike l2 ON l1.post.id = l2.post.id
        WHERE l1.user = :currentUser
          AND l2.user.id IN :candidateIds
          AND l2.user <> :currentUser
          AND l1.post.status = 'APPROVED'
        GROUP BY l2.user.id
    """)
    List<Object[]> countCommonLikedPosts(
            @Param("currentUser") User currentUser,
            @Param("candidateIds") List<Long> candidateIds
    );

    @Query("""
        SELECT DISTINCT l2.user
        FROM PostLike l1
        JOIN PostLike l2 ON l1.post.id = l2.post.id
        WHERE l1.user = :currentUser
          AND l2.user <> :currentUser
          AND l1.post.status = 'APPROVED'
    """)
    List<User> findUsersWithCommonLikes(@Param("currentUser") User currentUser);
}