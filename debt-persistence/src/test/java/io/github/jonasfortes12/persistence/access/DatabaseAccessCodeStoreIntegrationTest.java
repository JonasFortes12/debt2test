package io.github.jonasfortes12.persistence.access;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Tag("integration")
@Testcontainers
@DataJpaTest
@ImportAutoConfiguration(FlywayAutoConfiguration.class)
@Import(DatabaseAccessCodeStore.class)
@ActiveProfiles("test")
class DatabaseAccessCodeStoreIntegrationTest {

    private static final Instant NOW = Instant.parse("2026-11-06T12:00:00Z").truncatedTo(ChronoUnit.MICROS);

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    private AccessCodeStore store;

    @Autowired
    private JdbcTemplate jdbc;

    private static String hash(char fill) {
        return String.valueOf(fill).repeat(64);
    }

    private static NewAccessCode newCode(String codeHash) {
        return newCode(codeHash, NOW);
    }

    private static NewAccessCode newCode(String codeHash, Instant issuedAt) {
        return new NewAccessCode(codeHash, "v1", issuedAt, "203.0.113.7", issuedAt, issuedAt.plus(Duration.ofHours(72)));
    }

    private UUID insertPipelineRun() {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO pipeline_run (id, repository_url, status, started_at, context_enabled, "
                + "heuristic_fallback_allowed) VALUES (?, 'https://example.com/r.git', 'QUEUED', ?, false, false)",
                id, Timestamp.from(NOW));
        return id;
    }

    @Test
    void upsertKeepsOneParticipantPerEmailAndUpdatesName() {
        Participant first = store.upsertParticipant("ana@example.com", "Ana", NOW);
        Participant second = store.upsertParticipant("ana@example.com", "Ana Maria", NOW.plusSeconds(60));

        assertEquals(first.id(), second.id());
        assertEquals("Ana Maria", second.name());
        assertEquals(NOW, second.createdAt());
    }

    @Test
    void issueCodeRevokesPreviousUnusedCode() {
        Participant participant = store.upsertParticipant("ana@example.com", "Ana", NOW);
        store.issueCode(participant.id(), newCode(hash('a')), NOW);
        store.issueCode(participant.id(), newCode(hash('b'), NOW.plusSeconds(600)), NOW.plusSeconds(600));

        assertNotNull(store.findByHash(hash('a')).orElseThrow().revokedAt());
        assertNull(store.findByHash(hash('b')).orElseThrow().revokedAt());

    }

    @Test
    void markUsedSucceedsOnceThenFails() {
        Participant participant = store.upsertParticipant("ana@example.com", "Ana", NOW);
        store.issueCode(participant.id(), newCode(hash('a')), NOW);

        assertTrue(store.markUsed(hash('a'), NOW.plusSeconds(1), NOW.plus(Duration.ofDays(30)), 3));
        assertFalse(store.markUsed(hash('a'), NOW.plusSeconds(2), NOW.plus(Duration.ofDays(30)), 3));

        AccessCode used = store.findByHash(hash('a')).orElseThrow();
        assertEquals(NOW.plusSeconds(1), used.usedAt());
        assertEquals(NOW.plus(Duration.ofDays(30)), used.resultsAccessUntil());
    }

