package com.socialdashboard.service;

import com.socialdashboard.dto.Dtos;
import com.socialdashboard.entity.MetricSnapshot;
import com.socialdashboard.entity.Post;
import com.socialdashboard.entity.SocialAccount;
import com.socialdashboard.repository.MetricSnapshotRepository;
import com.socialdashboard.repository.PostRepository;
import com.socialdashboard.repository.SocialAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Derives plain-language observations from the metric history and post table.
 *
 * There is no model behind this: every insight is a comparison of the selected
 * window against the one before it, or of the user's own posts against each
 * other. The rules are deliberately conservative and state their evidence, so a
 * card never claims more than the numbers support.
 */
@Service
public class InsightsService {

    private static final Set<DayOfWeek> WEEKEND = Set.of(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY);

    private final SocialAccountRepository accountRepository;
    private final MetricSnapshotRepository metricRepository;
    private final PostRepository postRepository;

    public InsightsService(SocialAccountRepository accountRepository,
                           MetricSnapshotRepository metricRepository,
                           PostRepository postRepository) {
        this.accountRepository = accountRepository;
        this.metricRepository = metricRepository;
        this.postRepository = postRepository;
    }

    @Transactional(readOnly = true)
    public List<Dtos.Insight> insights(Long userId, String rangeKey, String platformRaw) {
        SocialAccount.Platform platform = AccountService.parsePlatform(platformRaw);
        DateRanges.Window window = DateRanges.window(rangeKey);
        DateRanges.Window previous = DateRanges.previous(window);

        List<MetricSnapshot> current = metricRepository.findRange(userId, window.from(), window.to(), platform);
        List<MetricSnapshot> before = metricRepository.findRange(userId, previous.from(), previous.to(), platform);

        if (current.isEmpty()) {
            return List.of(new Dtos.Insight("INFO", "No data yet",
                    "Connect an account to start collecting analytics.", null));
        }

        List<Dtos.Insight> out = new ArrayList<>();

        // 1. Engagement rate against the user's own previous window. A global
        //    benchmark would be arbitrary, so the comparison is self-referential.
        double engNow = engagementRate(current);
        double engBefore = engagementRate(before);
        if (engNow > 0) {
            String dir = engNow >= engBefore ? "up" : "down";
            double delta = Math.abs(engNow - engBefore);
            // "Last 30 days" reads badly after "the previous", so drop the prefix.
            String prior = previous.range().label().replaceFirst("^Last\\s+", "");
            out.add(new Dtos.Insight(
                    delta < 0.05 ? "NEUTRAL" : (dir.equals("up") ? "POSITIVE" : "NEGATIVE"),
                    dir.equals("up") ? "Engagement is climbing" : "Engagement has cooled",
                    String.format("Interactions per impression moved %s to %.2f%% from %.2f%% over the previous %s. %s",
                            dir, engNow, engBefore, prior.toLowerCase(),
                            dir.equals("up")
                                    ? "Your audience is responding more than it was."
                                    : "Worth reviewing the formats posted most recently."),
                    "/dashboard"
            ));
        }

        // 2. Follower momentum, as a growth rate so a big account is not
        //    rewarded for simply being big.
        FollowerMomentum momentum = followerMomentum(current, before);
        String rateText = momentum.rate() > 0 && momentum.rate() < 0.1
                ? "under 0.1%"
                : String.format("%.1f%%", momentum.rate());
        if (momentum.growth() > 0) {
            out.add(new Dtos.Insight(
                    momentum.rate() >= 5 ? "POSITIVE" : "NEUTRAL",
                    "Audience is growing, " + rateText,
                    String.format("You gained %s followers this %s, a %s increase on a starting audience of %s. %s",
                            Dtos.formatCount(momentum.growth()), window.range().label().toLowerCase(),
                            rateText, Dtos.formatCount(momentum.start()),
                            momentum.rate() >= 5
                                    ? "That is a strong pace at this audience size."
                                    : "On a large account this is a healthy, steady rate."),
                    "/accounts"
            ));
        } else {
            // Report a flat audience rather than dropping the card, so the list
            // does not silently lose a line whenever growth happens to be zero.
            out.add(new Dtos.Insight(
                    "NEUTRAL",
                    "Audience held steady",
                    String.format("No net follower change over this %s, ending on %s. %s",
                            window.range().label().toLowerCase(), Dtos.formatCount(momentum.end()),
                            "Worth checking whether recent posts reached anyone new."),
                    "/accounts"
            ));
        }

        // 3. Best posting day and time window, from the user's own engagement
        //    rate bucketed by weekday.
        BestSlot best = bestTimeSlot(current, window);
        if (best.day() != null) {
            out.add(new Dtos.Insight(
                    "POSITIVE",
                    "Your strongest day is " + titleCase(best.day().name()),
                    String.format("%s averaged a %.2f%% engagement rate against %.2f%% on your weakest day (%s). %s",
                            titleCase(best.day().name()), best.best(), best.worst(),
                            titleCase(best.worstDay().name()),
                            "Consider scheduling your strongest content for that day."),
                    "/posts"
            ));
        }

        // 4. Platform concentration: is the audience spread or dependent on one?
        List<SocialAccount> accounts = accountRepository.findByUserIdOrderByFollowersDesc(userId).stream()
                .filter(a -> platform == null || a.getPlatform() == platform)
                .toList();
        if (accounts.size() > 1) {
            long total = accounts.stream().mapToLong(SocialAccount::getFollowers).sum();
            SocialAccount top = accounts.get(0);
            double share = total == 0 ? 0 : top.getFollowers() * 100.0 / total;
            if (share >= 60) {
                out.add(new Dtos.Insight(
                        "WARNING",
                        share >= 80 ? "Almost entirely one platform" : "Audience is concentrated",
                        String.format("%s holds %.0f%% of your %s combined audience. %s",
                                titleCase(top.getPlatform().name()), share,
                                accounts.size() == 2 ? "two platforms" : accounts.size() + " platforms",
                                share >= 80
                                        ? "A policy change there would hit the whole business."
                                        : "Keep growing the smaller platforms to reduce that risk."),
                        "/accounts"
                ));
            }
        }

        // 5. Post format comparison, from the user's own posts in the window.
        FormatWinner format = bestFormat(userId, platform, window);
        if (format.type() != null) {
            out.add(new Dtos.Insight(
                    "POSITIVE",
                    titleCase(format.type().name()) + " posts lead on engagement",
                    String.format("They average %.1f%% engagement against %.1f%% across your other formats (%s posts analysed).",
                            format.bestRate(), format.overallRate(), format.count()),
                    "/posts"
            ));
        }

        // 6. Reach efficiency: impressions that never became a view.
        double impressions = sum(current, MetricSnapshot::getImpressions);
        double reach = sum(current, MetricSnapshot::getReach);
        if (impressions > 0 && reach > 0) {
            double frequency = impressions / reach;
            if (frequency > 3.0) {
                out.add(new Dtos.Insight(
                        "WARNING",
                        "Impressions are not converting to reach",
                        String.format("You are shown %.1f times for every person reached, well above a 2-3x norm. %s",
                                frequency, "This usually means impressions are being bought rather than earned."),
                        "/reports"
                ));
            }
        }

        // 7. Anything that needs attention, so the list is not purely positive.
        List<SocialAccount> unhealthy = accounts.stream()
                .filter(a -> a.getStatus() != SocialAccount.AccountStatus.ACTIVE)
                .toList();
        if (!unhealthy.isEmpty()) {
            out.add(new Dtos.Insight(
                    "NEGATIVE",
                    unhealthy.size() + (unhealthy.size() == 1 ? " account needs" : " accounts need") + " attention",
                    String.join("; ", unhealthy.stream()
                            .map(a -> "@" + a.getUsername() + " is " + a.getStatus().name().toLowerCase())
                            .toList()) + ". Data for these accounts may be stale.",
                    "/accounts"
            ));
        }

        if (out.isEmpty()) {
            out.add(new Dtos.Insight("NEUTRAL", "Holding steady",
                    "Nothing stands out in this range. Try a longer window to compare against a busier period.",
                    "/dashboard"));
        }

        out.sort(Comparator.comparingInt(i -> switch (i.severity()) {
            case "NEGATIVE", "WARNING" -> 0;
            case "POSITIVE" -> 1;
            default -> 2;
        }));
        return out;
    }

