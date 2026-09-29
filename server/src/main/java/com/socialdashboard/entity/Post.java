package com.socialdashboard.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "posts", indexes = {
        @Index(name = "idx_posts_user", columnList = "user_id"),
        @Index(name = "idx_posts_published", columnList = "publishedAt")
})
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id")
    private SocialAccount account;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SocialAccount.Platform platform;

    @Column(nullable = false, length = 1000)
    private String caption;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PostType type = PostType.IMAGE;

    @Column(nullable = false)
    private long likes;

    @Column(nullable = false)
    private long comments;

    @Column(nullable = false)
    private long shares;

    @Column(nullable = false)
    private long impressions;

    @Column(nullable = false)
    private long reach;

    @Column(length = 400)
    private String thumbnailUrl;

    @Column(length = 200)
    private String permalink;

    @Column(nullable = false)
    private Instant publishedAt = Instant.now();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "post_hashtags", joinColumns = @JoinColumn(name = "post_id"))
    @Column(name = "hashtag", length = 60)
    private List<String> hashtags = new ArrayList<>();

    public Post() {
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

    public SocialAccount getAccount() {
        return account;
    }

    public void setAccount(SocialAccount account) {
        this.account = account;
    }

    public SocialAccount.Platform getPlatform() {
        return platform;
    }

    public void setPlatform(SocialAccount.Platform platform) {
        this.platform = platform;
    }

    public String getCaption() {
        return caption;
    }

    public void setCaption(String caption) {
        this.caption = caption;
    }

    public PostType getType() {
        return type;
    }

    public void setType(PostType type) {
        this.type = type;
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

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public void setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }

    public String getPermalink() {
        return permalink;
    }

    public void setPermalink(String permalink) {
        this.permalink = permalink;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(Instant publishedAt) {
        this.publishedAt = publishedAt;
    }

    public List<String> getHashtags() {
        return hashtags;
    }

    public void setHashtags(List<String> hashtags) {
        this.hashtags = hashtags;
    }

    public enum PostType {
        IMAGE, VIDEO, CAROUSEL, STORY, TEXT, REEL, SHORT, LIVE
    }
}
