package com.socialdashboard.adapter;

import com.socialdashboard.entity.MetricSnapshot;
import com.socialdashboard.entity.SocialAccount;
import com.socialdashboard.entity.SocialAccount.Platform;

import java.time.LocalDate;
import java.util.List;

/**
 * One adapter per social network. The dashboard talks only to this interface,
 * so swapping the mock generator for real platform APIs is a matter of adding a
 * new implementation and selecting it in {@link PlatformAdapterRegistry}.
 */
public interface PlatformAdapter {

    Platform platform();

    /** Human readable name used in the UI, e.g. "Instagram". */
    String displayName();

    /** Check that the credentials for this account still work. */
    AccountHealth checkHealth(SocialAccount account);

    /** Pull the raw daily counters for the requested window. */
    List<MetricSnapshot> fetchHistory(SocialAccount account, LocalDate from, LocalDate to);

    record AccountHealth(boolean tokenValid, SocialAccount.AccountStatus status, String message) {
    }
}
