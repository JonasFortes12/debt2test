package io.github.jonasfortes12.persistence.access;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface ParticipantRepository extends JpaRepository<ParticipantEntity, UUID> {

    /**
     * Inserts the participant or renames the existing one, in one statement, so two first requests
     * for the same email cannot collide on the unique constraint. The row stays locked until the
     * surrounding transaction ends.
     */
    @Query(nativeQuery = true, value = "INSERT INTO participant (id, name, email, created_at) "
            + "VALUES (:id, :name, :email, :now) "
            + "ON CONFLICT (email) DO UPDATE SET name = EXCLUDED.name RETURNING id")
    UUID upsert(@Param("id") UUID id, @Param("name") String name, @Param("email") String email,
            @Param("now") Instant now);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update ParticipantEntity participant set participant.lastMailedAt = :now where participant.id = :id")
    int markMailed(@Param("id") UUID id, @Param("now") Instant now);
}
