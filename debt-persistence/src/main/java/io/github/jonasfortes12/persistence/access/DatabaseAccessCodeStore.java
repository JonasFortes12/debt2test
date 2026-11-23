package io.github.jonasfortes12.persistence.access;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;

/** JPA-backed {@link AccessCodeStore}. Never touches pipeline tables except through the run FK. */
@Component
public class DatabaseAccessCodeStore implements AccessCodeStore {

    private final ParticipantRepository participants;
    private final AccessCodeRepository codes;
    private final EntityManager entityManager;

    DatabaseAccessCodeStore(ParticipantRepository participants, AccessCodeRepository codes,
            EntityManager entityManager) {
        this.participants = participants;
        this.codes = codes;
        this.entityManager = entityManager;
    }

    @Override
    @Transactional
    public Participant upsertParticipant(String email, String name, Instant now) {
        UUID id = participants.upsert(UUID.randomUUID(), name, email, now);
        ParticipantEntity participant = entityManager.find(ParticipantEntity.class, id);
        // The native upsert bypassed the persistence context: reload the row's current values.
        entityManager.refresh(participant, LockModeType.PESSIMISTIC_WRITE);
        return participant.toRecord();
    }

    @Override
    @Transactional
    public void markMailed(UUID participantId, Instant now) {
        participants.markMailed(participantId, now);
    }

    @Override
    @Transactional(readOnly = true)
    public long countUsedCodes(UUID participantId) {
        return codes.countUsed(participantId);
    }

    @Override
    @Transactional
    public AccessCode issueCode(UUID participantId, NewAccessCode code, Instant now) {
        codes.revokeUnused(participantId, now);
        return codes.saveAndFlush(new AccessCodeEntity(UUID.randomUUID(), participantId, code)).toRecord();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AccessCode> findByHash(String codeHash) {
        return codes.findByCodeHash(codeHash).map(AccessCodeEntity::toRecord);
    }

    @Override
    @Transactional
    public boolean markUsed(String codeHash, Instant now, Instant resultsAccessUntil, int defaultRunQuota) {
        return codes.markUsed(codeHash, now, resultsAccessUntil, defaultRunQuota) == 1;
    }

    @Override
    @Transactional
    public void releaseUse(String codeHash) {
        codes.releaseUse(codeHash);
    }

    @Override
    @Transactional
    public void attachRun(String codeHash, UUID executionId) {
        if (codes.attachRun(codeHash, executionId) != 1) {
            throw new IllegalStateException("no used access code to attach the run to");
        }
    }
}
