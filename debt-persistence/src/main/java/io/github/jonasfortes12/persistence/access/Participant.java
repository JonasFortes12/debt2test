package io.github.jonasfortes12.persistence.access;

import java.time.Instant;
import java.util.UUID;

/**
 * A person who asked for an access code. {@code runQuotaOverride} is null unless an operator set one;
 * {@code lastMailedAt} is null until the first email (code or quota notice) is sent.
 */
public record Participant(UUID id, String name, String email, Integer runQuotaOverride, Instant createdAt,
        Instant lastMailedAt) {
}
