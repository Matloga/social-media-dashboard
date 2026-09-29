package com.socialdashboard.service;

import com.socialdashboard.dto.Dtos;
import com.socialdashboard.entity.Post;
import com.socialdashboard.entity.Post.PostType;
import com.socialdashboard.entity.SocialAccount.Platform;
import com.socialdashboard.exception.ApiException;
import com.socialdashboard.repository.PostRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class PostService {

    private static final int MAX_PAGE_SIZE = 100;

    private final PostRepository postRepository;

    public PostService(PostRepository postRepository) {
        this.postRepository = postRepository;
    }

    @Transactional(readOnly = true)
    public Page<Dtos.PostSummary> search(Long userId, int page, int size, String sort,
                                          String direction, String platformRaw, String typeRaw, Long accountId) {
        Platform platform = AccountService.parsePlatform(platformRaw);
        PostType type = parseType(typeRaw);

        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), MAX_PAGE_SIZE),
                Sort.by(resolveDirection(direction), resolveSortField(sort)));

        return postRepository.search(userId, platform, type, accountId, pageable)
                .map(Dtos.PostSummary::from);
    }

    /** Hashtag leaderboard, ranked by total engagement across the user's posts. */
    @Transactional(readOnly = true)
    public List<HashtagStat> hashtagAnalytics(Long userId, String platformRaw, int limit) {
        Platform platform = AccountService.parsePlatform(platformRaw);
        List<Post> posts = postRepository.findTopByUserForChart(userId, platform, null, PageRequest.of(0, 1000));

        Map<String, long[]> totals = new LinkedHashMap<>();
        for (Post post : posts) {
            for (String tag : post.getHashtags()) {
                long[] acc = totals.computeIfAbsent(tag, k -> new long[3]);
                acc[0] += post.getLikes();
                acc[1] += post.getComments();
                acc[2] += post.getShares();
            }
        }

        return totals.entrySet().stream()
                .map(e -> new HashtagStat("#" + e.getKey(), e.getKey(), e.getValue()[0], e.getValue()[1],
                        e.getValue()[2], e.getValue()[0] + e.getValue()[1] + e.getValue()[2]))
                .sorted((a, b) -> Long.compare(b.totalEngagement(), a.totalEngagement()))
                .limit(Math.min(Math.max(limit, 1), 50))
                .toList();
    }

    public record HashtagStat(String tag, String name, long likes, long comments, long shares, long totalEngagement) {
    }

    private static PostType parseType(String raw) {
        if (raw == null || raw.isBlank() || "all".equalsIgnoreCase(raw)) {
            return null;
        }
        try {
            return PostType.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new ApiException(org.springframework.http.HttpStatus.BAD_REQUEST,
                    "Unknown post type: " + raw, "type");
        }
    }

    private static String resolveSortField(String raw) {
        if (raw == null || raw.isBlank()) {
            return "publishedAt";
        }
        return switch (raw.toLowerCase(Locale.ROOT)) {
            case "likes" -> "likes";
            case "comments" -> "comments";
            case "shares" -> "shares";
            case "reach" -> "reach";
            case "impressions" -> "impressions";
            case "engagement" -> "reach";
            default -> "publishedAt";
        };
    }

    private static Sort.Direction resolveDirection(String raw) {
        return "asc".equalsIgnoreCase(raw) ? Sort.Direction.ASC : Sort.Direction.DESC;
    }
}
