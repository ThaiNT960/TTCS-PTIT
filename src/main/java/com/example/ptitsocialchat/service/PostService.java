package com.example.ptitsocialchat.service;

import com.example.ptitsocialchat.dto.PostDTO;
import com.example.ptitsocialchat.entity.*;
import com.example.ptitsocialchat.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
    @Autowired
    private FriendRepository friendRepository;
    @Autowired
    private PostMediaRepository postMediaRepository;
    @Autowired
    private NotificationService notificationService;
    @Autowired
    private CommentReactionRepository commentReactionRepository;

    public Post createPost(String content, List<String> mediaUrls, User user) {
        Post post = new Post();
        post.setContent(content);
        post.setUser(user);
        post.setCreatedAt(LocalDateTime.now());
        Post savedPost = postRepository.save(post);

        if (mediaUrls != null && !mediaUrls.isEmpty()) {
            for (String url : mediaUrls) {
                PostMedia media = new PostMedia();
                media.setPost(savedPost);
                media.setMediaUrl(url);
                if (url.toLowerCase().endsWith(".mp4") || url.toLowerCase().endsWith(".webm")) {
                    media.setMediaType(com.example.ptitsocialchat.enums.MediaType.VIDEO);
                } else {
                    media.setMediaType(com.example.ptitsocialchat.enums.MediaType.IMAGE);
                }
                postMediaRepository.save(media);
            }
        }
        return savedPost;
    }

    public List<PostDTO> getAllPosts() {
        return getAllPosts(null);
    }

    public List<PostDTO> getAllPosts(User currentUser) {
        if (currentUser == null) {
            return postRepository.findAllByOrderByCreatedAtDesc().stream()
                    .map(post -> convertToDTO(post, null))
                    .collect(Collectors.toList());
        }
        List<User> usersToFetch = friendRepository.findByUser(currentUser).stream()
                .map(friend -> friend.getFriend())
                .collect(Collectors.toList());
        usersToFetch.add(currentUser);
        return postRepository.findByUserInOrderByCreatedAtDesc(usersToFetch).stream()
                .map(post -> convertToDTO(post, currentUser))
                .collect(Collectors.toList());
    }

    public List<PostDTO> getUserPosts(User targetUser, User viewer) {
        return postRepository.findByUserOrderByCreatedAtDesc(targetUser).stream()
                .map(post -> convertToDTO(post, viewer))
                .collect(Collectors.toList());
    }

    public List<PostDTO> searchPosts(String keyword, User currentUser) {
        return postRepository.findByContentContainingIgnoreCaseOrderByCreatedAtDesc(keyword).stream()
                .map(post -> convertToDTO(post, currentUser))
                .collect(Collectors.toList());
    }

    public Comment addComment(Long postId, String content, Long parentCommentId, User user) {
        Post post = postRepository.findById(postId).orElseThrow();
        Comment comment = new Comment();
        comment.setContent(content);
        comment.setPost(post);
        comment.setUser(user);
        comment.setParentCommentId(parentCommentId);
        comment.setCreatedAt(LocalDateTime.now());
        Comment saved = commentRepository.save(comment);

        // Thông báo cho chủ bài viết
        if (!post.getUser().getId().equals(user.getId())) {
            notificationService.createNotification(post.getUser(), user, com.example.ptitsocialchat.enums.NotificationType.COMMENT_POST, "/profile.html?username=" + post.getUser().getUsername());
        }

        // Nếu là trả lời bình luận, thông báo cho người viết bình luận gốc
        if (parentCommentId != null) {
            commentRepository.findById(parentCommentId).ifPresent(parentComment -> {
                if (!parentComment.getUser().getId().equals(user.getId()) && !parentComment.getUser().getId().equals(post.getUser().getId())) {
                    notificationService.createNotification(parentComment.getUser(), user, com.example.ptitsocialchat.enums.NotificationType.COMMENT_POST, "/profile.html?username=" + post.getUser().getUsername());
                }
            });
        }

        return saved;
    }

    @Transactional
    public boolean reactToPost(Long postId, User user, String reactionType) {
        Post post = postRepository.findById(postId).orElseThrow();
        Optional<PostLike> existing = postLikeRepository.findByPostAndUser(post, user);
        
        boolean isNewReaction = false;
        if (reactionType == null || reactionType.trim().isEmpty() || reactionType.equalsIgnoreCase("NONE")) {
            existing.ifPresent(postLikeRepository::delete);
            return false;
        }

        if (existing.isPresent()) {
            PostLike like = existing.get();
            if (reactionType.equals(like.getReactionType())) {
                postLikeRepository.delete(like);
                return false;
            } else {
                like.setReactionType(reactionType);
                postLikeRepository.save(like);
                isNewReaction = true;
            }
        } else {
            PostLike like = new PostLike();
            like.setPost(post);
            like.setUser(user);
            like.setReactionType(reactionType);
            like.setCreatedAt(LocalDateTime.now());
            postLikeRepository.save(like);
            isNewReaction = true;
        }

        if (isNewReaction) {
            notificationService.createNotification(post.getUser(), user, com.example.ptitsocialchat.enums.NotificationType.LIKE_POST, "/profile.html?username=" + post.getUser().getUsername());
        }
        return isNewReaction;
    }

    @Transactional
    public boolean reactToComment(Long commentId, User user, String reactionType) {
        Comment comment = commentRepository.findById(commentId).orElseThrow();
        Optional<CommentReaction> existing = commentReactionRepository.findByCommentAndUser(comment, user);
        
        boolean isNewReaction = false;
        if (reactionType == null || reactionType.trim().isEmpty() || reactionType.equalsIgnoreCase("NONE")) {
            existing.ifPresent(commentReactionRepository::delete);
            return false;
        }

        if (existing.isPresent()) {
            CommentReaction reaction = existing.get();
            if (reactionType.equals(reaction.getReactionType().name())) {
                commentReactionRepository.delete(reaction);
                return false;
            } else {
                reaction.setReactionType(com.example.ptitsocialchat.enums.ReactionType.valueOf(reactionType));
                commentReactionRepository.save(reaction);
                isNewReaction = true;
            }
        } else {
            CommentReaction reaction = new CommentReaction();
            reaction.setComment(comment);
            reaction.setUser(user);
            reaction.setReactionType(com.example.ptitsocialchat.enums.ReactionType.valueOf(reactionType));
            commentReactionRepository.save(reaction);
            isNewReaction = true;
        }

        // Notify comment owner
        if (isNewReaction && !comment.getUser().getId().equals(user.getId())) {
            notificationService.createNotification(comment.getUser(), user, com.example.ptitsocialchat.enums.NotificationType.LIKE_POST, "/profile.html?username=" + comment.getPost().getUser().getUsername());
        }
        return isNewReaction;
    }

    public void deletePost(Long postId, User user) {
        Post post = postRepository.findById(postId).orElseThrow();
        if (!post.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Bạn không có quyền xóa bài viết này");
        }
        postRepository.delete(post);
    }

    public List<Map<String, Object>> getPostReactions(Long postId) {
        Post post = postRepository.findById(postId).orElseThrow();
        return post.getLikes().stream().map(like -> {
            Map<String, Object> reactionData = new java.util.HashMap<>();
            reactionData.put("reactionType", like.getReactionType() != null ? like.getReactionType() : "LIKE");
            reactionData.put("username", like.getUser().getUsername());
            reactionData.put("fullName", like.getUser().getFullName());
            reactionData.put("avatar", like.getUser().getAvatar());
            return reactionData;
        }).collect(Collectors.toList());
    }

    private PostDTO convertToDTO(Post post, User currentUser) {
        PostDTO dto = new PostDTO();
        dto.setId(post.getId());
        dto.setContent(post.getContent());
        dto.setCreatedAt(post.getCreatedAt());
        dto.setUsername(post.getUser().getUsername());
        dto.setFullName(post.getUser().getFullName());
        dto.setAvatar(post.getUser().getAvatar());
        dto.setVideoUrl(post.getVideoUrl());
        dto.setCheckInLocation(post.getCheckInLocation());
        dto.setPrivacy(post.getPrivacy() != null ? post.getPrivacy().name() : "PUBLIC");
        
        if (post.getMedia() != null) {
            dto.setMediaUrls(post.getMedia().stream()
                .map(PostMedia::getMediaUrl)
                .collect(Collectors.toList()));
        } else {
            dto.setMediaUrls(new ArrayList<>());
        }

        Map<String, Long> reactionCounts = post.getLikes().stream()
            .collect(Collectors.groupingBy(l -> l.getReactionType() != null ? l.getReactionType() : "LIKE", Collectors.counting()));
        dto.setReactionCounts(reactionCounts);
        dto.setLikeCount(post.getLikes().size());

        if (currentUser != null) {
            Optional<PostLike> currentLike = postLikeRepository.findByPostAndUser(post, currentUser);
            dto.setLiked(currentLike.isPresent());
            dto.setCurrentReaction(currentLike.map(l -> l.getReactionType() != null ? l.getReactionType() : "LIKE").orElse(null));
        }
        dto.setComments(post.getComments().stream().map(c -> {
            PostDTO.CommentDTO cdto = new PostDTO.CommentDTO();
            cdto.setId(c.getId());
            cdto.setContent(c.getContent());
            cdto.setCreatedAt(c.getCreatedAt());
            cdto.setUsername(c.getUser().getUsername());
            cdto.setFullName(c.getUser().getFullName());
            cdto.setParentCommentId(c.getParentCommentId());
            cdto.setImageUrl(c.getImageUrl());
            
            if (c.getReactions() != null) {
                Map<String, Long> cReactionCounts = c.getReactions().stream()
                    .collect(Collectors.groupingBy(r -> r.getReactionType().name(), Collectors.counting()));
                cdto.setReactionCounts(cReactionCounts);
                cdto.setLikeCount(c.getReactions().size());

                if (currentUser != null) {
                    Optional<CommentReaction> currentCReac = c.getReactions().stream()
                        .filter(r -> r.getUser().getId().equals(currentUser.getId())).findFirst();
                    cdto.setCurrentReaction(currentCReac.map(r -> r.getReactionType().name()).orElse(null));
                }
            } else {
                cdto.setReactionCounts(new java.util.HashMap<>());
                cdto.setLikeCount(0);
            }
            
            return cdto;
        }).collect(Collectors.toList()));
        return dto;
    }
}