    private record FollowerMomentum(long growth, double rate, long start, long end) {
    }

    /** Growth measured as a fraction, so large accounts are not flattered. */
    private FollowerMomentum followerMomentum(List<MetricSnapshot> current, List<MetricSnapshot> before) {
        long start = firstDayFollowers(current);
        long end = lastDayFollowers(current);
        long growth = end - start;
        long previousStart = firstDayFollowers(before);
        long previousEnd = lastDayFollowers(before);
        long previousGrowth = previousEnd - previousStart;
        // Prefer the rate over the window; fall back to the previous rate when
        // this window is too new to have a meaningful sample.
        if (start > 0) {
            return new FollowerMomentum(growth, growth * 100.0 / start, start, end);
        }
        if (previousStart > 0) {
            return new FollowerMomentum(previousGrowth, previousGrowth * 100.0 / previousStart,
                    previousStart, previousEnd);
        }
        return new FollowerMomentum(growth, 0, start, end);
    }

    private record BestSlot(DayOfWeek day, DayOfWeek worstDay, double best, double worst) {
    }

    private BestSlot bestTimeSlot(List<MetricSnapshot> current, DateRanges.Window window) {
        Map<DayOfWeek, List<MetricSnapshot>> byDay = current.stream()
                .filter(m -> !WEEKEND.contains(m.getStatDate().getDayOfWeek()))
                .collect(Collectors.groupingBy(m -> m.getStatDate().getDayOfWeek()));
        if (byDay.size() < 2) {
            return new BestSlot(null, null, 0, 0);
        }
        Map<DayOfWeek, Double> rates = new LinkedHashMap<>();
        byDay.forEach((day, rows) -> rates.put(day, engagementRate(rows)));
        DayOfWeek best = rates.entrySet().stream()
                .max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse(null);
        DayOfWeek worst = rates.entrySet().stream()
                .min(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse(null);
        return new BestSlot(best, worst, rates.getOrDefault(best, 0d), rates.getOrDefault(worst, 0d));
    }

    private record FormatWinner(Post.PostType type, double bestRate, double overallRate, long count) {
    }

    /**
     * Compares the mean engagement rate of each post type against the mean of
     * everything else, so a type only wins if it genuinely beats the mix. The
     * rate uses reach as the denominator, matching PostSummary, so the insight
     * and the posts table can never disagree.
     */
    private FormatWinner bestFormat(Long userId, SocialAccount.Platform platform, DateRanges.Window window) {
        java.time.Instant from = window.from().atStartOfDay(java.time.ZoneOffset.UTC).toInstant();
        List<Post> posts = postRepository.search(userId, platform, null, null,
                        org.springframework.data.domain.PageRequest.of(0, 500)).getContent().stream()
                .filter(p -> !p.getPublishedAt().isBefore(from))
                .toList();
        if (posts.size() < 6) {
            return new FormatWinner(null, 0, 0, 0);
        }
        Map<Post.PostType, List<Post>> byType = posts.stream()
                .collect(Collectors.groupingBy(Post::getType));
        if (byType.size() < 2) {
            return new FormatWinner(null, 0, 0, 0);
        }
        Map<Post.PostType, Double> rates = new LinkedHashMap<>();
        byType.forEach((type, list) -> rates.put(type, list.stream()
                .mapToDouble(InsightsService::postEngagementRate).average().orElse(0)));
        Map.Entry<Post.PostType, Double> best = rates.entrySet().stream()
                .max(Map.Entry.comparingByValue()).orElseThrow();
        double others = posts.stream().filter(p -> p.getType() != best.getKey())
                .mapToDouble(InsightsService::postEngagementRate).average().orElse(0);
        if (best.getValue() <= others * 1.1) {
            return new FormatWinner(null, 0, 0, 0);
        }
        return new FormatWinner(best.getKey(), best.getValue(), others, byType.get(best.getKey()).size());
    }

    /** Interactions per reach, as a percentage. Mirrors Dtos.PostSummary. */
    private static double postEngagementRate(Post post) {
        return post.getReach() == 0 ? 0
                : (post.getLikes() + post.getComments() + post.getShares()) * 100.0 / post.getReach();
    }

    private static double engagementRate(List<MetricSnapshot> rows) {
        double impressions = sum(rows, MetricSnapshot::getImpressions);
        if (impressions <= 0) {
            return 0;
        }
        double interactions = sum(rows, m -> m.getLikes() + m.getComments() + m.getShares());
        return interactions * 100.0 / impressions;
    }

    private static double sum(List<MetricSnapshot> rows, java.util.function.ToDoubleFunction<MetricSnapshot> f) {
        return rows.stream().mapToDouble(f).sum();
    }

    /**
     * Follower count on each account's most recent day in the rows, summed.
     * Picks by date rather than by max, so an account that briefly lost
     * followers is not reported as being larger than it really is.
     */
    private static long lastDayFollowers(List<MetricSnapshot> rows) {
        Map<Long, MetricSnapshot> latest = new LinkedHashMap<>();
        for (MetricSnapshot row : rows) {
            latest.merge(row.getAccount().getId(), row,
                    (a, b) -> a.getStatDate().isBefore(b.getStatDate()) ? b : a);
        }
        return latest.values().stream().mapToLong(MetricSnapshot::getFollowers).sum();
    }

    /** Follower count on each account's earliest day in the rows, summed. */
    private static long firstDayFollowers(List<MetricSnapshot> rows) {
        Map<Long, MetricSnapshot> earliest = new LinkedHashMap<>();
        for (MetricSnapshot row : rows) {
            earliest.merge(row.getAccount().getId(), row,
                    (a, b) -> a.getStatDate().isAfter(b.getStatDate()) ? b : a);
        }
        return earliest.values().stream().mapToLong(MetricSnapshot::getFollowers).sum();
    }

    private static String titleCase(String raw) {
        if (raw == null || raw.isBlank()) {
            return raw;
        }
        return raw.charAt(0) + raw.substring(1).toLowerCase();
    }
}
