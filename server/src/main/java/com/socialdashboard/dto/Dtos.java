package com.socialdashboard.dto;

import com.socialdashboard.entity.Goal;
import com.socialdashboard.entity.Notification;
import com.socialdashboard.entity.Post;
import com.socialdashboard.entity.SocialAccount;
import com.socialdashboard.entity.SocialAccount.AccountStatus;
import com.socialdashboard.entity.SocialAccount.Platform;
import com.socialdashboard.entity.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

public class Dtos {

    private Dtos() {
    }

    // ---------- auth ----------

    public record RegisterRequest(
            @NotBlank(message = "Email is required")
            @Email(message = "Email must be valid")
            String email,

            @NotBlank(message = "Password is required")
            @Size(min = 8, message = "Password must be at least 8 characters")
            String password,

            @NotBlank(message = "Name is required")
            @Size(max = 80, message = "Name must be under 80 characters")
            String name
    ) {
    }

    public record LoginRequest(
            @NotBlank(message = "Email is required")
            @Email(message = "Email must be valid")
            String email,

            @NotBlank(message = "Password is required")
            String password
    ) {
    }

    public record UpdateProfileRequest(
            @Size(max = 80, message = "Name must be under 80 characters")
            String name,

            @Size(max = 500, message = "Bio must be under 500 characters")
            String bio,

            String currentPassword,
            String newPassword
    ) {
    }

    public record UserResponse(
            Long id,
            String email,
            String name,
            String bio,
            String avatarColor,
            Instant createdAt,
            long accountCount,
            long postCount
    ) {
        public static UserResponse from(User user) {
            return new UserResponse(
                    user.getId(),
                    user.getEmail(),
                    user.getName(),
                    user.getBio(),
                    user.getAvatarColor(),
                    user.getCreatedAt(),
                    user.getAccounts().size(),
                    user.getPosts().size()
            );
        }
    }

    public record AuthResponse(String token, long expiresInMs, UserResponse user) {
    }

    // ---------- accounts ----------

    /**
     * {@code platform} is a String rather than the enum type on purpose: a
     * constraint annotation on a record component of enum type is applied to the
     * enum class itself (TYPE_USE), which Hibernate Validator rejects. Parsing
     * here also turns an unknown platform into a clean 400 instead of a Jackson
     * deserialisation failure.
     */
    public record CreateAccountRequest(
            @NotBlank(message = "Platform is required")
            String platform,

            @NotBlank(message = "Username is required")
            @Size(max = 80)
            String username
    ) {
    }

    public record UpdateAccountRequest(
            @Size(max = 80)
            String username,
            AccountStatus status
    ) {
    }

    public record AccountResponse(
            Long id,
            Platform platform,
            String username,
            String handle,
            long followers,
            long following,
            AccountStatus status,
            String statusMessage,
            boolean tokenValid,
            Instant connectedAt,
            Instant lastSyncedAt,
            List<PostSummary> topPosts
    ) {
        public static AccountResponse from(SocialAccount account, List<PostSummary> topPosts) {
            return new AccountResponse(
                    account.getId(),
                    account.getPlatform(),
                    account.getUsername(),
                    "@" + account.getUsername().toLowerCase(),
                    account.getFollowers(),
                    account.getFollowing(),
                    account.getStatus(),
                    account.getStatusMessage(),
                    account.isTokenValid(),
                    account.getConnectedAt(),
                    account.getLastSyncedAt(),
                    topPosts
            );
        }
    }

    // ---------- posts ----------

    public record PostSummary(
            Long id,
            Platform platform,
            String username,
            String caption,
            Post.PostType type,
            long likes,
            long comments,
            long shares,
            long reach,
            long impressions,
            String thumbnailUrl,
            String permalink,
            Instant publishedAt,
            double engagementRate,
            List<String> hashtags
    ) {
        public static PostSummary from(Post post) {
            double rate = post.getReach() == 0 ? 0
                    : (post.getLikes() + post.getComments() + post.getShares()) * 100.0 / post.getReach();
            return new PostSummary(
                    post.getId(),
                    post.getPlatform(),
                    post.getAccount() != null ? post.getAccount().getUsername() : "-",
                    post.getCaption(),
                    post.getType(),
                    post.getLikes(),
                    post.getComments(),
                    post.getShares(),
                    post.getReach(),
                    post.getImpressions(),
                    post.getThumbnailUrl(),
                    post.getPermalink(),
                    post.getPublishedAt(),
                    Math.round(rate * 100) / 100.0,
                    post.getHashtags()
            );
        }
    }

