package io.github.jonasfortes12.persistence.access;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** State changes after insert go through the atomic updates in {@link AccessCodeRepository}. */
@Entity
@Table(name = "access_code")
class AccessCodeEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "participant_id", nullable = false, updatable = false)
    private UUID participantId;

    @Column(name = "code_hash", nullable = false, updatable = false, length = 64, columnDefinition = "bpchar(64)")
    private String codeHash;

    @Column(name = "terms_version", nullable = false, updatable = false, length = 32)
    private String termsVersion;

    @Column(name = "consented_at", nullable = false, updatable = false)
    private Instant consentedAt;

    @Column(name = "consent_ip", nullable = false, updatable = false, length = 45)
    private String consentIp;

    @Column(name = "issued_at", nullable = false, updatable = false)
    private Instant issuedAt;

    @Column(name = "expires_at", nullable = false, updatable = false)
    private Instant expiresAt;

    @Column(name = "revoked_at", insertable = false, updatable = false)
    private Instant revokedAt;

    @Column(name = "used_at", insertable = false, updatable = false)
    private Instant usedAt;

    @Column(name = "execution_id", insertable = false, updatable = false)
    private UUID executionId;

    @Column(name = "results_access_until", insertable = false, updatable = false)
    private Instant resultsAccessUntil;

    protected AccessCodeEntity() {
    }

    AccessCodeEntity(UUID id, UUID participantId, NewAccessCode code) {
        this.id = id;
        this.participantId = participantId;
        this.codeHash = code.codeHash();
        this.termsVersion = code.termsVersion();
        this.consentedAt = code.consentedAt();
        this.consentIp = code.consentIp();
        this.issuedAt = code.issuedAt();
        this.expiresAt = code.expiresAt();
    }

    AccessCode toRecord() {
        return new AccessCode(id, participantId, codeHash, termsVersion, consentedAt, consentIp, issuedAt,
                expiresAt, revokedAt, usedAt, executionId, resultsAccessUntil);
    }
}
