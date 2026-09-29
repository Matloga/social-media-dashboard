package com.socialdashboard.entity;

import jakarta.persistence.*;

import java.time.LocalDate;

/**
 * One row per account per day. Holds the raw counters for that day so the
 * dashboard can plot trends and compute deltas without scanning every post.
 */
@Entity
@Table(name = "metric_snapshots", uniqueConstraints = {
        @UniqueConstraint(name = "uk_snapshot_account_date", columnNames = {"account_id", "stat_date"})
}, indexes = {
        @Index(name = "idx_snapshot_date", columnList = "stat_date")
})
public class MetricSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private SocialAccount account;

    @Column(name = "stat_date", nullable = false)
    private LocalDate statDate;

    @Column(nullable = false)
    private long followers;

    @Column(nullable = false)
    private long following;

    @Column(nullable = false)
    private long impressions;

    @Column(nullable = false)
    private long reach;

    @Column(nullable = false)
    private long likes;

    @Column(nullable = false)
    private long comments;

    @Column(nullable = false)
    private long shares;

    @Column(nullable = false)
    private long profileVisits;

    @Column(nullable = false)
    private long postsPublished;

    public MetricSnapshot() {
    }

    public MetricSnapshot(SocialAccount account, LocalDate statDate) {
        this.account = account;
        this.statDate = statDate;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public SocialAccount getAccount() {
        return account;
    }

    public void setAccount(SocialAccount account) {
        this.account = account;
    }

    public LocalDate getStatDate() {
        return statDate;
    }

    public void setStatDate(LocalDate statDate) {
        this.statDate = statDate;
    }

    public long getFollowers() {
        return followers;
    }

    public void setFollowers(long followers) {
        this.followers = followers;
    }

    public long getFollowing() {
        return following;
    }

    public void setFollowing(long following) {
        this.following = following;
    }

    public long getImpressions() {
        return impressions;
    }

    public void setImpressions(long impressions) {
        this.impressions = impressions;
    }

    public long getReach() {
        return reach;
    }

    public void setReach(long reach) {
        this.reach = reach;
    }

    public long getLikes() {
        return likes;
    }

    public void setLikes(long likes) {
        this.likes = likes;
    }

    public long getComments() {
        return comments;
    }

    public void setComments(long comments) {
        this.comments = comments;
    }

    public long getShares() {
        return shares;
    }

    public void setShares(long shares) {
        this.shares = shares;
    }

    public long getProfileVisits() {
        return profileVisits;
    }

    public void setProfileVisits(long profileVisits) {
        this.profileVisits = profileVisits;
    }

    public long getPostsPublished() {
        return postsPublished;
    }

    public void setPostsPublished(long postsPublished) {
        this.postsPublished = postsPublished;
    }
}
