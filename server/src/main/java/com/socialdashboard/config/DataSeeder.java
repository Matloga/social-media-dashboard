package com.socialdashboard.config;

import com.socialdashboard.entity.Notification;
import com.socialdashboard.entity.SocialAccount.Platform;
import com.socialdashboard.entity.User;
import com.socialdashboard.generator.SeededRandom;
import com.socialdashboard.repository.NotificationRepository;
import com.socialdashboard.repository.SocialAccountRepository;
import com.socialdashboard.repository.UserRepository;
import com.socialdashboard.service.AccountService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Creates the demo account on first boot so the dashboard has something to show.
 * Disable with SEED_ENABLED=false in production.
 */
@Component
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true")
public class DataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);
    private static final String DEMO_EMAIL = "demo@socialdashboard.dev";
    private static final String DEMO_PASSWORD = "demo12345";

    private final UserRepository userRepository;
    private final SocialAccountRepository accountRepository;
    private final NotificationRepository notificationRepository;
    private final AccountService accountService;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(UserRepository userRepository,
                      SocialAccountRepository accountRepository,
                      NotificationRepository notificationRepository,
                      AccountService accountService,
                      PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
        this.notificationRepository = notificationRepository;
        this.accountService = accountService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(org.springframework.boot.ApplicationArguments args) {
        if (userRepository.existsByEmailIgnoreCase(DEMO_EMAIL)) {
            log.info("Demo data already present, skipping seed");
            return;
        }

        log.info("Seeding demo account {}", DEMO_EMAIL);
        User demo = new User(DEMO_EMAIL, passwordEncoder.encode(DEMO_PASSWORD), "Demo Creator");
        demo.setBio("Social media dashboard demo account with a year of generated history.");
        userRepository.save(demo);

        List<Platform> platforms = List.of(Platform.INSTAGRAM, Platform.TIKTOK, Platform.X, Platform.LINKEDIN);
        for (Platform platform : platforms) {
            String username = switch (platform) {
                case INSTAGRAM -> "creator";
                case TIKTOK -> "creator_daily";
                case X -> "creatorhq";
                case LINKEDIN -> "creator-insights";
                case YOUTUBE -> "creatorstudio";
                case FACEBOOK -> "creatorpage";
            };
            var account = new com.socialdashboard.entity.SocialAccount(demo, platform, username, 0, 0);
            account.setStatus(com.socialdashboard.entity.SocialAccount.AccountStatus.ACTIVE);
            account.setConnectedAt(Instant.now().minus(java.time.Duration.ofDays(400)));
            account.setStatusMessage("Connected and syncing");
            accountRepository.save(account);
            accountService.backfillHistory(account);
        }

        seedNotifications(demo);
        log.info("Seeded {} accounts for {}", accountRepository.countByUserId(demo.getId()), DEMO_EMAIL);
        log.info("Demo login: {} / {}", DEMO_EMAIL, DEMO_PASSWORD);
    }

    private void seedNotifications(User demo) {
        SeededRandom rng = new SeededRandom(424242L, java.time.LocalDate.now());
        String[][] templates = {
                {"MILESTONE", "You crossed 100K followers", "Your combined audience passed 100,000. That is a big milestone."},
                {"FOLLOWER_GAIN", "Follower spike detected", "Your TikTok account gained unusually high followers in the last 24 hours."},
                {"NEW_COMMENT", "New comment mentions you", "A post received a comment asking for a collaboration."},
                {"REPORT_READY", "Weekly report ready", "Your weekly analytics summary is ready to view."},
                {"ACCOUNT_ISSUE", "Token expiring soon", "The LinkedIn access token expires in under 7 days. Reconnect to avoid a gap in data."},
                {"MILESTONE", "Best post this month", "One of your posts reached 3.2x your usual engagement."},
        };

        for (int i = 0; i < templates.length; i++) {
            Notification.NotificationType type =
                    Notification.NotificationType.valueOf(templates[i][0]);
            Instant createdAt = Instant.now().minusSeconds(rng.between(600, 6 * 24 * 3600L));
            Notification n = new Notification(demo, type, templates[i][1], templates[i][2], "/notifications");
            n.setCreatedAt(createdAt);
            n.setRead(i >= 4);
            notificationRepository.save(n);
        }
    }
}
