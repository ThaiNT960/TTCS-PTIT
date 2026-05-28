package com.example.ptitsocialchat.repository;

import com.example.ptitsocialchat.entity.Friend;
import com.example.ptitsocialchat.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface FriendRepository extends JpaRepository<Friend, Long> {
    List<Friend> findByUser(User user);
    Optional<Friend> findByUserAndFriend(User user, User friend);
    boolean existsByUserAndFriend(User user, User friend);

    @Query("""
        SELECT f.friend.id
        FROM Friend f
        WHERE f.user = :user
    """)
    List<Long> findFriendIdsByUser(@Param("user") User user);

    @Query("""
        SELECT f2.friend, COUNT(f2.friend.id)
        FROM Friend f1
        JOIN Friend f2 ON f1.friend = f2.user
        WHERE f1.user = :user
          AND f2.friend <> :user
          AND f2.friend.id NOT IN :excludedIds
        GROUP BY f2.friend
        ORDER BY COUNT(f2.friend.id) DESC
    """)
    List<Object[]> findSuggestionsByMutualFriends(
            @Param("user") User user,
            @Param("excludedIds") List<Long> excludedIds
    );
}
