package com.socialdashboard.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "social_accounts", indexes = {
        @Index(name = "idx_accounts_user", columnList = "user_id")
})
public class SocialAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Platform platform;

    @Column(nullable = false, length = 80)
    private String username;

    @Column(nullable = false)
    private long followers;

    @Column(nullable = false)
    private long following;

    @Column(length = 255)
    private String externalAccountId;

    @Column(nullable = false)
    private AccountStatus status = AccountStatus.ACTIVE;

    @Column(length = 300)
    private String statusMessage;

    @Column(nullable = false)
    private Instant connectedAt = Instant.now();

    @Column(nullable = false)
    private Instant lastSyncedAt = Instant.now();

    @Column(nullable = false)
    private boolean tokenValid = true;

    public SocialAccount() {
    }

    public SocialAccount(User user, Platform platform, String username, long followers, long following) {
        this.user = user;
        this.platform = platform;
        this.username = username;
        this.followers = followers;
        this.following = following;
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

    public Platform getPlatform() {
        return platform;
    }

    public void setPlatform(Platform platform) {
        this.platform = platform;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
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

    public String getExternalAccountId() {
        return externalAccountId;
    }

    public void setExternalAccountId(String externalAccountId) {
        this.externalAccountId = externalAccountId;
    }

    public AccountStatus getStatus() {
        return status;
    }

    public void setStatus(AccountStatus status) {
        this.status = status;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public void setStatusMessage(String statusMessage) {
        this.statusMessage = statusMessage;
    }

    public Instant getConnectedAt() {
        return connectedAt;
    }

    public void setConnectedAt(Instant connectedAt) {
        this.connectedAt = connectedAt;
    }

    public Instant getLastSyncedAt() {
        return lastSyncedAt;
    }

    public void setLastSyncedAt(Instant lastSyncedAt) {
        this.lastSyncedAt = lastSyncedAt;
    }

    public boolean isTokenValid() {
        return tokenValid;
    }

    public void setTokenValid(boolean tokenValid) {
        this.tokenValid = tokenValid;
    }

    public enum Platform {
        INSTAGRAM, X, FACEBOOK, LINKEDIN, TIKTOK, YOUTUBE
    }

    public enum AccountStatus {
        ACTIVE, WARNING, ERROR, DISCONNECTED
    }
}
