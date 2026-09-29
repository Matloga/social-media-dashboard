package com.socialdashboard.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.time.LocalDate;

/**
 * A numeric target a user sets for themselves, e.g. "gain 2,000 followers this
 * month". Progress is always derived from the metric snapshots at read time
 * rather than stored, so it stays correct after a re-sync.
 */
@Entity
@Table(name = "goals", indexes = {
        @Index(name = "idx_goal_user", columnList = "user_id"),
        @Index(name = "idx_goal_active", columnList = "user_id, active")
})
public class Goal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private GoalMetric metric = GoalMetric.FOLLOWERS;

    /** Which platform the target applies to, or null for the whole audience. */
    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private SocialAccount.Platform platform;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false)
    private double targetValue;

    @Column(nullable = false, length = 20)
    private Period period = Period.MONTHLY;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(nullable = false)
    private boolean active = true;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    public Goal() {
    }

    public Goal(User user, GoalMetric metric, SocialAccount.Platform platform, String title,
                double targetValue, Period period, LocalDate startDate, LocalDate endDate) {
        this.user = user;
        this.metric = metric;
        this.platform = platform;
        this.title = title;
        this.targetValue = targetValue;
        this.period = period;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public GoalMetric getMetric() {
        return metric;
    }

    public void setMetric(GoalMetric metric) {
        this.metric = metric;
    }

    public SocialAccount.Platform getPlatform() {
        return platform;
    }

    public void setPlatform(SocialAccount.Platform platform) {
        this.platform = platform;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public double getTargetValue() {
        return targetValue;
    }

    public void setTargetValue(double targetValue) {
        this.targetValue = targetValue;
    }

    public Period getPeriod() {
        return period;
    }

    public void setPeriod(Period period) {
        this.period = period;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    /** Metrics a goal can be set against. */
    public enum GoalMetric {
        FOLLOWERS, LIKES, COMMENTS, ENGAGEMENT_RATE, REACH, POSTS
    }

    public enum Period {
        WEEKLY, MONTHLY, QUARTERLY;

        public int days() {
            return switch (this) {
                case WEEKLY -> 7;
                case MONTHLY -> 30;
                case QUARTERLY -> 90;
            };
        }
    }
}
