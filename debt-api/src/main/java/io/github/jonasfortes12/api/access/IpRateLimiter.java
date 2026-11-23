package io.github.jonasfortes12.api.access;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Fixed one-hour window per client IP, held in memory: correct for the single API instance this
 * project runs, not shared across instances.
 */
public class IpRateLimiter {

    private static final Duration WINDOW = Duration.ofHours(1);

    private record Window(Instant start, int count) {
    }

    private final Clock clock;
    private final int limitPerHour;
    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    public IpRateLimiter(Clock clock, int limitPerHour) {
        this.clock = clock;
        this.limitPerHour = limitPerHour;
    }

    /** @return true when the request is within this IP's limit, counting it */
    public boolean tryAcquire(String ip) {
        Instant now = clock.instant();
        Window window = windows.compute(ip, (key, current) -> current == null || isOver(current, now)
                ? new Window(now, 1)
                : new Window(current.start(), current.count() + 1));
        dropFinishedWindows(now);
        return window.count() <= limitPerHour;
    }

    private static boolean isOver(Window window, Instant now) {
        return !now.isBefore(window.start().plus(WINDOW));
    }

    private void dropFinishedWindows(Instant now) {
        windows.values().removeIf(window -> isOver(window, now));
    }
}
