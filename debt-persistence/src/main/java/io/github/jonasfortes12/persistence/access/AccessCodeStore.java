package io.github.jonasfortes12.persistence.access;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Storage for participants and their access codes. Storage only: quota, expiry windows and
 * cooldowns are decided by the caller (debt-api), which passes the resulting instants in.
 */
public interface AccessCodeStore {

    /**
     * Inserts the participant, or updates its name when the email is already known. Inside a
     * caller's transaction the participant row stays locked until it ends, which serializes
     * concurrent requests for the same email.
     */
    Participant upsertParticipant(String email, String name, Instant now);

    /** Records that an email (code or quota notice) was sent; the cooldown is measured from it. */
    void markMailed(UUID participantId, Instant now);

    long countUsedCodes(UUID participantId);

    /**
     * In one transaction: revokes every unused, unrevoked code of the participant, then stores the
     * new one.
     */
    AccessCode issueCode(UUID participantId, NewAccessCode code, Instant now);

    Optional<AccessCode> findByHash(String codeHash);

    /**
     * Atomically marks an unused, unrevoked, unexpired code as used, provided its participant has
     * started fewer runs than their quota (their override, else {@code defaultRunQuota}).
     *
     * @return true for exactly one caller per code; false when the code is unknown, already used,
     *         revoked, expired at {@code now}, or its participant is at quota
     */
    boolean markUsed(String codeHash, Instant now, Instant resultsAccessUntil, int defaultRunQuota);

    /** Undoes {@link #markUsed} when the run could not be created; a code linked to a run stays used. */
    void releaseUse(String codeHash);

    /** Links a used code to the run it created; the run must exist. */
    void attachRun(String codeHash, UUID executionId);
}
