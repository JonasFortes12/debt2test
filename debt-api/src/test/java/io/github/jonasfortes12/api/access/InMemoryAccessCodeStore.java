package io.github.jonasfortes12.api.access;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import io.github.jonasfortes12.persistence.access.AccessCode;
import io.github.jonasfortes12.persistence.access.AccessCodeStore;
import io.github.jonasfortes12.persistence.access.NewAccessCode;
import io.github.jonasfortes12.persistence.access.Participant;

/** Mirrors DatabaseAccessCodeStore's semantics in memory; every method is synchronized like a row lock. */
final class InMemoryAccessCodeStore implements AccessCodeStore {

    private final Map<String, Participant> participantsByEmail = new LinkedHashMap<>();
    private final Map<String, AccessCode> codesByHash = new LinkedHashMap<>();

    private boolean failNextAttach;

    synchronized void setQuotaOverride(String email, int quota) {
        Participant p = participantsByEmail.get(email);
        participantsByEmail.put(email, new Participant(p.id(), p.name(), p.email(), quota, p.createdAt(),
                p.lastMailedAt()));
    }

    synchronized void failNextAttach() {
        failNextAttach = true;
    }

    synchronized int participantCount() {
        return participantsByEmail.size();
    }

    synchronized boolean containsPlaintext(String code) {
        return codesByHash.containsKey(code);
    }

    @Override
    public synchronized Participant upsertParticipant(String email, String name, Instant now) {
        Participant existing = participantsByEmail.get(email);
        Participant participant = existing == null
                ? new Participant(UUID.randomUUID(), name, email, null, now, null)
                : new Participant(existing.id(), name, email, existing.runQuotaOverride(), existing.createdAt(),
                        existing.lastMailedAt());
        participantsByEmail.put(email, participant);
        return participant;
    }

    @Override
    public synchronized void markMailed(UUID participantId, Instant now) {
        participantsByEmail.replaceAll((email, p) -> p.id().equals(participantId)
                ? new Participant(p.id(), p.name(), p.email(), p.runQuotaOverride(), p.createdAt(), now)
                : p);
    }

    @Override
    public synchronized long countUsedCodes(UUID participantId) {
        return codesByHash.values().stream()
                .filter(c -> c.participantId().equals(participantId) && c.usedAt() != null).count();
    }

    @Override
    public synchronized AccessCode issueCode(UUID participantId, NewAccessCode code, Instant now) {
        codesByHash.replaceAll((hash, c) -> c.participantId().equals(participantId) && c.usedAt() == null
                && c.revokedAt() == null ? with(c, now, c.usedAt(), c.executionId(), c.resultsAccessUntil()) : c);
        AccessCode stored = new AccessCode(UUID.randomUUID(), participantId, code.codeHash(), code.termsVersion(),
                code.consentedAt(), code.consentIp(), code.issuedAt(), code.expiresAt(), null, null, null, null);
        codesByHash.put(code.codeHash(), stored);
        return stored;
    }

    @Override
    public synchronized Optional<AccessCode> findByHash(String codeHash) {
        return Optional.ofNullable(codesByHash.get(codeHash));
    }

    @Override
    public synchronized boolean markUsed(String codeHash, Instant now, Instant resultsAccessUntil,
            int defaultRunQuota) {
        AccessCode c = codesByHash.get(codeHash);
        if (c == null || c.usedAt() != null || c.revokedAt() != null || !c.expiresAt().isAfter(now)) {
            return false;
        }
        Participant owner = participantsByEmail.values().stream()
                .filter(p -> p.id().equals(c.participantId())).findFirst().orElseThrow();
        int quota = owner.runQuotaOverride() != null ? owner.runQuotaOverride() : defaultRunQuota;
        if (countUsedCodes(owner.id()) >= quota) {
            return false;
        }
        codesByHash.put(codeHash, with(c, null, now, null, resultsAccessUntil));
        return true;
    }

    @Override
    public synchronized void releaseUse(String codeHash) {
        AccessCode c = codesByHash.get(codeHash);
        if (c != null && c.executionId() == null) {
            codesByHash.put(codeHash, with(c, c.revokedAt(), null, null, null));
        }
    }

    @Override
    public synchronized void attachRun(String codeHash, UUID executionId) {
        AccessCode c = codesByHash.get(codeHash);
        if (failNextAttach) {
            failNextAttach = false;
            throw new IllegalStateException("database unavailable");
        }
        if (c == null || c.usedAt() == null) {
            throw new IllegalStateException("no used access code to attach the run to");
        }
        codesByHash.put(codeHash, with(c, c.revokedAt(), c.usedAt(), executionId, c.resultsAccessUntil()));
    }

    private static AccessCode with(AccessCode c, Instant revokedAt, Instant usedAt, UUID executionId,
            Instant resultsAccessUntil) {
        return new AccessCode(c.id(), c.participantId(), c.codeHash(), c.termsVersion(), c.consentedAt(),
                c.consentIp(), c.issuedAt(), c.expiresAt(), revokedAt, usedAt, executionId, resultsAccessUntil);
    }
}
