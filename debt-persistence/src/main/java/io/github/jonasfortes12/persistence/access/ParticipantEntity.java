package io.github.jonasfortes12.persistence.access;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "participant")
class ParticipantEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "email", nullable = false, length = 320, updatable = false)
    private String email;

    @Column(name = "run_quota_override")
    private Integer runQuotaOverride;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "last_mailed_at", insertable = false, updatable = false)
    private Instant lastMailedAt;

    /** Rows are written only by the native statements in {@link ParticipantRepository}. */
    protected ParticipantEntity() {
    }

    Participant toRecord() {
        return new Participant(id, name, email, runQuotaOverride, createdAt, lastMailedAt);
    }
}
