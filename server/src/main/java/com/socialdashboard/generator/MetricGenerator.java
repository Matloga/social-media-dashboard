package com.socialdashboard.generator;

import com.socialdashboard.entity.MetricSnapshot;
import com.socialdashboard.entity.SocialAccount;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Synthesises believable daily counters for an account: compounding follower
 * growth, weekly seasonality, occasional viral spikes and random churn.
 * Deterministic per (username, platform, from) so charts stay consistent.
 */
public class MetricGenerator {

    /** Autocorrelation of the day-to-day noise. 0 = pure random walk, 1 = sticky. */
    private static final double MEAN_REVERSION = 0.35;

    public record Profile(
            double dailyFollowerGrowth,
            double followerVolatility,
            double likeRate,
            double commentRate,
            double shareRate,
            double reachToImpressionRatio,
            double spikeChance,
            double spikeMultiplier,
            double weekendLift
    ) {
    }

    private static final Map<SocialAccount.Platform, Profile> PROFILES = Map.of(
            SocialAccount.Platform.INSTAGRAM, new Profile(0.0042, 0.10, 0.062, 0.0085, 0.0041, 0.42, 0.050, 2.00, 1.06),
            SocialAccount.Platform.X,         new Profile(0.0021, 0.14, 0.041, 0.0062, 0.0135, 0.38, 0.040, 2.10, 0.94),
            SocialAccount.Platform.FACEBOOK,  new Profile(0.0009, 0.08, 0.034, 0.0071, 0.0022, 0.33, 0.030, 1.70, 1.04),
            SocialAccount.Platform.LINKEDIN, new Profile(0.0016, 0.07, 0.028, 0.0094, 0.0031, 0.29, 0.020, 1.60, 0.82),
            SocialAccount.Platform.TIKTOK,   new Profile(0.0075, 0.18, 0.118, 0.0152, 0.0210, 0.55, 0.060, 2.40, 1.12),
            SocialAccount.Platform.YOUTUBE,  new Profile(0.0028, 0.12, 0.052, 0.0048, 0.0067, 0.44, 0.035, 2.20, 1.09)
    );

    /** Log-scale day-to-day noise on reach. Multiplicative, so it never goes negative. */
    private static final double REACH_NOISE = 0.22;

    public List<MetricSnapshot> generate(SocialAccount account, LocalDate from, LocalDate to) {
        Profile profile = PROFILES.getOrDefault(account.getPlatform(), PROFILES.get(SocialAccount.Platform.INSTAGRAM));
        SeededRandom rng = new SeededRandom(
                account.getUsername().hashCode() * 7919L + account.getPlatform().ordinal() * 104729L, from);

        int days = (int) ChronoUnit.DAYS.between(from, to) + 1;
        if (days <= 0) {
            return List.of();
        }
        List<MetricSnapshot> rows = new ArrayList<>(days);

        double[] followerMultiplier = new double[days];
        double[] seasonalityArr = new double[days];
        boolean[] viralArr = new boolean[days];

        // Pass 1: simulate the relative shape of the series. followerMultiplier[0] is
        // always 1.0; later days compound. Knowing the shape up front lets us rescale
        // it in pass 2 so the newest day lands exactly on the account's real count,
        // which keeps the headline card consistent with the chart.
        double multiplier = 1.0;
        double noise = 0;
        for (int i = 0; i < days; i++) {
            LocalDate date = from.plusDays(i);
            DayOfWeek dow = date.getDayOfWeek();
            boolean weekend = dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY;
            double seasonality = (weekend ? profile.weekendLift() : 1.0) * (dow == DayOfWeek.WEDNESDAY ? 1.06 : 1.0);
            boolean viral = rng.chance(profile.spikeChance());

            seasonalityArr[i] = seasonality;
            viralArr[i] = viral;
            followerMultiplier[i] = multiplier;

            if (i < days - 1) {
                // AR(1) noise. A plain random walk would accumulate a permanent
                // drift over 400 days, and because the series is rescaled to land on
                // today's follower count, that drift gets amplified into a fake
                // growth surge in recent weeks. Mean reversion keeps the day-to-day
                // wobble but stops it from trending.
                double sigma = profile.followerVolatility() * 0.05;
                noise = MEAN_REVERSION * noise
                        + rng.gaussian(0, sigma * Math.sqrt(1 - MEAN_REVERSION * MEAN_REVERSION));
                double netGrowth = profile.dailyFollowerGrowth() * seasonality * (viral ? 1.9 : 1.0) + noise;
                multiplier = Math.max(0.5, multiplier * (1 + netGrowth));
            }
        }
        double finalMultiplier = followerMultiplier[days - 1];
        double startFollowers = account.getFollowers() / Math.max(finalMultiplier, 1e-6);

        double startImpressions = Math.max(400, startFollowers * profile.reachToImpressionRatio() * 2.4);
        double startProfileVisits = Math.max(20, startFollowers * 0.006);
        double startFollowing = Math.max(50, account.getFollowing() * 0.85);

        // Pass 2: expand the shape into concrete daily counters.
        for (int i = 0; i < days; i++) {
            LocalDate date = from.plusDays(i);
            double seasonality = seasonalityArr[i];
            boolean viral = viralArr[i];
            double viralFactor = viral ? 1.0 + (profile.spikeMultiplier() - 1.0) * rng.nextDouble() : 1.0;

            double followers = Math.max(20, startFollowers * followerMultiplier[i]);
            double impressions = Math.max(300, startImpressions * followerMultiplier[i]);
            double profileVisits = Math.max(5, startProfileVisits * followerMultiplier[i]);
            double following = startFollowing * (0.995 + rng.nextDouble() * 0.012) + rng.gaussian(0, 2.5);

            // Multiplicative noise keeps daily reach right-skewed like real data
            // while the window average stays stable, because E[exp(z)] is a
            // constant. Additive noise with a large std instead dragged the
            // median far below the mean, so short windows swung wildly.
            double baseReach = impressions * profile.reachToImpressionRatio() * seasonality;
            double reach = Math.max(20, baseReach * Math.exp(rng.gaussian(0, REACH_NOISE)) * viralFactor);
            double dailyImpressions = Math.max(reach,
                    reach / profile.reachToImpressionRatio() * (0.94 + rng.nextDouble() * 0.14));

            // Engagement scales with reach, so it inherits the same trend.
            long likes = Math.max(0, Math.round(reach * profile.likeRate() * seasonality * (0.85 + rng.nextDouble() * 0.3) * viralFactor));
            long comments = Math.max(0, Math.round(reach * profile.commentRate() * seasonality * (0.80 + rng.nextDouble() * 0.4) * viralFactor));
            long shares = Math.max(0, Math.round(reach * profile.shareRate() * seasonality * (0.80 + rng.nextDouble() * 0.4) * viralFactor));

            MetricSnapshot row = new MetricSnapshot(account, date);
            row.setFollowers(Math.round(followers));
            row.setFollowing(Math.round(Math.max(30, following)));
            row.setImpressions(Math.round(dailyImpressions));
            row.setReach(Math.round(reach));
            row.setLikes(likes);
            row.setComments(comments);
            row.setShares(shares);
            row.setProfileVisits(Math.round(profileVisits * seasonality * (0.80 + rng.nextDouble() * 0.45)));
            row.setPostsPublished(rng.chance(0.28) ? (rng.chance(0.15) ? 2 : 1) : 0);
            rows.add(row);
        }

        return rows;
    }
}
