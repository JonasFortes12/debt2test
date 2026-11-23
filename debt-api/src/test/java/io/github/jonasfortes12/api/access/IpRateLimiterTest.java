package io.github.jonasfortes12.api.access;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.Test;

class IpRateLimiterTest {

    private final MutableClock clock = new MutableClock(Instant.parse("2026-11-06T12:00:00Z"));

    @Test
    void allowsLimitThenBlocks() {
        IpRateLimiter limiter = new IpRateLimiter(clock, 2);
        assertTrue(limiter.tryAcquire("203.0.113.7"));
        assertTrue(limiter.tryAcquire("203.0.113.7"));
        assertFalse(limiter.tryAcquire("203.0.113.7"));
    }

    @Test
    void resetsAfterOneHour() {
        IpRateLimiter limiter = new IpRateLimiter(clock, 1);
        assertTrue(limiter.tryAcquire("203.0.113.7"));
        assertFalse(limiter.tryAcquire("203.0.113.7"));
        clock.advance(Duration.ofHours(1));
        assertTrue(limiter.tryAcquire("203.0.113.7"));
    }

    @Test
    void limitsAreIndependentPerIp() {
        IpRateLimiter limiter = new IpRateLimiter(clock, 1);
        assertTrue(limiter.tryAcquire("203.0.113.7"));
        assertTrue(limiter.tryAcquire("198.51.100.1"));
    }
}
