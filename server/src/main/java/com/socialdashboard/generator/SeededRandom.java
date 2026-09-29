package com.socialdashboard.generator;

import java.time.LocalDate;
import java.util.Random;

/**
 * Deterministic pseudo-random number generator. Seeding per account keeps the
 * generated history stable across restarts, so charts and reports do not
 * contradict each other between requests.
 */
public class SeededRandom {

    private final Random random;
    private final LocalDate epoch;

    public SeededRandom(long seed, LocalDate epoch) {
        this.random = new Random(seed);
        this.epoch = epoch;
    }

    public SeededRandom forStream(String stream) {
        return new SeededRandom(seedFor(stream), epoch);
    }

    private long seedFor(String stream) {
        return stream.hashCode() * 31L + epoch.toEpochDay();
    }

    /** Uniform double in [0,1). */
    public double nextDouble() {
        return random.nextDouble();
    }

    /** Uniform long in [min,max]. */
    public long between(long min, long max) {
        if (max <= min) {
            return min;
        }
        return min + (long) (random.nextDouble() * (max - min));
    }

    public int nextInt(int bound) {
        return random.nextInt(bound);
    }

    public boolean chance(double probability) {
        return random.nextDouble() < probability;
    }

    public <T> T pick(java.util.List<T> options) {
        return options.get(random.nextInt(options.size()));
    }

    /** Approximate normal via sum of uniforms, clamped to +/- 3 sigma. */
    public double gaussian(double mean, double stdDev) {
        double sum = 0;
        for (int i = 0; i < 6; i++) {
            sum += random.nextDouble();
        }
        double z = (sum - 3) / 0.7071;
        return mean + z * stdDev;
    }
}
