package com.example.ptitsocialchat.repository;

import com.example.ptitsocialchat.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);

    @Query("SELECT u FROM User u WHERE LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<User> searchUsers(@Param("keyword") String keyword);

    @Query("""
        SELECT u
        FROM User u
        WHERE u.id NOT IN :excludedIds
          AND (
                (:education IS NOT NULL AND LOWER(u.education) = LOWER(:education))
             OR (:workplace IS NOT NULL AND LOWER(u.workplace) = LOWER(:workplace))
             OR (:location IS NOT NULL AND LOWER(u.location) = LOWER(:location))
          )
    """)
    List<User> findSimilarProfileUsers(
            @Param("education") String education,
            @Param("workplace") String workplace,
            @Param("location") String location,
            @Param("excludedIds") List<Long> excludedIds
    );
}
