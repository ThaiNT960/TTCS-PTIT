package com.ptit.socialchat.repository;

import com.ptit.socialchat.entity.Post;
import com.ptit.socialchat.entity.PostLike;
import com.ptit.socialchat.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface PostLikeRepository extends JpaRepository<PostLike, Long> {
    Optional<PostLike> findByPostAndUser(Post post, User user);

    long countByPost(Post post);

    void deleteByPostAndUser(Post post, User user);

    @Query("SELECT l2.user.id, COUNT(DISTINCT l2.post.id) FROM PostLike l1 JOIN PostLike l2 ON l1.post.id = l2.post.id WHERE l1.user = :currentUser AND l2.user.id IN :candidateIds AND l2.user <> :currentUser AND l1.post.status = 'APPROVED' GROUP BY l2.user.id")
    List<Object[]> countCommonLikedPosts(@Param("currentUser") com.ptit.socialchat.entity.User currentUser, @Param("candidateIds") List<Long> candidateIds);

    @Query("SELECT DISTINCT l2.user FROM PostLike l1 JOIN PostLike l2 ON l1.post.id = l2.post.id WHERE l1.user = :currentUser AND l2.user <> :currentUser AND l1.post.status = 'APPROVED'")
    List<com.ptit.socialchat.entity.User> findUsersWithCommonLikes(@Param("currentUser") com.ptit.socialchat.entity.User currentUser);

}



