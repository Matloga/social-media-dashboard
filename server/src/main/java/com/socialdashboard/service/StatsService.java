package com.socialdashboard.service;

import com.socialdashboard.dto.Dtos;
import com.socialdashboard.entity.MetricSnapshot;
import com.socialdashboard.entity.Post;
import com.socialdashboard.entity.SocialAccount;
import com.socialdashboard.entity.SocialAccount.Platform;
import com.socialdashboard.repository.MetricSnapshotRepository;
import com.socialdashboard.repository.NotificationRepository;
import com.socialdashboard.repository.PostRepository;
import com.socialdashboard.repository.SocialAccountRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class StatsService {

    private final SocialAccountRepository accountRepository;
    private final MetricSnapshotRepository metricRepository;
    private final PostRepository postRepository;
    private final NotificationRepository notificationRepository;

    public StatsService(SocialAccountRepository accountRepository,
                        MetricSnapshotRepository metricRepository,
                        PostRepository postRepository,
                        NotificationRepository notificationRepository) {
        this.accountRepository = accountRepository;
        this.metricRepository = metricRepository;
        this.postRepository = postRepository;
        this.notificationRepository = notificationRepository;
    }

    @Transactional(readOnly = true)
    public Dtos.OverviewResponse overview(Long userId, String rangeKey, String platformRaw) {
        Platform platform = AccountService.parsePlatform(platformRaw);
        DateRanges.Window window = DateRanges.window(rangeKey);
        DateRanges.Window previousWindow = DateRanges.previous(window);

        List<SocialAccount> allAccounts = accountRepository.findByUserIdOrderByFollowersDesc(userId);
        List<SocialAccount> scoped = filterAccounts(allAccounts, platform);

        List<MetricSnapshot> current = metricRepository.findRange(userId, window.from(), window.to(), platform);
        List<MetricSnapshot> previous = metricRepository.findRange(userId,
                previousWindow.from(), previousWindow.to(), platform);

        Totals now = Totals.of(current);
        Totals before = Totals.of(previous);

        // Followers is a stock, so the delta has to be computed per account
        // (last day minus first day) and then summed. Taking a global
        // min/max would compare the biggest account against the smallest one.
        FollowerDelta followers = followerDelta(current);
        FollowerDelta followersPrev = followerDelta(previous);

        List<Dtos.SummaryCard> cards = List.of(
                card("followers", "Total Followers", followers.end(), followers.delta(followersPrev), "number"),
                card("likes", "Total Likes", now.likes(), Dtos.DeltaInfo.of(now.likes(), before.likes()), "number"),
                card("comments", "Total Comments", now.comments(), Dtos.DeltaInfo.of(now.comments(), before.comments()), "number"),
                card("engagement", "Engagement Rate", round2(now.engagementRate(before.engagementRate(0))),
                        Dtos.DeltaInfo.of(Math.round(now.engagementRate(0) * 100), Math.round(before.engagementRate(0) * 100)), "percent"),
                card("posts", "Total Posts", now.postsPublished(), Dtos.DeltaInfo.of(now.postsPublished(), before.postsPublished()), "number"),
                card("reach", "Total Reach", now.reach(), Dtos.DeltaInfo.of(now.reach(), before.reach()), "number"),
                card("impressions", "Impressions", now.impressions(), Dtos.DeltaInfo.of(now.impressions(), before.impressions()), "number"),
                card("shares", "Shares", now.shares(), Dtos.DeltaInfo.of(now.shares(), before.shares()), "number")
        );

        List<Dtos.PlatformBreakdown> breakdown = buildBreakdown(scoped, current, followers.end());

        List<Dtos.PostSummary> topPosts = postRepository
                .findTopByUserForChart(userId, platform, null, PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "likes")))
                .stream()
                .map(Dtos.PostSummary::from)
                .toList();

        return new Dtos.OverviewResponse(cards, breakdown, topPosts, window.range().key(),
                notificationRepository.countByUserIdAndReadFalse(userId));
    }

    /** Per-account first/last follower counts, summed across accounts. */
    private record FollowerDelta(long start, long end) {
        /** Growth as a fraction of the starting audience. */
        double rate() {
            return start <= 0 ? 0 : (end - start) / (double) start;
        }

        /**
         * Compares this window's growth rate against the previous window's. Using
         * absolute counts would always look like "growth" for an account that is
         * simply getting bigger, so the rates are what actually get compared.
         */
        Dtos.DeltaInfo delta(FollowerDelta previous) {
            double now = rate();
            double before = previous.rate();
            if (before == 0) {
                return new Dtos.DeltaInfo(end - start, 0, "flat");
            }
            long absolute = (long) Math.round((end - start) - (previous.end() - previous.start()));
            double pct = (now - before) / Math.abs(before) * 100.0;
            return new Dtos.DeltaInfo(absolute, round2(pct), pct > 0 ? "up" : pct < 0 ? "down" : "flat");
        }
    }

    private FollowerDelta followerDelta(List<MetricSnapshot> rows) {
        Map<Long, long[]> bounds = new java.util.LinkedHashMap<>();
        for (MetricSnapshot row : rows) {
            long[] b = bounds.computeIfAbsent(row.getAccount().getId(), k -> new long[]{Long.MAX_VALUE, Long.MIN_VALUE});
            b[0] = Math.min(b[0], row.getFollowers());
            b[1] = Math.max(b[1], row.getFollowers());
        }
        long start = bounds.values().stream().mapToLong(b -> b[0]).sum();
        long end = bounds.values().stream().mapToLong(b -> b[1]).sum();
        return new FollowerDelta(start, end);
    }

    @Transactional(readOnly = true)
    public Dtos.TimeSeriesResponse timeSeries(Long userId, String rangeKey, String platformRaw, String metricsRaw) {
        Platform platform = AccountService.parsePlatform(platformRaw);
        DateRanges.Window window = DateRanges.window(rangeKey);

        List<MetricSnapshot> rows = metricRepository.findRange(userId, window.from(), window.to(), platform);
        List<LocalDate> dates = window.from().datesUntil(window.to().plusDays(1)).toList();

        // One entry per date, aggregating every account selected by the filter.
        Map<LocalDate, List<MetricSnapshot>> byDate = rows.stream()
                .collect(Collectors.groupingBy(MetricSnapshot::getStatDate));

        List<Dtos.MetricPoint> points = new ArrayList<>(dates.size());
        for (LocalDate date : dates) {
            List<MetricSnapshot> forDay = byDate.getOrDefault(date, List.of());
            Totals t = Totals.of(forDay);
            // Followers is a stock, not a flow: take each account's newest value
            // for the day, then sum across the accounts in scope.
            Map<Long, Long> followersPerAccount = forDay.stream().collect(Collectors.toMap(
                    r -> r.getAccount().getId(), MetricSnapshot::getFollowers, Math::max));
            long followers = followersPerAccount.values().stream().mapToLong(Long::longValue).sum();
            points.add(new Dtos.MetricPoint(
                    date.toString(), followers, t.likes(), t.comments(), t.shares(), t.impressions(),
                    t.reach(), t.following(), round2(t.engagementRate(0)), t.profileVisits(), t.postsPublished()));
        }

        List<String> metrics = metricsRaw == null || metricsRaw.isBlank()
                ? List.of("followers", "likes", "comments", "engagementRate")
                : List.of(metricsRaw.split(",")).stream().map(String::trim).filter(s -> !s.isEmpty()).toList();

        return new Dtos.TimeSeriesResponse(points, metrics, window.range().key(), points.size());
    }

    @Transactional(readOnly = true)
    public Dtos.ReportResponse report(Long userId, String rangeKey, String platformRaw) {
        Platform platform = AccountService.parsePlatform(platformRaw);
        DateRanges.Window window = DateRanges.window(rangeKey);
        DateRanges.Window previousWindow = DateRanges.previous(window);

        List<SocialAccount> allAccounts = accountRepository.findByUserIdOrderByFollowersDesc(userId);
        List<SocialAccount> scoped = filterAccounts(allAccounts, platform);

        List<MetricSnapshot> rows = metricRepository.findRange(userId, window.from(), window.to(), platform);
        List<MetricSnapshot> before = metricRepository.findRange(userId, previousWindow.from(), previousWindow.to(), platform);

        Totals now = Totals.of(rows);
        Totals prev = Totals.of(before);

        FollowerDelta followers = followerDelta(rows);
        long followersEnd = followers.end();
        long followerGrowth = followers.end() - followers.start();

        List<Dtos.MetricPoint> trend = timeSeries(userId, rangeKey, platformRaw, "followers,likes,comments").points();

        String id = "rpt_" + rangeKeyOrDefault(rangeKey) + "_" + platformRawOrAll(platformRaw);

        return new Dtos.ReportResponse(
                id,
                window.range().key(),
                window.range().label(),
                window.from(),
                window.to(),
                now.reach(),
                now.impressions(),
                followersEnd,
                followerGrowth,
                now.likes(),
                now.comments(),
                now.shares(),
                round2(now.engagementRate(prev.engagementRate(0))),
                now.postsPublished(),
                buildBreakdown(scoped, rows, followersEnd),
                trend,
                java.time.Instant.now()
        );
    }

    private List<Dtos.PlatformBreakdown> buildBreakdown(List<SocialAccount> accounts,
                                                        List<MetricSnapshot> rows,
                                                        long totalFollowers) {
        if (accounts.isEmpty()) {
            return List.of();
        }
        Map<Long, List<MetricSnapshot>> byAccount = rows.stream()
                .collect(Collectors.groupingBy(r -> r.getAccount().getId()));

        return accounts.stream().map(account -> {
            List<MetricSnapshot> own = byAccount.getOrDefault(account.getId(), List.of());
            Totals t = Totals.of(own);
            long followers = own.stream().mapToLong(MetricSnapshot::getFollowers).max().orElse(account.getFollowers());
            return new Dtos.PlatformBreakdown(
                    account.getPlatform(),
                    account.getUsername(),
                    followers,
                    t.likes(),
                    t.comments(),
                    round2(t.engagementRate(0)),
                    totalFollowers == 0 ? 0 : round2(followers * 100.0 / totalFollowers)
            );
        }).collect(Collectors.toCollection(java.util.ArrayList::new));
    }

    private static List<SocialAccount> filterAccounts(List<SocialAccount> accounts, Platform platform) {
        if (platform == null) {
            return accounts;
        }
        return accounts.stream().filter(a -> a.getPlatform() == platform).toList();
    }

    private static Dtos.SummaryCard card(String key, String label, double value, Dtos.DeltaInfo delta, String format) {
        return new Dtos.SummaryCard(key, label, value, delta, format);
    }

    private static String rangeKeyOrDefault(String rangeKey) {
        return rangeKey == null || rangeKey.isBlank() ? "30d" : rangeKey.toLowerCase();
    }

    private static String platformRawOrAll(String platformRaw) {
        return platformRaw == null || platformRaw.isBlank() ? "all" : platformRaw.toLowerCase();
    }

    private static double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    /** Aggregated counters over a set of daily rows. */
    record Totals(long reach, long impressions, long likes, long comments, long shares,
                  long following, long profileVisits, long postsPublished) {

        static Totals of(List<MetricSnapshot> rows) {
            return new Totals(
                    rows.stream().mapToLong(MetricSnapshot::getReach).sum(),
                    rows.stream().mapToLong(MetricSnapshot::getImpressions).sum(),
                    rows.stream().mapToLong(MetricSnapshot::getLikes).sum(),
                    rows.stream().mapToLong(MetricSnapshot::getComments).sum(),
                    rows.stream().mapToLong(MetricSnapshot::getShares).sum(),
                    rows.stream().mapToLong(MetricSnapshot::getFollowing).max().orElse(0),
                    rows.stream().mapToLong(MetricSnapshot::getProfileVisits).sum(),
                    rows.stream().mapToLong(MetricSnapshot::getPostsPublished).sum()
            );
        }

        /** Interactions per impression, as a percentage. */
        double engagementRate(double fallback) {
            if (impressions <= 0) {
                return fallback;
            }
            return (likes + comments + shares) * 100.0 / impressions;
        }
    }
}
