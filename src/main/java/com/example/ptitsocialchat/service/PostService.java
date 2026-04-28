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
    private com.example.ptitsocialchat.repository.PostMediaRepository postMediaRepository;
    @Autowired
    private NotificationService notificationService;
    @Autowired
    private ModerationService moderationService;
    @Autowired
    private com.example.ptitsocialchat.repository.ModerationSettingsRepository moderationSettingsRepository;
    @Autowired
    private com.example.ptitsocialchat.repository.CommentReactionRepository commentReactionRepository;

    /**
     * DTO chứa kết quả trả về sau khi tạo bài viết, bao gồm trạng thái kiểm duyệt
     */
    public static class CreatePostResult {
        private String status; // Trạng thái: APPROVED (Đã duyệt), PENDING (Chờ duyệt), REJECTED (Từ chối)
        private String message; // Thông báo chi tiết gửi về cho client
        private String label; // Nhãn kiểm duyệt AI (ví dụ: CLEAN, OFFENSIVE)


        public CreatePostResult(String status, String message, String label) {
            this.status = status;
            this.message = message;
            this.label = label;
        }

        public String getStatus() {
            return status;
        }

        public String getMessage() {
            return message;
        }

        public String getLabel() {
            return label;
        }
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
            // Không kiểm duyệt → duyệt ngay
            post.setStatus("APPROVED");
            postRepository.save(post);
            return new CreatePostResult("APPROVED", "Đăng bài thành công!", null);

        } else if ("MANUAL".equals(mode)) {
            // Kiểm duyệt thủ công → chờ admin
            post.setStatus("PENDING");
            postRepository.save(post);
            return new CreatePostResult("PENDING",
                    "Bài viết đang chờ Admin kiểm duyệt trước khi hiển thị.", null);

        } else if ("AUTO_AI".equals(mode)) {
            // Kiểm duyệt AI tự động
            // Nếu chỉ có ảnh (content rỗng) → AI không phân tích được → tự động duyệt
            if (content == null || content.trim().isEmpty()) {
                post.setStatus("APPROVED");
                postRepository.save(post);
                return new CreatePostResult("APPROVED", "Đăng bài thành công!", null);
            }
            ModerationService.ModerationResult aiResult = moderationService.moderate(content, aiServiceUrl);

            if (!aiResult.isSuccess()) {
                // AI service lỗi → fallback: chờ duyệt thủ công
                System.err.println("[PostService] AI service failed: " + aiResult.getErrorMessage());
                post.setStatus("PENDING");
                postRepository.save(post);
                return new CreatePostResult("PENDING",
                        "AI Moderation tạm thời không khả dụng. Bài viết đang chờ kiểm duyệt thủ công.",
                        null);
            }

            post.setModerationLabel(aiResult.getLabel());
            post.setModerationConfidence(aiResult.getConfidence());

            if (aiResult.isToxic()) {
                // Nội dung toxic → TỰ ĐỘNG TỪ CHỐI
                post.setStatus("REJECTED");
                postRepository.save(post);
                String labelVi = "OFFENSIVE".equals(aiResult.getLabel()) ? "Xúc phạm" : "Thù ghét";
                return new CreatePostResult("REJECTED",
                        "Bài viết bị từ chối tự động. Nội dung được phát hiện: " + labelVi
                                + " (độ tin cậy: " + Math.round(aiResult.getConfidence() * 100) + "%).",
                        aiResult.getLabel());
            } else {
                // Nội dung sạch → TỰ ĐỘNG DUYỆT
                post.setStatus("APPROVED");
                postRepository.save(post);
                return new CreatePostResult("APPROVED", "Đăng bài thành công!", aiResult.getLabel());
            }
        }

        // Fallback
        post.setStatus("PENDING");
        postRepository.save(post);
        return new CreatePostResult("PENDING", "Bài viết đang chờ kiểm duyệt.", null);
    }

    public List<PostDTO> getAllPosts(User currentUser, String search) {
        List<Post> posts;
        if (search != null && !search.trim().isEmpty()) {
            posts = postRepository.findByStatusAndContentContainingIgnoreCaseOrderByCreatedAtDesc("APPROVED",
                    search.trim());
        } else {
            posts = postRepository.findByStatusOrderByCreatedAtDesc("APPROVED");
        }
        return posts.stream()
                .map(post -> convertToDTO(post, currentUser))
                .collect(Collectors.toList());
    }

    public List<PostDTO> getAllPostsForAdmin(User currentUser) {
        return postRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(post -> convertToDTO(post, currentUser))
                .collect(Collectors.toList());
    }

    public List<PostDTO> getPostsByStatus(String status, User currentUser) {
        return postRepository.findByStatusOrderByCreatedAtDesc(status).stream()
                .map(post -> convertToDTO(post, currentUser))
                .collect(Collectors.toList());
    }

    public List<PostDTO> getPostsByUser(User targetUser, User viewerUser) {
        return postRepository.findByUserAndStatusOrderByCreatedAtDesc(targetUser, "APPROVED").stream()
                .map(post -> convertToDTO(post, viewerUser))
                .collect(Collectors.toList());
    }

    public Comment addComment(Long postId, String content, User user, Long parentId) {
        Post post = postRepository.findById(postId).orElseThrow();
        Comment comment = new Comment();
        comment.setContent(content);
        if (parentId != null) {
            comment.setParentCommentId(parentId);
        }
        comment.setPost(post);
        comment.setUser(user);
        comment.setCreatedAt(LocalDateTime.now());
        Comment savedComment = commentRepository.save(comment);
        notificationService.createNotification(post.getUser(), user, com.example.ptitsocialchat.enums.NotificationType.COMMENT, "home.html#post-" + postId);
        return savedComment;
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

        if (isNewReaction && !post.getUser().getId().equals(user.getId())) {
            notificationService.createNotification(post.getUser(), user, com.example.ptitsocialchat.enums.NotificationType.LIKE, "home.html#post-" + postId);
        }
        return isNewReaction;
    }

    @Transactional
    public boolean reactToComment(Long commentId, User user, String reactionType) {
        Comment comment = commentRepository.findById(commentId).orElseThrow();
        Optional<com.example.ptitsocialchat.entity.CommentReaction> existing = commentReactionRepository.findByCommentAndUser(comment, user);
        
        boolean isNewReaction = false;
        if (reactionType == null || reactionType.trim().isEmpty() || reactionType.equalsIgnoreCase("NONE")) {
            existing.ifPresent(commentReactionRepository::delete);
            return false;
        }

        if (existing.isPresent()) {
            com.example.ptitsocialchat.entity.CommentReaction reaction = existing.get();
            if (reactionType.equals(reaction.getReactionType().name())) {
                commentReactionRepository.delete(reaction);
                return false;
            } else {
                reaction.setReactionType(com.example.ptitsocialchat.enums.ReactionType.valueOf(reactionType));
                commentReactionRepository.save(reaction);
                isNewReaction = true;
            }
        } else {
            com.example.ptitsocialchat.entity.CommentReaction reaction = new com.example.ptitsocialchat.entity.CommentReaction();
            reaction.setComment(comment);
            reaction.setUser(user);
            reaction.setReactionType(com.example.ptitsocialchat.enums.ReactionType.valueOf(reactionType));
            commentReactionRepository.save(reaction);
            isNewReaction = true;
        }

        if (isNewReaction && !comment.getUser().getId().equals(user.getId())) {
            notificationService.createNotification(comment.getUser(), user, com.example.ptitsocialchat.enums.NotificationType.LIKE, "home.html#post-" + comment.getPost().getId());
        }
        return isNewReaction;
    }

    public List<PostDTO.ReactionUserDTO> getPostReactions(Long postId) {
        Post post = postRepository.findById(postId).orElseThrow();
        return post.getLikes().stream().map(l -> {
            PostDTO.ReactionUserDTO dto = new PostDTO.ReactionUserDTO();
            dto.setUsername(l.getUser().getUsername());
            dto.setFullName(l.getUser().getFullName());
            dto.setAvatar(l.getUser().getAvatar());
            dto.setReactionType(l.getReactionType() != null ? l.getReactionType() : "LIKE");
            return dto;
        }).collect(Collectors.toList());
    }

    public void deletePost(Long postId, User user) {
        Post post = postRepository.findById(postId).orElseThrow();
        if (!post.getUser().getId().equals(user.getId()) && !"ROLE_ADMIN".equals(user.getRole())) {
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
        dto.setVideoUrl(post.getVideoUrl());
        dto.setCheckInLocation(post.getCheckInLocation());
        dto.setPrivacy(post.getPrivacy() != null ? post.getPrivacy().name() : "PUBLIC");
        dto.setCreatedAt(post.getCreatedAt());
        dto.setUsername(post.getUser().getUsername());
        dto.setFullName(post.getUser().getFullName());
        dto.setAvatar(post.getUser().getAvatar());
        
        if (post.getMedia() != null) {
            dto.setMediaUrls(post.getMedia().stream()
                .map(com.example.ptitsocialchat.entity.PostMedia::getMediaUrl)
                .collect(Collectors.toList()));
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

        dto.setStatus(post.getStatus());
        dto.setModerationLabel(post.getModerationLabel());
        dto.setModerationConfidence(post.getModerationConfidence());
        
        dto.setComments(post.getComments().stream().map(c -> {
            PostDTO.CommentDTO cdto = new PostDTO.CommentDTO();
            cdto.setId(c.getId());
            cdto.setContent(c.getContent());
            cdto.setImageUrl(c.getImageUrl());
            cdto.setCreatedAt(c.getCreatedAt());
            cdto.setUsername(c.getUser().getUsername());
            cdto.setFullName(c.getUser().getFullName());
            cdto.setParentCommentId(c.getParentCommentId());
            
            if (c.getReactions() != null) {
                Map<String, Long> cReactionCounts = c.getReactions().stream()
                    .collect(Collectors.groupingBy(r -> r.getReactionType().name(), Collectors.counting()));
                cdto.setReactionCounts(cReactionCounts);
                cdto.setLikeCount(c.getReactions().size());

                if (currentUser != null) {
                    Optional<com.example.ptitsocialchat.entity.CommentReaction> currentCReac = c.getReactions().stream()
                        .filter(r -> r.getUser().getId().equals(currentUser.getId())).findFirst();
                    cdto.setCurrentReaction(currentCReac.map(r -> r.getReactionType().name()).orElse(null));
                }
            }
            return cdto;
        }).collect(Collectors.toList()));
        return dto;
    }
}
