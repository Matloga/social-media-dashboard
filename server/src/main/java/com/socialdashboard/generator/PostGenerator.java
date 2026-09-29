package com.socialdashboard.generator;

import com.socialdashboard.entity.Post;
import com.socialdashboard.entity.Post.PostType;
import com.socialdashboard.entity.SocialAccount;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

/** Builds realistic post records (captions, hashtags, counters) for an account. */
public class PostGenerator {

    private static final List<String> CAPTION_OPENERS = List.of(
            "New drop just landed", "Behind the scenes", "Three things nobody tells you about",
            "Okay this one is special", "Save this for later", "Unpopular opinion",
            "We did it", "A quiet reminder", "The process was the hardest part",
            "Six months of work in one frame", "This changed everything", "Small team, big energy",
            "Still thinking about this", "Everyone asked, so here it is", "Take your time with this one"
    );

    private static final List<String> CAPTION_BODIES = List.of(
            "Spent a month getting the details right and it shows in every frame.",
            "Turns out the simplest version was the hardest to make.",
            "Some things you only notice once you stop rushing them.",
            "The team showed up at 6am and I am so grateful for these people.",
            "Not the result we expected, but the one we needed.",
            "If you take one thing from this, make it the last paragraph.",
            "Comment below with the one thing you would change.",
            "There is a version of this that is perfect and this is not it.",
            "Honest breakdown of what worked and what did not."
    );

    private static final List<String> HASHTAGS = List.of(
            "contentcreator", "socialmediatips", "behindthescenes", "digitalmarketing",
            "creatoreconomy", "growthhacking", "brandbuilding", "storytelling",
            "workinpublic", "smallbusiness", "photography", "videography",
            "motivation", "entrepreneurship", "buildinpublic", "contentstrategy"
    );

    private static PostType typeFor(SocialAccount.Platform platform, SeededRandom rng) {
        return switch (platform) {
            case INSTAGRAM -> rng.pick(List.of(PostType.IMAGE, PostType.IMAGE, PostType.CAROUSEL, PostType.REEL, PostType.STORY));
            case TIKTOK -> rng.pick(List.of(PostType.VIDEO, PostType.VIDEO, PostType.REEL, PostType.IMAGE));
            case YOUTUBE -> rng.pick(List.of(PostType.VIDEO, PostType.VIDEO, PostType.SHORT, PostType.LIVE));
            case X -> rng.pick(List.of(PostType.TEXT, PostType.IMAGE, PostType.TEXT, PostType.VIDEO));
            case LINKEDIN -> rng.pick(List.of(PostType.TEXT, PostType.IMAGE, PostType.VIDEO, PostType.TEXT));
            case FACEBOOK -> rng.pick(List.of(PostType.IMAGE, PostType.VIDEO, PostType.LIVE, PostType.TEXT));
        };
    }

    public List<Post> generate(SocialAccount account, int count, LocalDate to, long avgReach) {
        SeededRandom rng = new SeededRandom(
                account.getUsername().hashCode() * 15485863L + account.getPlatform().ordinal() * 32452843L, to);
        List<Post> posts = new ArrayList<>(count);

        for (int i = 0; i < count; i++) {
            int daysBack = (int) Math.round(Math.pow(rng.nextDouble(), 1.7) * 180);
            LocalDate date = to.minusDays(daysBack);
            int hour = 8 + rng.nextInt(12);
            int minute = rng.nextInt(60);
            Instant publishedAt = date.atTime(hour, minute).toInstant(ZoneOffset.UTC);

            boolean viral = rng.chance(0.05);
            double reachFactor = viral ? 2.5 + rng.nextDouble() * 4 : 0.35 + rng.nextDouble() * 1.3;
            long reach = Math.max(80, Math.round(avgReach * reachFactor));

            Post post = new Post();
            post.setUser(account.getUser());
            post.setAccount(account);
            post.setPlatform(account.getPlatform());
            post.setType(typeFor(account.getPlatform(), rng));
            post.setCaption(rng.pick(CAPTION_OPENERS) + ". " + rng.pick(CAPTION_BODIES) + ".");
            post.setPublishedAt(publishedAt);
            post.setReach(reach);
            post.setImpressions(Math.round(reach * (1.1 + rng.nextDouble() * 0.9)));
            post.setLikes(Math.round(reach * (0.02 + rng.nextDouble() * 0.09)));
            post.setComments(Math.round(reach * (0.002 + rng.nextDouble() * 0.012)));
            post.setShares(Math.round(reach * (0.001 + rng.nextDouble() * 0.014)));
            post.setPermalink("https://" + account.getPlatform().name().toLowerCase() + ".example/@"
                    + account.getUsername() + "/" + (900000L + rng.between(1, 99999)));

            List<String> tags = new ArrayList<>();
            int tagCount = 2 + rng.nextInt(4);
            for (int t = 0; t < tagCount; t++) {
                String tag = rng.pick(HASHTAGS);
                if (!tags.contains(tag)) {
                    tags.add(tag);
                }
            }
            post.setHashtags(tags);
            posts.add(post);
        }
        return posts;
    }
}
