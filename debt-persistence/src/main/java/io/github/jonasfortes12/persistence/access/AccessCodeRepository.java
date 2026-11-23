package io.github.jonasfortes12.persistence.access;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Every state change is a single conditional UPDATE, so the database, not a read-then-write in
 * Java, decides races such as two requests spending the same code.
 */
interface AccessCodeRepository extends JpaRepository<AccessCodeEntity, UUID> {

    Optional<AccessCodeEntity> findByCodeHash(String codeHash);

    @Query("select count(code) from AccessCodeEntity code "
            + "where code.participantId = :participantId and code.usedAt is not null")
    long countUsed(@Param("participantId") UUID participantId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update AccessCodeEntity code set code.revokedAt = :now "
            + "where code.participantId = :participantId and code.usedAt is null and code.revokedAt is null")
    int revokeUnused(@Param("participantId") UUID participantId, @Param("now") Instant now);

    /**
     * Spends a usable code only while its participant is still under quota (the operator override,
     * else {@code defaultRunQuota}): the quota is enforced when a run starts, not only when a code
     * is issued.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(nativeQuery = true, value = "UPDATE access_code code "
            + "SET used_at = :now, results_access_until = :resultsAccessUntil "
            + "WHERE code.code_hash = :codeHash AND code.used_at IS NULL AND code.revoked_at IS NULL "
            + "AND code.expires_at > :now "
            + "AND (SELECT count(*) FROM access_code spent "
            + "     WHERE spent.participant_id = code.participant_id AND spent.used_at IS NOT NULL) "
            + "  < (SELECT coalesce(owner.run_quota_override, :defaultRunQuota) FROM participant owner "
            + "     WHERE owner.id = code.participant_id)")
    int markUsed(@Param("codeHash") String codeHash, @Param("now") Instant now,
            @Param("resultsAccessUntil") Instant resultsAccessUntil, @Param("defaultRunQuota") int defaultRunQuota);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update AccessCodeEntity code set code.usedAt = null, code.resultsAccessUntil = null "
            + "where code.codeHash = :codeHash and code.executionId is null")
    int releaseUse(@Param("codeHash") String codeHash);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update AccessCodeEntity code set code.executionId = :executionId "
            + "where code.codeHash = :codeHash and code.usedAt is not null")
    int attachRun(@Param("codeHash") String codeHash, @Param("executionId") UUID executionId);
}
