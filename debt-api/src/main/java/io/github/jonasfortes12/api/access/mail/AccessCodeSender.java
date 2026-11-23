package io.github.jonasfortes12.api.access.mail;

import java.time.Instant;

/** Delivers access-code emails. Implementations must never log the code (except the dev log sender). */
public interface AccessCodeSender {

    void sendCode(String email, String code, Instant expiresAt, int runsRemaining, String termsVersion);

    void sendQuotaReached(String email, int runQuota);
}
