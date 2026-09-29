package com.socialdashboard.adapter.mock;

import com.socialdashboard.adapter.PlatformAdapter;
import com.socialdashboard.entity.MetricSnapshot;
import com.socialdashboard.entity.SocialAccount;
import com.socialdashboard.generator.MetricGenerator;
import com.socialdashboard.generator.SeededRandom;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Stand-in for a real platform client. Everything is synthesised locally so the
 * dashboard is fully functional without third-party credentials. To go live,
 * create a sibling component that calls the platform's REST API and return its
 * response from {@link #fetchHistory}.
 */
public abstract class MockPlatformAdapter implements PlatformAdapter {

    protected final MetricGenerator metricGenerator = new MetricGenerator();

    @Override
    public AccountHealth checkHealth(SocialAccount account) {
        if (account.getStatus() == SocialAccount.AccountStatus.DISCONNECTED) {
            return new AccountHealth(false, SocialAccount.AccountStatus.DISCONNECTED,
                    "Account was disconnected. Reconnect to resume syncing.");
        }
        if (account.getStatus() == SocialAccount.AccountStatus.ERROR) {
            return new AccountHealth(false, SocialAccount.AccountStatus.ERROR,
                    account.getStatusMessage() != null ? account.getStatusMessage() : "API returned an error");
        }
        if (!account.isTokenValid()) {
            return new AccountHealth(false, SocialAccount.AccountStatus.WARNING,
                    "Access token expired. Re-authentication required.");
        }
        if (account.getStatus() == SocialAccount.AccountStatus.WARNING) {
            return new AccountHealth(true, SocialAccount.AccountStatus.WARNING,
                    account.getStatusMessage() != null ? account.getStatusMessage() : "API rate limit nearing threshold");
        }
        return new AccountHealth(true, SocialAccount.AccountStatus.ACTIVE, "Connected and syncing");
    }

    @Override
    public List<MetricSnapshot> fetchHistory(SocialAccount account, LocalDate from, LocalDate to) {
        return metricGenerator.generate(account, from, to);
    }

    protected void touch(SocialAccount account) {
        account.setLastSyncedAt(Instant.now());
    }

    protected boolean isStale(SocialAccount account) {
        return account.getLastSyncedAt() == null
                || account.getLastSyncedAt().isBefore(Instant.now().minusSeconds(3600));
    }

    protected SeededRandom streamFor(SocialAccount account, String label) {
        return new SeededRandom(account.getUsername().hashCode() * 31L + label.hashCode(), LocalDate.now());
    }
}