    @Test
    void markUsedFailsForExpiredOrRevokedCode() {
        Participant participant = store.upsertParticipant("ana@example.com", "Ana", NOW);
        store.issueCode(participant.id(), newCode(hash('a')), NOW);
        store.issueCode(participant.id(), newCode(hash('b')), NOW);

        assertFalse(store.markUsed(hash('a'), NOW.plusSeconds(1), NOW.plus(Duration.ofDays(30)), 3), "revoked");
        assertFalse(store.markUsed(hash('b'), NOW.plus(Duration.ofHours(73)), NOW.plus(Duration.ofDays(30)), 3),
                "expired");
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void concurrentMarkUsedHasExactlyOneWinner() throws Exception {
        Participant participant = store.upsertParticipant("race@example.com", "Race", NOW);
        store.issueCode(participant.id(), newCode(hash('c')), NOW);
        int threads = 8;
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        try {
            List<Future<Boolean>> results = new ArrayList<>();
            for (int i = 0; i < threads; i++) {
                results.add(pool.submit(() -> {
                    start.await();
                    return store.markUsed(hash('c'), NOW.plusSeconds(1), NOW.plus(Duration.ofDays(30)), 3);
                }));
            }
            start.countDown();
            int winners = 0;
            for (Future<Boolean> result : results) {
                if (result.get()) {
                    winners++;
                }
            }
            assertEquals(1, winners);
        } finally {
            pool.shutdownNow();
            jdbc.update("DELETE FROM access_code");
            jdbc.update("DELETE FROM participant");
        }
    }

    @Test
    void releaseUseRestoresUnusedOnlyWhenNoRunAttached() {
        Participant participant = store.upsertParticipant("ana@example.com", "Ana", NOW);
        store.issueCode(participant.id(), newCode(hash('a')), NOW);
        store.markUsed(hash('a'), NOW, NOW.plus(Duration.ofDays(30)), 3);
        store.releaseUse(hash('a'));
        AccessCode released = store.findByHash(hash('a')).orElseThrow();
        assertNull(released.usedAt());
        assertNull(released.resultsAccessUntil());

        store.markUsed(hash('a'), NOW, NOW.plus(Duration.ofDays(30)), 3);
        UUID runId = insertPipelineRun();
        store.attachRun(hash('a'), runId);
        store.releaseUse(hash('a'));
        AccessCode attached = store.findByHash(hash('a')).orElseThrow();
        assertNotNull(attached.usedAt());
        assertEquals(runId, attached.executionId());
    }

    @Test
    void attachRunRequiresExistingPipelineRun() {
        Participant participant = store.upsertParticipant("ana@example.com", "Ana", NOW);
        store.issueCode(participant.id(), newCode(hash('a')), NOW);
        store.markUsed(hash('a'), NOW, NOW.plus(Duration.ofDays(30)), 3);

        assertThrows(DataIntegrityViolationException.class,
                () -> store.attachRun(hash('a'), UUID.randomUUID()));
    }

    @Test
    void markUsedRefusesWhenParticipantIsAtQuota() {
        Participant participant = store.upsertParticipant("ana@example.com", "Ana", NOW);
        store.issueCode(participant.id(), newCode(hash('a')), NOW);
        assertTrue(store.markUsed(hash('a'), NOW, NOW.plus(Duration.ofDays(30)), 1));
        store.issueCode(participant.id(), newCode(hash('b')), NOW);

        assertFalse(store.markUsed(hash('b'), NOW, NOW.plus(Duration.ofDays(30)), 1), "default quota 1 reached");
        jdbc.update("UPDATE participant SET run_quota_override = 2 WHERE id = ?", participant.id());
        assertTrue(store.markUsed(hash('b'), NOW, NOW.plus(Duration.ofDays(30)), 1), "override raises it");
    }

    @Test
    void markMailedIsReturnedByTheNextUpsert() {
        Participant participant = store.upsertParticipant("ana@example.com", "Ana", NOW);
        assertNull(participant.lastMailedAt());

        store.markMailed(participant.id(), NOW.plusSeconds(5));

        assertEquals(NOW.plusSeconds(5), store.upsertParticipant("ana@example.com", "Ana", NOW).lastMailedAt());
    }

    @Test
    void atMostOneUsableCodePerParticipant() {
        Participant participant = store.upsertParticipant("ana@example.com", "Ana", NOW);
        store.issueCode(participant.id(), newCode(hash('a')), NOW);

        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update(
                "INSERT INTO access_code (id, participant_id, code_hash, terms_version, consented_at, consent_ip, "
                        + "issued_at, expires_at) VALUES (?, ?, ?, 'v1', ?, '203.0.113.7', ?, ?)",
                UUID.randomUUID(), participant.id(), hash('z'), Timestamp.from(NOW), Timestamp.from(NOW),
                Timestamp.from(NOW.plus(Duration.ofHours(72)))));
    }

    @Test
    void countUsedCodesCountsOnlyUsed() {
        Participant participant = store.upsertParticipant("ana@example.com", "Ana", NOW);
        store.issueCode(participant.id(), newCode(hash('a')), NOW);
        store.markUsed(hash('a'), NOW, NOW.plus(Duration.ofDays(30)), 3);
        store.issueCode(participant.id(), newCode(hash('b')), NOW);

        assertEquals(1, store.countUsedCodes(participant.id()));
    }
}
