package com.example.ptitsocialchat.service;

import com.example.ptitsocialchat.dto.PostDTO;
import com.example.ptitsocialchat.entity.Comment;
import com.example.ptitsocialchat.entity.Post;
import com.example.ptitsocialchat.entity.PostLike;
import com.example.ptitsocialchat.entity.User;
import com.example.ptitsocialchat.repository.CommentRepository;
import com.example.ptitsocialchat.repository.PostLikeRepository;
import com.example.ptitsocialchat.repository.PostRepository;
import com.example.ptitsocialchat.repository.CommentReactionRepository;
import com.example.ptitsocialchat.repository.ModerationSettingsRepository;
import com.example.ptitsocialchat.repository.FriendRepository;
import com.example.ptitsocialchat.entity.CommentReaction;
import com.example.ptitsocialchat.entity.ModerationSettings;
import com.example.ptitsocialchat.enums.ReactionType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.HashMap;
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
    private CommentReactionRepository commentReactionRepository;
    @Autowired
    private ModerationService moderationService;
    @Autowired
    private ModerationSettingsRepository moderationSettingsRepository;
    @Autowired
    private FriendRepository friendRepository;

    public static class CreatePostResult {
        private String status;
        private String message;
        private String label;

        public CreatePostResult(String status, String message, String label) {
            this.status = status;
            this.message = message;
            this.label = label;
        }

        public String getStatus() { return status; }
        public String getMessage() { return message; }
        public String getLabel() { return label; }
    }

    public CreatePostResult createPost(String content, String imageUrl, User user) {
        Post post = new Post();
        post.setContent(content);
        post.setImageUrl(imageUrl);
        post.setUser(user);
        post.setCreatedAt(LocalDateTime.now());

        String mode = "MANUAL";
        String aiServiceUrl = "http://localhost:8000";
        var settingsList = moderationSettingsRepository.findAll();
        if (!settingsList.isEmpty()) {
            mode = settingsList.get(0).getMode();
            aiServiceUrl = settingsList.get(0).getAiServiceUrl();
        }

        if ("NONE".equals(mode)) {
            post.setStatus("APPROVED");
            postRepository.save(post);
            return new CreatePostResult("APPROVED", "Đăng bài thành công!", null);

        } else if ("MANUAL".equals(mode)) {
            post.setStatus("PENDING");
            postRepository.save(post);
            return new CreatePostResult("PENDING", "Bài viết đang chờ Admin kiểm duyệt trước khi hiển thị.", null);

        } else if ("AUTO_AI".equals(mode)) {
            if (content == null || content.trim().isEmpty()) {
                post.setStatus("APPROVED");
                postRepository.save(post);
                return new CreatePostResult("APPROVED", "Đăng bài thành công!", null);
            }
            try {
                com.example.ptitsocialchat.service.ModerationService.ModerationResult aiResult = moderationService.moderate(content, aiServiceUrl);

                if (!aiResult.isSuccess()) {
                    System.err.println("[PostService] AI service failed: " + aiResult.getErrorMessage());
                    post.setStatus("PENDING");
                    postRepository.save(post);
                    return new CreatePostResult("PENDING", "AI Moderation tạm thời không khả dụng. Bài viết đang chờ kiểm duyệt thủ công.", null);
                }

                post.setModerationLabel(aiResult.getLabel());
                post.setModerationConfidence(aiResult.getConfidence());

                if (aiResult.isToxic() || "OFFENSIVE".equalsIgnoreCase(aiResult.getLabel()) || "HATE".equalsIgnoreCase(aiResult.getLabel())) {
                    post.setStatus("REJECTED");
                    postRepository.save(post);
                    String labelVi = "OFFENSIVE".equals(aiResult.getLabel()) ? "Xúc phạm" : "Thù ghét";
                    return new CreatePostResult("REJECTED", "Bài viết bị từ chối tự động. Nội dung được phát hiện: " + labelVi + " (độ tin cậy: " + Math.round(aiResult.getConfidence() * 100) + "%).", aiResult.getLabel());
                } else {
                    post.setStatus("APPROVED");
                    postRepository.save(post);
                    return new CreatePostResult("APPROVED", "Đăng bài thành công!", aiResult.getLabel());
                }
            } catch (Exception e) {
                post.setStatus("PENDING");
                postRepository.save(post);
                return new CreatePostResult("PENDING", "Lỗi gọi AI Moderation. Bài viết đang chờ kiểm duyệt thủ công.", null);
            }
        }

        post.setStatus("PENDING");
        postRepository.save(post);
        return new CreatePostResult("PENDING", "Bài viết đang chờ kiểm duyệt.", null);
    }

    public Map<String, Object> getAllPosts() {
        return getAllPosts(null, null, 0, 10);
    }

    public Comment addComment(Long postId, String content, User user, Long parentId) {
        Post post = postRepository.findById(postId).orElseThrow();
        if (!canViewPost(post, user)) {
            throw new RuntimeException("Bạn không có quyền truy cập bài viết này");
        }
        Comment comment = new Comment();
        comment.setContent(content);
        comment.setPost(post);
        comment.setUser(user);
        comment.setParentCommentId(parentId);
        comment.setCreatedAt(LocalDateTime.now());
        return commentRepository.save(comment);
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

    private List<PostDTO> convertToDTOs(List<Post> posts, User currentUser) {
        if (posts == null || posts.isEmpty()) return new java.util.ArrayList<>();
        
        java.util.Map<Long, Long> likeCounts = postLikeRepository.countLikesForPosts(posts).stream()
                .collect(Collectors.toMap(arr -> (Long) arr[0], arr -> (Long) arr[1]));

        java.util.Set<Long> likedPostIds = currentUser != null ?
                new java.util.HashSet<>(postLikeRepository.findLikedPostIdsByUser(posts, currentUser)) :
                new java.util.HashSet<>();

        return posts.stream().map(post -> {
            PostDTO dto = new PostDTO();
            dto.setId(post.getId());
            dto.setContent(post.getContent());
            dto.setImageUrl(post.getImageUrl());
            dto.setCreatedAt(post.getCreatedAt());
            dto.setUsername(post.getUser().getUsername());
            dto.setUserFullName(post.getUser().getFullName());
            dto.setUserAvatar(post.getUser().getAvatar());
            dto.setStatus(post.getStatus());
            dto.setModerationLabel(post.getModerationLabel());
            dto.setModerationConfidence(post.getModerationConfidence());

            // Set reaction counts
            java.util.Map<String, Long> reactionMap = new java.util.HashMap<>();
            List<PostLike> likesForPost = postLikeRepository.findByPost(post);
            for (PostLike l : likesForPost) {
                String type = l.getReactionType() != null ? l.getReactionType().toUpperCase() : "LIKE";
                reactionMap.put(type, reactionMap.getOrDefault(type, 0L) + 1);
                if (currentUser != null && l.getUser().getId().equals(currentUser.getId())) {
                    dto.setCurrentReaction(type);
                }
            }
            dto.setReactionCounts(reactionMap);

            dto.setComments(post.getComments().stream().map(c -> {
                PostDTO.CommentDTO cdto = new PostDTO.CommentDTO();
                cdto.setId(c.getId());
                cdto.setContent(c.getContent());
                cdto.setCreatedAt(c.getCreatedAt());
                cdto.setUsername(c.getUser().getUsername());
                cdto.setFullName(c.getUser().getFullName());
                cdto.setParentCommentId(c.getParentCommentId());
                cdto.setLikeCount((int) commentReactionRepository.countByComment(c));
                if (currentUser != null) {
                    commentReactionRepository.findByCommentAndUser(c, currentUser)
                            .ifPresent(r -> cdto.setCurrentReaction(r.getReactionType().name()));
                }
                return cdto;
            }).collect(Collectors.toList()));
            return dto;
        }).collect(Collectors.toList());
    }

    public List<PostDTO.ReactionUserDTO> getPostReactions(Long postId) {
    Post post = postRepository.findById(postId).orElseThrow();

    return postLikeRepository.findByPost(post)
            .stream()
            .map(like -> new PostDTO.ReactionUserDTO(
                    like.getUser().getUsername(),
                    like.getUser().getFullName(),
                    like.getUser().getAvatar(),
                    like.getReactionType()
            ))
            .collect(Collectors.toList());
}
    public List<PostDTO> getAllPostsForAdmin(User currentUser) {
        return convertToDTOs(postRepository.findAll(), currentUser);
    }
    public Map<String, Object> getAllPosts(User currentUser, String search, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Post> postPage;
        if (search == null || search.trim().isEmpty()) {
            postPage = postRepository.findByStatusOrderByCreatedAtDesc("APPROVED", pageable);
        } else {
            postPage = postRepository.findByStatusAndContentContainingIgnoreCaseOrderByCreatedAtDesc("APPROVED", search.trim(), pageable);
        }
        List<Post> posts = postPage.getContent().stream().filter(p -> canViewPost(p, currentUser)).collect(Collectors.toList());
        
        Map<String, Object> response = new HashMap<>();
        response.put("content", convertToDTOs(posts, currentUser));
        response.put("currentPage", postPage.getNumber());
        response.put("totalPages", postPage.getTotalPages());
        return response;
    }

    public Map<String, Object> getPostsByUser(User targetUser, User viewerUser, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Post> postPage = postRepository.findByUserAndStatusOrderByCreatedAtDesc(targetUser, "APPROVED", pageable);
        List<Post> posts = postPage.getContent().stream()
                .filter(p -> canViewPost(p, viewerUser))
                .collect(Collectors.toList());
        
        Map<String, Object> response = new HashMap<>();
        response.put("content", convertToDTOs(posts, viewerUser));
        response.put("currentPage", postPage.getNumber());
        response.put("totalPages", postPage.getTotalPages());
        return response;
    }

    private boolean canViewPost(Post post, User currentUser) {
        if (!"APPROVED".equals(post.getStatus())) {
            if (currentUser == null) return false;
            if (!post.getUser().getId().equals(currentUser.getId()) && !"ROLE_ADMIN".equals(currentUser.getRole())) {
                return false;
            }
        }
        if (currentUser != null && post.getUser().getId().equals(currentUser.getId())) {
            return true;
        }
        if (post.getPrivacy() == com.example.ptitsocialchat.enums.PrivacySetting.ONLY_ME || post.getPrivacy() == com.example.ptitsocialchat.enums.PrivacySetting.PRIVATE) {
            return false;
        }
        if (post.getPrivacy() == com.example.ptitsocialchat.enums.PrivacySetting.FRIENDS) {
            if (currentUser == null) return false;
            return friendRepository.findByUserAndFriend(post.getUser(), currentUser).isPresent();
        }
        return true;
    }

public boolean reactToPost(Long postId, User user, String reactionType) {
    Post post = postRepository.findById(postId).orElseThrow();
    if (!canViewPost(post, user)) {
        throw new RuntimeException("Bạn không có quyền truy cập bài viết này");
    }

    Optional<PostLike> existingLike = postLikeRepository.findByPostAndUser(post, user);

    if (existingLike.isPresent()) {
        PostLike like = existingLike.get();

        if (reactionType.equalsIgnoreCase(like.getReactionType())) {
            postLikeRepository.delete(like);
            return false;
        }

        like.setReactionType(reactionType);
        postLikeRepository.save(like);
        return true;
    }

    PostLike like = new PostLike();
    like.setPost(post);
    like.setUser(user);
    like.setReactionType(reactionType);
    like.setCreatedAt(LocalDateTime.now());
    postLikeRepository.save(like);

    return true;
}

public boolean reactToComment(Long commentId, User user, String reactionType) {
    Comment comment = commentRepository.findById(commentId).orElseThrow();
    if (!canViewPost(comment.getPost(), user)) {
        throw new RuntimeException("Bạn không có quyền truy cập bài viết này");
    }
    Optional<CommentReaction> existing = commentReactionRepository.findByCommentAndUser(comment, user);

    if (existing.isPresent()) {
        CommentReaction reaction = existing.get();
        if (reactionType.equalsIgnoreCase(reaction.getReactionType().name())) {
            commentReactionRepository.delete(reaction);
            return false;
        }
        reaction.setReactionType(ReactionType.valueOf(reactionType.toUpperCase()));
        commentReactionRepository.save(reaction);
        return true;
    }

    CommentReaction reaction = new CommentReaction();
    reaction.setComment(comment);
    reaction.setUser(user);
    reaction.setReactionType(ReactionType.valueOf(reactionType.toUpperCase()));
    commentReactionRepository.save(reaction);
    return true;
}
}
