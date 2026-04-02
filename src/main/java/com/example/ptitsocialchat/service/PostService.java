package com.example.ptitsocialchat.service;

import com.example.ptitsocialchat.dto.PostDTO;
import com.example.ptitsocialchat.entity.Comment;
import com.example.ptitsocialchat.entity.Post;
import com.example.ptitsocialchat.entity.PostLike;
import com.example.ptitsocialchat.entity.User;
import com.example.ptitsocialchat.repository.CommentRepository;
import com.example.ptitsocialchat.repository.PostLikeRepository;
import com.example.ptitsocialchat.repository.PostRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class PostService {
    @Autowired
    private PostRepository postRepository;
    @Autowired
    private CommentRepository commentRepository;
    @Autowired
    private PostLikeRepository postLikeRepository;

    public Post createPost(String content, String imageUrl, User user) {
        Post post = new Post();
        post.setContent(content);
        post.setImageUrl(imageUrl);
        post.setUser(user);
        post.setCreatedAt(LocalDateTime.now());
        return postRepository.save(post);
    }

    public List<PostDTO> getAllPosts() {
        return getAllPosts(null);
    }

    public List<PostDTO> getAllPosts(User currentUser) {
        return postRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(post -> convertToDTO(post, currentUser))
                .collect(Collectors.toList());
    }

    public Comment addComment(Long postId, String content, User user) {
        Post post = postRepository.findById(postId).orElseThrow();
        Comment comment = new Comment();
        comment.setContent(content);
        comment.setPost(post);
        comment.setUser(user);
        comment.setCreatedAt(LocalDateTime.now());
        return commentRepository.save(comment);
    }

    @Transactional
    public boolean toggleLike(Long postId, User user) {
        Post post = postRepository.findById(postId).orElseThrow();
        Optional<PostLike> existing = postLikeRepository.findByPostAndUser(post, user);
        if (existing.isPresent()) {
            postLikeRepository.deleteByPostAndUser(post, user);
            return false; // unliked
        } else {
            PostLike like = new PostLike();
            like.setPost(post);
            like.setUser(user);
            like.setCreatedAt(LocalDateTime.now());
            postLikeRepository.save(like);
            return true; // liked
        }
    }

    public void deletePost(Long postId, User user) {
        Post post = postRepository.findById(postId).orElseThrow();
        if (!post.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Bạn không có quyền xóa bài viết này");
        }
        postRepository.delete(post);
    }

    public long getLikeCount(Long postId) {
        Post post = postRepository.findById(postId).orElseThrow();
        return postLikeRepository.countByPost(post);
    }

    private PostDTO convertToDTO(Post post, User currentUser) {
        PostDTO dto = new PostDTO();
        dto.setId(post.getId());
        dto.setContent(post.getContent());
        dto.setImageUrl(post.getImageUrl());
        dto.setCreatedAt(post.getCreatedAt());
        dto.setUsername(post.getUser().getUsername());
        dto.setFullName(post.getUser().getFullName());
        dto.setAvatar(post.getUser().getAvatar());
        dto.setLikeCount((int) postLikeRepository.countByPost(post));
        if (currentUser != null) {
            dto.setLiked(postLikeRepository.findByPostAndUser(post, currentUser).isPresent());
        }
        dto.setComments(post.getComments().stream().map(c -> {
            PostDTO.CommentDTO cdto = new PostDTO.CommentDTO();
            cdto.setId(c.getId());
            cdto.setContent(c.getContent());
            cdto.setCreatedAt(c.getCreatedAt());
            cdto.setUsername(c.getUser().getUsername());
            cdto.setFullName(c.getUser().getFullName());
            return cdto;
        }).collect(Collectors.toList()));
        return dto;
    }
}
