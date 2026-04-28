package com.example.ptitsocialchat.repository;

import com.example.ptitsocialchat.entity.PostMedia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PostMediaRepository extends JpaRepository<PostMedia, Long> {
}
