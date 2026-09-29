package com.socialdashboard.service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public final class DateRanges {

    public record Range(String key, String label, int days) {
    }

    public static final Range DAY = new Range("1d", "Last 24 hours", 1);
    public static final Range WEEK = new Range("7d", "Last 7 days", 7);
    public static final Range MONTH = new Range("30d", "Last 30 days", 30);
    public static final Range QUARTER = new Range("90d", "Last 90 days", 90);
    public static final Range YEAR = new Range("365d", "Last 12 months", 365);
    public static final Range ALL = new Range("all", "All time", 730);

    private DateRanges() {
    }

    public static final int MAX_DAYS = 730;

    public static Range resolve(String key) {
        if (key == null || key.isBlank()) {
            return MONTH;
        }
        return switch (key.toLowerCase()) {
            case "1d", "day", "daily" -> DAY;
            case "7d", "week", "weekly" -> WEEK;
            case "30d", "month", "monthly" -> MONTH;
            case "90d", "quarter", "quarterly" -> QUARTER;
            case "365d", "year", "yearly" -> YEAR;
            case "all" -> ALL;
            default -> fromNumericKey(key);
        };
    }

    /**
     * Accepts any "Nd" / "N" form, e.g. "45d" or "120", so the client is not
     * limited to the named presets. Out-of-range values are clamped rather than
     * rejected, because a chart with fewer points is better than a 400 error.
     */
    private static Range fromNumericKey(String key) {
        String digits = key.toLowerCase().replaceAll("[^0-9]", "");
        if (digits.isEmpty()) {
            return MONTH;
        }
        try {
            int days = Math.min(Integer.parseInt(digits), MAX_DAYS);
            if (days <= 0) {
                return MONTH;
            }
            return new Range(days + "d", "Last " + days + " days", days);
        } catch (NumberFormatException ex) {
            return MONTH;
        }
    }

    /** Inclusive window ending today. */
    public static Window window(String key) {
        Range range = resolve(key);
        LocalDate to = LocalDate.now();
        LocalDate from = to.minusDays(range.days() - 1L);
        return new Window(range, from, to);
    }

    /** The equally sized window immediately before the given one, for delta maths. */
    public static Window previous(Window current) {
        long days = ChronoUnit.DAYS.between(current.from(), current.to()) + 1;
        return new Window(current.range(), current.from().minusDays(days), current.from().minusDays(1));
    }

    public record Window(Range range, LocalDate from, LocalDate to) {
        public long days() {
            return ChronoUnit.DAYS.between(from, to) + 1;
        }
    }
}
