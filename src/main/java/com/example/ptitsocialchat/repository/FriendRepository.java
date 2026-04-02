package com.example.ptitsocialchat.repository;

import com.example.ptitsocialchat.entity.Friend;
import com.example.ptitsocialchat.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface FriendRepository extends JpaRepository<Friend, Long> {
    List<Friend> findByUser(User user);
    Optional<Friend> findByUserAndFriend(User user, User friend);
}