    // ---------- stats ----------

    public record MetricPoint(
            String date,
            long followers,
            long likes,
            long comments,
            long shares,
            long impressions,
            long reach,
            long following,
            double engagementRate,
            long profileVisits,
            long postsPublished
    ) {
    }

    public record DeltaInfo(long absolute, double percent, String direction) {
        public static DeltaInfo of(long current, long previous) {
            long diff = current - previous;
            double pct = previous == 0 ? (diff == 0 ? 0 : 100.0) : diff * 100.0 / previous;
            String dir = diff > 0 ? "up" : diff < 0 ? "down" : "flat";
            return new DeltaInfo(diff, Math.round(pct * 10) / 10.0, dir);
        }
    }

    /** {@code format} tells the client how to render: "number" or "percent". */
    public record SummaryCard(String key, String label, double value, DeltaInfo delta, String format) {
    }

    public record PlatformBreakdown(
            Platform platform,
            String username,
            long followers,
            long likes,
            long comments,
            double engagementRate,
            double shareOfFollowers
    ) {
    }

    public record OverviewResponse(
            List<SummaryCard> cards,
            List<PlatformBreakdown> byPlatform,
            List<PostSummary> topPosts,
            String range,
            long unreadNotifications
    ) {
    }

    public record TimeSeriesResponse(
            List<MetricPoint> points,
            List<String> metrics,
            String range,
            long totalPoints
    ) {
    }

    // ---------- reports ----------

    public record ReportResponse(
            String id,
            String range,
            String rangeLabel,
            java.time.LocalDate from,
            java.time.LocalDate to,
            long totalReach,
            long totalImpressions,
            long totalFollowers,
            long followerGrowth,
            long totalLikes,
            long totalComments,
            long totalShares,
            double engagementRate,
            long postsPublished,
            List<PlatformBreakdown> byPlatform,
            List<MetricPoint> trend,
            Instant generatedAt
    ) {
    }

    // ---------- notifications ----------

    public record NotificationResponse(
            Long id,
            Notification.NotificationType type,
            String title,
            String message,
            String link,
            boolean read,
            Instant createdAt
    ) {
        public static NotificationResponse from(Notification n) {
            return new NotificationResponse(
                    n.getId(), n.getType(), n.getTitle(), n.getMessage(), n.getLink(), n.isRead(), n.getCreatedAt()
            );
        }
    }

    // ---------- goals ----------

    public record CreateGoalRequest(
            Goal.GoalMetric metric,
            String platform,
            String title,
            @jakarta.validation.constraints.Positive(message = "must be greater than zero") double targetValue,
            Goal.Period period,
            LocalDate startDate,
            LocalDate endDate
    ) {
    }

    public record UpdateGoalRequest(
            String title,
            @jakarta.validation.constraints.Positive(message = "must be greater than zero") Double targetValue,
            LocalDate endDate,
            Boolean active
    ) {
    }

    /**
     * @param currentValue where the metric stands today, for context
     * @param progress     percent of the target reached, 0-100+
     * @param timeProgress percent of the goal period elapsed, 0-100
     * @param paceDelta    progress minus timeProgress; positive is ahead of pace
     */
    public record GoalResponse(
            Long id,
            String title,
            String metric,
            String platform,
            double targetValue,
            double achieved,
            double currentValue,
            double baselineValue,
            double progress,
            double timeProgress,
            double paceDelta,
            String status,
            String period,
            LocalDate startDate,
            LocalDate endDate,
            long daysElapsed,
            long daysTotal,
            boolean active,
            Instant createdAt
    ) {
    }

    /**
     * A single observation with the evidence behind it. Severity is one of
     * POSITIVE, NEGATIVE, WARNING or NEUTRAL, and the client is responsible for
     * choosing the colour.
     */
    public record Insight(String severity, String title, String detail, String link) {
    }

    // ---------- generic ----------

    public record ApiError(String message, String field, int status) {
        public static ApiError of(String message, int status) {
            return new ApiError(message, null, status);
        }
    }

    /** Compact human-readable count, e.g. 1_540 -> "1.5K". */
    public static String formatCount(long value) {
        long abs = Math.abs(value);
        if (abs >= 1_000_000) {
            return trim(value / 1_000_000.0) + "M";
        }
        if (abs >= 1_000) {
            return trim(value / 1_000.0) + "K";
        }
        return String.valueOf(value);
    }

    private static String trim(double value) {
        String text = String.format(Locale.ROOT, "%.1f", value);
        return text.endsWith(".0") ? text.substring(0, text.length() - 2) : text;
    }
}
