package io.github.jonasfortes12.persistence.access;

import java.time.Instant;
import java.util.UUID;

/**
 * A stored access code, identified by the SHA-256 hash of the code (the plaintext is never stored).
 * {@code revokedAt}, {@code usedAt}, {@code executionId} and {@code resultsAccessUntil} are null
 * until the code is replaced, used, or linked to its run.
 */
public record AccessCode(
        UUID id,
        UUID participantId,
        String codeHash,
        String termsVersion,
        Instant consentedAt,
        String consentIp,
        Instant issuedAt,
        Instant expiresAt,
        Instant revokedAt,
        Instant usedAt,
        UUID executionId,
        Instant resultsAccessUntil) {
}
