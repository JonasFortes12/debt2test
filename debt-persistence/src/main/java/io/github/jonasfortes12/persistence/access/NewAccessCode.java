package io.github.jonasfortes12.persistence.access;

import java.time.Instant;

/** The values of a code being issued, including the consent given when it was requested. */
public record NewAccessCode(
        String codeHash,
        String termsVersion,
        Instant consentedAt,
        String consentIp,
        Instant issuedAt,
        Instant expiresAt) {
}
