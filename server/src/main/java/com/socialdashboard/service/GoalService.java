package com.socialdashboard.service;

import com.socialdashboard.dto.Dtos;
import com.socialdashboard.entity.Goal;
import com.socialdashboard.entity.Goal.GoalMetric;
import com.socialdashboard.entity.Goal.Period;
import com.socialdashboard.entity.MetricSnapshot;
import com.socialdashboard.entity.SocialAccount.Platform;
import com.socialdashboard.entity.User;
import com.socialdashboard.exception.ApiException;
import com.socialdashboard.repository.GoalRepository;
import com.socialdashboard.repository.MetricSnapshotRepository;
import com.socialdashboard.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Goal targets and their live progress. Nothing about progress is stored: it is
 * recomputed from the daily snapshots every time the goals are read, so a
 * re-sync can never leave a stale percentage on screen.
 */
@Service
public class GoalService {

    private final GoalRepository goalRepository;
    private final MetricSnapshotRepository metricRepository;
    private final UserRepository userRepository;

    public GoalService(GoalRepository goalRepository,
                       MetricSnapshotRepository metricRepository,
                       UserRepository userRepository) {
        this.goalRepository = goalRepository;
        this.metricRepository = metricRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<Dtos.GoalResponse> list(Long userId) {
        return goalRepository.findByUserIdOrderByActiveDescCreatedAtDesc(userId).stream()
                .map(goal -> toResponse(userId, goal))
                .toList();
    }

    @Transactional
    public Dtos.GoalResponse create(Long userId, Dtos.CreateGoalRequest request) {
        Platform platform = AccountService.parsePlatform(request.platform());
        User user = userRepository.findById(userId).orElseThrow(() -> ApiException.notFound("User"));

        double target = request.targetValue();
        if (target <= 0) {
            throw new ApiException(org.springframework.http.HttpStatus.BAD_REQUEST,
                    "Target must be greater than zero", "targetValue");
        }

        Period period = request.period() == null ? Period.MONTHLY : request.period();
        // The default window trails today rather than starting today, because a
        // window that began today would report no progress at all until the
        // future arrived. A trailing window is measurable straight away, and a
        // caller can still pass explicit dates to plan a future period.
        // minusDays(days - 1) because both endpoints are inclusive, so a
        // "monthly" goal is exactly 30 days rather than 31.
        LocalDate end = request.endDate() == null
                ? LocalDate.now()
                : request.endDate();
        if (end.isBefore(request.startDate() == null
                ? LocalDate.now().minusDays(period.days() - 1L)
                : request.startDate())) {
            throw new ApiException(org.springframework.http.HttpStatus.BAD_REQUEST,
                    "End date cannot be before the start date", "endDate");
        }
        LocalDate start = request.startDate() == null
                ? LocalDate.now().minusDays(period.days() - 1L)
                : request.startDate();
        if (start.isAfter(end)) {
            throw new ApiException(org.springframework.http.HttpStatus.BAD_REQUEST,
                    "Start date must be before the end date", "startDate");
        }

        String title = request.title() == null || request.title().isBlank()
                ? defaultTitle(request.metric(), period)
                : request.title().trim();

        Goal goal = new Goal(user, request.metric(), platform, title, target, period, start, end);
        return toResponse(userId, goalRepository.save(goal));
    }

    @Transactional
    public Dtos.GoalResponse update(Long userId, Long goalId, Dtos.UpdateGoalRequest request) {
        Goal goal = requireOwned(userId, goalId);
        if (request.title() != null && !request.title().isBlank()) {
            goal.setTitle(request.title().trim());
        }
        if (request.targetValue() != null) {
            if (request.targetValue() <= 0) {
                throw new ApiException(org.springframework.http.HttpStatus.BAD_REQUEST,
                        "Target must be greater than zero", "targetValue");
            }
            goal.setTargetValue(request.targetValue());
        }
        if (request.endDate() != null) {
            goal.setEndDate(request.endDate());
        }
        if (request.active() != null) {
            goal.setActive(request.active());
        }
        return toResponse(userId, goalRepository.save(goal));
    }

    @Transactional
    public void delete(Long userId, Long goalId) {
        goalRepository.delete(requireOwned(userId, goalId));
    }

    private Goal requireOwned(Long userId, Long goalId) {
        return goalRepository.findById(goalId)
                .filter(g -> g.getUser().getId().equals(userId))
                .orElseThrow(() -> ApiException.notFound("Goal"));
    }

    private Dtos.GoalResponse toResponse(Long userId, Goal goal) {
        List<MetricSnapshot> rows = metricRepository.findWindowAscending(
                userId, goal.getStartDate(), goal.getEndDate(), goal.getPlatform());

        double baseline = baseline(goal, rows);
        double current = current(goal.getMetric(), rows);
        double progress = goal.getTargetValue() <= 0 ? 0 : (current - baseline) / goal.getTargetValue() * 100.0;
        progress = round1(Math.max(0, progress));

        long daysTotal = java.time.temporal.ChronoUnit.DAYS.between(goal.getStartDate(), goal.getEndDate()) + 1;
        // Counts the days that have actually happened. A goal starting in the
        // future is not yet in progress, so this stays 0 until the start date.
        long daysElapsed = java.time.temporal.ChronoUnit.DAYS.between(
                goal.getStartDate(),
                LocalDate.now().isBefore(goal.getEndDate()) ? LocalDate.now() : goal.getEndDate()) + 1;
        daysElapsed = Math.min(daysTotal, Math.max(0, daysElapsed));
        // Straight-line pace: how far along the calendar we are, 0-100. Rounded
        // so the client never receives a raw repeating decimal.
        double timeProgress = daysTotal <= 0 ? 100.0 : round1(daysElapsed * 100.0 / daysTotal);

        // Positive means ahead of the pace required to hit the target in time.
        double paceDelta = round1(progress - timeProgress);
        // "BEHIND" is the worse label, so it must be tested first. A 1 point
        // shortfall is at risk; a 10 point shortfall is genuinely behind.
        // A window ending today is not over yet, so only a window whose end date
        // has passed counts as expired.
        boolean windowClosed = LocalDate.now().isAfter(goal.getEndDate());
        String status = progress >= 100 ? "ACHIEVED"
                : windowClosed ? "EXPIRED"
                : paceDelta <= -10 ? "BEHIND"
                : paceDelta < 0 ? "AT_RISK"
                : "ON_TRACK";

        return new Dtos.GoalResponse(
                goal.getId(),
                goal.getTitle(),
                goal.getMetric().name(),
                goal.getPlatform() == null ? null : goal.getPlatform().name(),
                goal.getTargetValue(),
                round1(current - baseline),
                round1(current),
                baseline,
                progress,
                timeProgress,
                paceDelta,
                status,
                goal.getPeriod().name(),
                goal.getStartDate(),
                goal.getEndDate(),
                daysElapsed,
                daysTotal,
                goal.isActive(),
                goal.getCreatedAt()
        );
    }

    /**
     * Value at the start of the window. For a stock metric such as followers this
     * is each account's own first day, summed, so the goal measures growth. For a
     * flow metric such as likes it is zero, so the target measures the raw total.
     */
    private double baseline(Goal goal, List<MetricSnapshot> rows) {
        if (rows.isEmpty() || goal.getMetric() != GoalMetric.FOLLOWERS) {
            return 0;
        }
        // Sum each account's earliest day separately. Taking the first row of the
        // whole result set would only count a single account. On a tie the newer
        // row wins, which keeps the choice deterministic.
        java.util.Map<Long, MetricSnapshot> earliest = new java.util.LinkedHashMap<>();
        for (MetricSnapshot row : rows) {
            earliest.merge(row.getAccount().getId(), row,
                    (a, b) -> a.getStatDate().isAfter(b.getStatDate()) ? b : a);
        }
        return earliest.values().stream().mapToLong(MetricSnapshot::getFollowers).sum();
    }

    /**
     * Latest value for a stock metric, or the summed total for a flow metric.
     * Followers are summed across the accounts in scope, using each account's
     * own latest day so one account cannot mask another's growth.
     */
    private double current(GoalMetric metric, List<MetricSnapshot> rows) {
        if (rows.isEmpty()) {
            return 0;
        }
        return switch (metric) {
            case FOLLOWERS -> {
                // Latest day per account, not the maximum: an account that
                // briefly lost followers should not read as a larger audience.
                java.util.Map<Long, MetricSnapshot> latest = new java.util.LinkedHashMap<>();
                for (MetricSnapshot row : rows) {
                    latest.merge(row.getAccount().getId(), row,
                            (a, b) -> a.getStatDate().isBefore(b.getStatDate()) ? b : a);
                }
                yield latest.values().stream().mapToLong(MetricSnapshot::getFollowers).sum();
            }
            case LIKES -> rows.stream().mapToLong(MetricSnapshot::getLikes).sum();
            case COMMENTS -> rows.stream().mapToLong(MetricSnapshot::getComments).sum();
            case REACH -> rows.stream().mapToLong(MetricSnapshot::getReach).sum();
            case POSTS -> rows.stream().mapToLong(MetricSnapshot::getPostsPublished).sum();
            case ENGAGEMENT_RATE -> {
                double impressions = rows.stream().mapToLong(MetricSnapshot::getImpressions).sum();
                double interactions = rows.stream()
                        .mapToLong(r -> r.getLikes() + r.getComments() + r.getShares()).sum();
                yield impressions <= 0 ? 0 : interactions * 100.0 / impressions;
            }
        };
    }

    private String defaultTitle(GoalMetric metric, Period period) {
        String noun = switch (metric) {
            case FOLLOWERS -> "Followers";
            case LIKES -> "Likes";
            case COMMENTS -> "Comments";
            case ENGAGEMENT_RATE -> "Engagement rate";
            case REACH -> "Reach";
            case POSTS -> "Posts";
        };
        String cadence = switch (period) {
            case WEEKLY -> "Weekly";
            case MONTHLY -> "Monthly";
            case QUARTERLY -> "Quarterly";
        };
        return cadence + " " + noun.toLowerCase(Locale.ROOT) + " target";
    }

    private static double round1(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}
