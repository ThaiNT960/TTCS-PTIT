package com.ptit.socialchat.repository;

import com.ptit.socialchat.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    Optional<User> findByStudentId(String studentId);

    @Query("SELECT u FROM User u WHERE u.locked = false AND u.role = 'ROLE_USER' AND (LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<User> searchUsers(@Param("keyword") String keyword);

    @Query("SELECT u FROM User u WHERE (u.role IS NULL OR u.role <> 'ROLE_ADMIN') AND " +
           "(LOWER(u.fullName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(u.username) LIKE LOWER(CONCAT('%', :search, '%')))")
    List<User> searchUsersAdmin(@Param("search") String search);

    @Query("SELECT u FROM User u WHERE u.role IS NULL OR u.role <> 'ROLE_ADMIN'")
    List<User> findAllNonAdmin();

    @Query("SELECT u FROM User u WHERE u.locked = false AND u.role = 'ROLE_USER' AND u.id NOT IN :excludedIds AND " +
           "((:major IS NOT NULL AND LOWER(u.major) = LOWER(:major)) OR " +
           "(:campus IS NOT NULL AND LOWER(u.campus) = LOWER(:campus)) OR " +
           "(:batch IS NOT NULL AND LOWER(u.studentId) LIKE LOWER(CONCAT(:batch, '%'))))")
    List<User> findSimilarProfileUsers(@Param("major") String major, 
                                       @Param("campus") String campus, 
                                       @Param("batch") String batch, 
                                       @Param("excludedIds") List<Long> excludedIds);

}



