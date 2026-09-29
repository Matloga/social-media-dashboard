package com.socialdashboard.service;

import com.socialdashboard.adapter.PlatformAdapter;
import com.socialdashboard.adapter.PlatformAdapterRegistry;
import com.socialdashboard.dto.Dtos;
import com.socialdashboard.entity.MetricSnapshot;
import com.socialdashboard.entity.Notification;
import com.socialdashboard.entity.Post;
import com.socialdashboard.entity.SocialAccount;
import com.socialdashboard.entity.SocialAccount.AccountStatus;
import com.socialdashboard.entity.SocialAccount.Platform;
import com.socialdashboard.entity.User;
import com.socialdashboard.exception.ApiException;
import com.socialdashboard.generator.MetricGenerator;
import com.socialdashboard.generator.PostGenerator;
import com.socialdashboard.generator.SeededRandom;
import com.socialdashboard.repository.MetricSnapshotRepository;
import com.socialdashboard.repository.NotificationRepository;
import com.socialdashboard.repository.PostRepository;
import com.socialdashboard.repository.SocialAccountRepository;
import com.socialdashboard.repository.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AccountService {

    private static final int HISTORY_DAYS = 400;
    private static final int SEED_POSTS_PER_ACCOUNT = 90;

    private final SocialAccountRepository accountRepository;
    private final PostRepository postRepository;
    private final MetricSnapshotRepository metricRepository;
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final PlatformAdapterRegistry adapters;

    private final MetricGenerator metricGenerator = new MetricGenerator();
    private final PostGenerator postGenerator = new PostGenerator();

    public AccountService(SocialAccountRepository accountRepository,
                          PostRepository postRepository,
                          MetricSnapshotRepository metricRepository,
                          NotificationRepository notificationRepository,
                          UserRepository userRepository,
                          PlatformAdapterRegistry adapters) {
        this.accountRepository = accountRepository;
        this.postRepository = postRepository;
        this.metricRepository = metricRepository;
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.adapters = adapters;
    }

    @Transactional(readOnly = true)
    public List<Dtos.AccountResponse> list(Long userId) {
        List<SocialAccount> accounts = accountRepository.findByUserIdOrderByFollowersDesc(userId);
        return accounts.stream().map(account -> {
            List<Post> top = postRepository.findTopByUserForChart(userId, account.getPlatform(), null,
                    PageRequest.of(0, 3));
            return Dtos.AccountResponse.from(account, top.stream().map(Dtos.PostSummary::from).toList());
        }).toList();
    }

    @Transactional
    public Dtos.AccountResponse connect(Long userId, Dtos.CreateAccountRequest request) {
        Platform platform = parsePlatform(request.platform());
        if (platform == null) {
            throw new ApiException(org.springframework.http.HttpStatus.BAD_REQUEST,
                    "Platform is required", "platform");
        }
        String username = request.username().trim().replaceFirst("^@", "");
        if (username.isEmpty()) {
            throw new ApiException(org.springframework.http.HttpStatus.BAD_REQUEST, "Username is required", "username");
        }
        if (accountRepository.existsByUserIdAndPlatformAndUsernameIgnoreCase(userId, platform, username)) {
            throw ApiException.conflict("That " + platform.name().toLowerCase()
                    + " account is already connected");
        }

        User user = userRepository.findById(userId).orElseThrow(() -> ApiException.notFound("User"));
        SocialAccount account = new SocialAccount(user, platform, username, 0, 0);
        account.setStatus(AccountStatus.ACTIVE);
        account.setConnectedAt(Instant.now());
        account.setLastSyncedAt(Instant.now());
        account.setStatusMessage("Connected, waiting for first sync");
        accountRepository.save(account);

        backfillHistory(account);

        PlatformAdapter adapter = adapters.get(account.getPlatform());
        PlatformAdapter.AccountHealth health = adapter.checkHealth(account);
        applyHealth(account, health);
        accountRepository.save(account);

        if (health.status() == AccountStatus.ACTIVE) {
            notificationRepository.save(new Notification(user, Notification.NotificationType.MILESTONE,
                    "Account connected",
                    adapter.displayName() + " account @" + username + " is now syncing.",
                    "/accounts"));
        } else {
            notificationRepository.save(new Notification(user, Notification.NotificationType.ACCOUNT_ISSUE,
                    "Connection needs attention", health.message(), "/accounts"));
        }
        return Dtos.AccountResponse.from(account, List.of());
    }

    @Transactional
    public Dtos.AccountResponse update(Long userId, Long accountId, Dtos.UpdateAccountRequest request) {
        SocialAccount account = requireOwned(userId, accountId);
        if (request.username() != null && !request.username().isBlank()) {
            account.setUsername(request.username().trim().replaceFirst("^@", ""));
        }
        if (request.status() != null) {
            account.setStatus(request.status());
            account.setStatusMessage(switch (request.status()) {
                case ACTIVE -> "Manually resumed";
                case WARNING -> "Marked for review";
                case ERROR -> "Manually marked as errored";
                case DISCONNECTED -> "Manually disconnected";
            });
        }
        accountRepository.save(account);
        return Dtos.AccountResponse.from(account, List.of());
    }

    @Transactional
    public void disconnect(Long userId, Long accountId) {
        SocialAccount account = requireOwned(userId, accountId);
        postRepository.deleteAllByAccountId(accountId);
        metricRepository.deleteByAccountId(accountId);
        accountRepository.delete(account);
    }

    /** Re-pull history for one account, or all of them when accountId is null. */
    @Transactional
    public Dtos.AccountResponse sync(Long userId, Long accountId) {
        List<SocialAccount> targets = accountId == null
                ? accountRepository.findByUserIdOrderByFollowersDesc(userId)
                : List.of(requireOwned(userId, accountId));

        for (SocialAccount account : targets) {
            PlatformAdapter adapter = adapters.get(account.getPlatform());
            LocalDate from = LocalDate.now().minusDays(HISTORY_DAYS);
            LocalDate to = LocalDate.now();

            // Clear the previous pull first, otherwise every sync would stack
            // another 90 duplicate posts and 400 duplicate snapshots on top.
            metricRepository.deleteByAccountId(account.getId());
            postRepository.deleteAllByAccountId(account.getId());

            List<MetricSnapshot> history = adapter.fetchHistory(account, from, to);
            metricRepository.saveAll(history);

            long avgReach = Math.round(history.stream().mapToLong(MetricSnapshot::getReach).average().orElse(500));
            postRepository.saveAll(postGenerator.generate(account, SEED_POSTS_PER_ACCOUNT, to, avgReach));

            account.setLastSyncedAt(Instant.now());
            PlatformAdapter.AccountHealth health = adapter.checkHealth(account);
            applyHealth(account, health);
            accountRepository.save(account);
        }
        return accountId == null
                ? new Dtos.AccountResponse(-1L, null, "All accounts", "All", 0, 0, AccountStatus.ACTIVE,
                "Sync completed for " + targets.size() + " account(s)", true, Instant.now(), Instant.now(), List.of())
                : Dtos.AccountResponse.from(targets.get(0), List.of());
    }

    /** Create the metric + post history for a freshly connected account. */
    public void backfillHistory(SocialAccount account) {
        SeededRandom rng = new SeededRandom(account.getUsername().hashCode() * 2654435761L, LocalDate.now());
        long seedFollowers = switch (account.getPlatform()) {
            case INSTAGRAM -> rng.between(2_400, 145_000);
            case X -> rng.between(800, 62_000);
            case FACEBOOK -> rng.between(400, 28_000);
            case LINKEDIN -> rng.between(300, 19_000);
            case TIKTOK -> rng.between(1_500, 240_000);
            case YOUTUBE -> rng.between(500, 88_000);
        };
        long seedFollowing = Math.max(50, Math.round(seedFollowers * (0.04 + rng.nextDouble() * 0.35)));

        account.setFollowers(seedFollowers);
        account.setFollowing(seedFollowing);

        LocalDate to = LocalDate.now();
        LocalDate from = to.minusDays(HISTORY_DAYS);
        List<MetricSnapshot> history = adapters.get(account.getPlatform()).fetchHistory(account, from, to);
        metricRepository.saveAll(history);

        long avgReach = Math.round(history.stream().mapToLong(MetricSnapshot::getReach).average().orElse(500));
        postRepository.saveAll(postGenerator.generate(account, SEED_POSTS_PER_ACCOUNT, to, avgReach));
    }

    @Transactional(readOnly = true)
    public SocialAccount requireOwned(Long userId, Long accountId) {
        return accountRepository.findByIdAndUserId(accountId, userId)
                .orElseThrow(() -> ApiException.notFound("Account"));
    }

    private void applyHealth(SocialAccount account, PlatformAdapter.AccountHealth health) {
        account.setTokenValid(health.tokenValid());
        account.setStatus(health.status());
        account.setStatusMessage(health.message());
    }

    /** Platform filter parsing shared by stats and posts endpoints. */
    public static Platform parsePlatform(String raw) {
        if (raw == null || raw.isBlank() || "all".equalsIgnoreCase(raw)) {
            return null;
        }
        try {
            return Platform.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new ApiException(org.springframework.http.HttpStatus.BAD_REQUEST,
                    "Unknown platform: " + raw, "platform");
        }
    }

    public static Map<Platform, SocialAccount> byPlatform(List<SocialAccount> accounts) {
        return accounts.stream().collect(Collectors.toMap(SocialAccount::getPlatform, Function.identity(),
                (a, b) -> a));
    }
}
