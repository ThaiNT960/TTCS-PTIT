package com.example.ptitsocialchat.service;

import com.example.ptitsocialchat.dto.FriendSuggestionDTO;
import com.example.ptitsocialchat.entity.User;
import com.example.ptitsocialchat.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class FriendSuggestionService {

    @Autowired
    private FriendRepository friendRepository;
    @Autowired
    private FriendRequestRepository friendRequestRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PostLikeRepository postLikeRepository;
    @Autowired
    private CommentRepository commentRepository;

    private static class CandidateFeatures {
        User user;
        int mutualFriendCount = 0;
        int commonLikedPosts = 0;
        int commonCommentedPosts = 0;

        CandidateFeatures(User user) {
            this.user = user;
        }
    }

    public List<FriendSuggestionDTO> getSuggestions(User currentUser, int limit) {
        limit = Math.max(1, Math.min(50, limit));

        // 1. Build excluded IDs
        Set<Long> excludedIds = new HashSet<>();
        excludedIds.add(currentUser.getId());
        excludedIds.addAll(friendRepository.findFriendIdsByUser(currentUser));
        excludedIds.addAll(friendRequestRepository.findPendingSenderIds(currentUser));
        excludedIds.addAll(friendRequestRepository.findPendingReceiverIds(currentUser));

        List<Long> excludedList = new ArrayList<>(excludedIds);

        // 2. Map to hold candidates
        Map<Long, CandidateFeatures> candidateMap = new HashMap<>();

        // 3. Add friend-of-friend candidates
        List<Object[]> mutualFriends = friendRepository.findSuggestionsByMutualFriends(currentUser, excludedList);
        for (Object[] row : mutualFriends) {
            User friend = (User) row[0];
            int count = ((Number) row[1]).intValue();
            CandidateFeatures cf = candidateMap.computeIfAbsent(friend.getId(), id -> new CandidateFeatures(friend));
            cf.mutualFriendCount = count;
        }

        // 4. Add fallback candidates by profile similarity
        if (candidateMap.size() < limit * 2) {
            boolean hasProfile = (currentUser.getEducation() != null && !currentUser.getEducation().trim().isEmpty())
                    || (currentUser.getWorkplace() != null && !currentUser.getWorkplace().trim().isEmpty())
                    || (currentUser.getLocation() != null && !currentUser.getLocation().trim().isEmpty());
                    
            if (hasProfile) {
                // To avoid passing empty lists to NOT IN in JPQL (which can fail), ensure excludedList is never empty.
                List<User> similarUsers = userRepository.findSimilarProfileUsers(
                        currentUser.getEducation(),
                        currentUser.getWorkplace(),
                        currentUser.getLocation(),
                        excludedList
                );
                for (User u : similarUsers) {
                    if (!candidateMap.containsKey(u.getId())) {
                        candidateMap.put(u.getId(), new CandidateFeatures(u));
                    }
                }
            }
        }

        // 4.5. Add candidates from common interactions
        if (candidateMap.size() < limit * 2) {
            List<User> commonLiker = postLikeRepository.findUsersWithCommonLikes(currentUser);
            for (User u : commonLiker) {
                if (!excludedIds.contains(u.getId()) && !candidateMap.containsKey(u.getId())) {
                    candidateMap.put(u.getId(), new CandidateFeatures(u));
                }
            }
            List<User> commonCommenter = commentRepository.findUsersWithCommonComments(currentUser);
            for (User u : commonCommenter) {
                if (!excludedIds.contains(u.getId()) && !candidateMap.containsKey(u.getId())) {
                    candidateMap.put(u.getId(), new CandidateFeatures(u));
                }
            }
        }

        if (candidateMap.isEmpty()) {
            return new ArrayList<>();
        }

        List<Long> candidateIds = new ArrayList<>(candidateMap.keySet());

        // 5. Fetch interactions in batch
        List<Object[]> likedPosts = postLikeRepository.countCommonLikedPosts(currentUser, candidateIds);
        for (Object[] row : likedPosts) {
            Long uid = ((Number) row[0]).longValue();
            int count = ((Number) row[1]).intValue();
            if (candidateMap.containsKey(uid)) {
                candidateMap.get(uid).commonLikedPosts = count;
            }
        }

        List<Object[]> commentedPosts = commentRepository.countCommonCommentedPosts(currentUser, candidateIds);
        for (Object[] row : commentedPosts) {
            Long uid = ((Number) row[0]).longValue();
            int count = ((Number) row[1]).intValue();
            if (candidateMap.containsKey(uid)) {
                candidateMap.get(uid).commonCommentedPosts = count;
            }
        }

        // 6. Calculate scores and map to DTO
        List<FriendSuggestionDTO> results = new ArrayList<>();
        for (CandidateFeatures cf : candidateMap.values()) {
            User c = cf.user;
            int sameEd = isSame(currentUser.getEducation(), c.getEducation()) ? 1 : 0;
            int sameWp = isSame(currentUser.getWorkplace(), c.getWorkplace()) ? 1 : 0;
            int sameLoc = isSame(currentUser.getLocation(), c.getLocation()) ? 1 : 0;

            int score = Math.min(cf.mutualFriendCount, 5) * 12
                      + sameEd * 18
                      + sameWp * 10
                      + sameLoc * 6
                      + Math.min(cf.commonLikedPosts, 5) * 3
                      + Math.min(cf.commonCommentedPosts, 3) * 5;

            if (score <= 0) continue;

            List<String> reasons = new ArrayList<>();
            if (cf.mutualFriendCount > 0) reasons.add(cf.mutualFriendCount + " bạn chung");
            if (sameEd == 1) reasons.add("Cùng học tại " + c.getEducation());
            if (sameWp == 1) reasons.add("Cùng làm việc tại " + c.getWorkplace());
            if (sameLoc == 1) reasons.add("Cùng địa điểm " + c.getLocation());
            if (cf.commonLikedPosts > 0) reasons.add("Cùng quan tâm " + cf.commonLikedPosts + " bài viết");
            if (cf.commonCommentedPosts > 0) reasons.add("Cùng thảo luận ở " + cf.commonCommentedPosts + " bài viết");

            FriendSuggestionDTO dto = new FriendSuggestionDTO();
            dto.setUsername(c.getUsername());
            dto.setFullName(c.getFullName() != null ? c.getFullName() : c.getUsername());
            dto.setAvatar(c.getAvatar());
            dto.setMutualFriendCount(cf.mutualFriendCount);
            dto.setCommonLikedPosts(cf.commonLikedPosts);
            dto.setCommonCommentedPosts(cf.commonCommentedPosts);
            dto.setScore(score);
            dto.setReasons(reasons);
            results.add(dto);
        }

        // 7. Sort
        results.sort((a, b) -> {
            if (a.getScore() != b.getScore()) return Integer.compare(b.getScore(), a.getScore());
            if (a.getMutualFriendCount() != b.getMutualFriendCount()) return Integer.compare(b.getMutualFriendCount(), a.getMutualFriendCount());
            if (a.getCommonCommentedPosts() != b.getCommonCommentedPosts()) return Integer.compare(b.getCommonCommentedPosts(), a.getCommonCommentedPosts());
            if (a.getCommonLikedPosts() != b.getCommonLikedPosts()) return Integer.compare(b.getCommonLikedPosts(), a.getCommonLikedPosts());
            String nameA = a.getFullName() != null ? a.getFullName() : "";
            String nameB = b.getFullName() != null ? b.getFullName() : "";
            return nameA.compareToIgnoreCase(nameB);
        });

        // 8. Limit
        return results.stream().limit(limit).collect(Collectors.toList());
    }

    private boolean isSame(String s1, String s2) {
        if (s1 == null || s2 == null) return false;
        if (s1.trim().isEmpty() || s2.trim().isEmpty()) return false;
        return s1.trim().equalsIgnoreCase(s2.trim());
    }
}
