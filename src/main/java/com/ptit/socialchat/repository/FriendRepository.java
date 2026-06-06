package com.ptit.socialchat.repository;

import com.ptit.socialchat.entity.Friend;
import com.ptit.socialchat.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FriendRepository extends JpaRepository<Friend, Long> {
    List<Friend> findByUser(User user);
    Optional<Friend> findByUserAndFriend(User user, User friend);

    @Query("SELECT f.friend.id FROM Friend f WHERE f.user = :user")
    List<Long> findFriendIdsByUser(@Param("user") com.ptit.socialchat.entity.User user);

    @Query("SELECT f2.friend, COUNT(f2.friend.id) FROM Friend f1 JOIN Friend f2 ON f1.friend = f2.user WHERE f1.user = :user AND f2.friend <> :user AND f2.friend.id NOT IN :excludedIds GROUP BY f2.friend ORDER BY COUNT(f2.friend.id) DESC")
    List<Object[]> findSuggestionsByMutualFriends(@Param("user") com.ptit.socialchat.entity.User user, @Param("excludedIds") List<Long> excludedIds);

}



