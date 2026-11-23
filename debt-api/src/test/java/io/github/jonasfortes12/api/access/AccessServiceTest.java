package io.github.jonasfortes12.api.access;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionOperations;

class AccessServiceTest {

    private static final Instant START = Instant.parse("2026-11-06T12:00:00Z");
    private static final String IP = "203.0.113.7";

    private final MutableClock clock = new MutableClock(START);
    private final InMemoryAccessCodeStore store = new InMemoryAccessCodeStore();
    private final RecordingAccessCodeSender sender = new RecordingAccessCodeSender();
    private final AccessProperties properties = new AccessProperties(
            3, Duration.ofHours(72), Duration.ofDays(30), Duration.ofMinutes(5), 10, "v1", "");
    private final AccessService service = new AccessService(store, sender, properties, clock,
            TransactionOperations.withoutTransaction());
    private final List<String> abandonedRuns = new ArrayList<>();

    private String requestCode(String email) {
        service.requestCode("Ana", email, "v1", IP);
        return sender.lastCode();
    }

    private String startRun(String code) {
        String executionId = UUID.randomUUID().toString();
        return service.startRun(code, () -> executionId, abandonedRuns::add);
    }

    @Test
    void requestIssuesCodeAndEmailsIt() {
        service.requestCode("Ana", "ana@example.com", "v1", IP);

        assertEquals(1, sender.codes.size());
        RecordingAccessCodeSender.SentCode sent = sender.codes.getFirst();
        assertTrue(sent.code().matches("^D2T(-[0-9A-HJKMNP-TV-Z]{4}){7}$"));
        assertEquals("ana@example.com", sent.email());
        assertEquals(START.plus(Duration.ofHours(72)), sent.expiresAt());
        assertEquals(3, sent.runsRemaining());
        assertEquals("v1", sent.termsVersion());
        assertFalse(store.containsPlaintext(sent.code()), "only the hash is stored");
        assertTrue(store.findByHash(AccessCodes.hash(sent.code())).isPresent());
    }

    @Test
    void requestRecordsConsent() {
        String code = requestCode("ana@example.com");

        var stored = store.findByHash(AccessCodes.hash(code)).orElseThrow();
        assertEquals("v1", stored.termsVersion());
        assertEquals(START, stored.consentedAt());
        assertEquals(IP, stored.consentIp());
    }

    @Test
    void requestWithNonCurrentTermsThrows() {
        TermsNotCurrentException failure = assertThrows(TermsNotCurrentException.class,
                () -> service.requestCode("Ana", "ana@example.com", "v0", IP));
        assertEquals("terms version 'v0' is not current; current is 'v1'", failure.getMessage());
        assertTrue(sender.codes.isEmpty());
    }

    @Test
    void requestOverIpLimitThrows() {
        for (int i = 0; i < 10; i++) {
            service.requestCode("Ana", "user" + i + "@example.com", "v1", IP);
        }
        assertThrows(TooManyRequestsException.class,
                () -> service.requestCode("Ana", "other@example.com", "v1", IP));
    }

    @Test
    void requestWithinCooldownSendsNothing() {
        requestCode("ana@example.com");
        clock.advance(Duration.ofMinutes(4));
        service.requestCode("Ana", "ana@example.com", "v1", IP);

        assertEquals(1, sender.codes.size());
        assertTrue(sender.quotas.isEmpty());
    }

    @Test
    void reRequestAfterCooldownRevokesPreviousCode() {
        String first = requestCode("ana@example.com");
        clock.advance(Duration.ofMinutes(5));
        String second = requestCode("ana@example.com");

        assertEquals(AccessCodeState.EXPIRED, service.authenticate(first).orElseThrow().state());
        assertEquals(AccessCodeState.UNUSED, service.authenticate(second).orElseThrow().state());
        assertEquals(3, sender.codes.getLast().runsRemaining(), "replacing a code does not count toward the quota");
    }

    @Test
    void emailCasingMapsToOneParticipant() {
        requestCode("Jonas@Gmail.com");
        clock.advance(Duration.ofMinutes(5));
        requestCode("  jonas@gmail.com ");

        assertEquals(1, store.participantCount());
        assertEquals("jonas@gmail.com", sender.codes.getLast().email());
    }

    @Test
    void quotaReachedSendsExplanationInsteadOfCode() {
        for (int i = 0; i < 3; i++) {
            startRun(requestCode("ana@example.com"));
            clock.advance(Duration.ofMinutes(5));
        }
        service.requestCode("Ana", "ana@example.com", "v1", IP);

        assertEquals(3, sender.codes.size());
        assertEquals(1, sender.quotas.size());
        assertEquals(3, sender.quotas.getFirst().runQuota());
        assertEquals(1, sender.codes.getLast().runsRemaining());
    }

    @Test
    void quotaOverrideRaisesLimit() {
        for (int i = 0; i < 3; i++) {
            startRun(requestCode("ana@example.com"));
            clock.advance(Duration.ofMinutes(5));
        }
        store.setQuotaOverride("ana@example.com", 4);
        service.requestCode("Ana", "ana@example.com", "v1", IP);

        assertEquals(4, sender.codes.size());
        assertEquals(1, sender.codes.getLast().runsRemaining());
        assertTrue(sender.quotas.isEmpty());
    }

    @Test
    void authenticateAcceptsPastedCodeWithWhitespaceAndLowercase() {
        String code = requestCode("ana@example.com");

        AuthenticatedCode authenticated = service.authenticate("  " + code.toLowerCase() + "\n").orElseThrow();
        assertEquals(AccessCodeState.UNUSED, authenticated.state());
    }

    @Test
    void authenticateUnknownCodeIsEmpty() {
        assertTrue(service.authenticate("D2T-0000-0000-0000-0000-0000-0000-0000").isEmpty());
        assertTrue(service.authenticate("").isEmpty());
        assertTrue(service.authenticate(null).isEmpty());
    }

    @Test
    void authenticateReportsStates() {
        String code = requestCode("ana@example.com");
        AuthenticatedCode unused = service.authenticate(code).orElseThrow();
        assertEquals(AccessCodeState.UNUSED, unused.state());
        assertEquals(START.plus(Duration.ofHours(72)), unused.expiresAt());
        assertNull(unused.executionId());

        clock.advance(Duration.ofHours(1));
        String executionId = startRun(code);
        AuthenticatedCode used = service.authenticate(code).orElseThrow();
        assertEquals(AccessCodeState.USED, used.state());
        assertEquals(UUID.fromString(executionId), used.executionId());
        assertEquals(START.plus(Duration.ofHours(1)).plus(Duration.ofDays(30)), used.expiresAt());

        clock.advance(Duration.ofDays(30));
        assertEquals(AccessCodeState.EXPIRED, service.authenticate(code).orElseThrow().state());
    }

    @Test
    void unusedCodeExpiresAfterTtl() {
        String code = requestCode("ana@example.com");
        clock.advance(Duration.ofHours(72));

        assertEquals(AccessCodeState.EXPIRED, service.authenticate(code).orElseThrow().state());
        assertThrows(AccessCodeUsedException.class, () -> startRun(code));
    }

    @Test
    void startRunTwiceThrowsAccessCodeUsed() {
        String code = requestCode("ana@example.com");
        startRun(code);

        AccessCodeUsedException failure = assertThrows(AccessCodeUsedException.class, () -> startRun(code));
        assertEquals("access code has already been used for a run", failure.getMessage());
    }

    @Test
    void failedSubmitReleasesCode() {
        String code = requestCode("ana@example.com");

        assertThrows(IllegalStateException.class, () -> service.startRun(code, () -> {
            throw new IllegalStateException("persistence is unavailable");
        }, abandonedRuns::add));
        assertEquals(AccessCodeState.UNUSED, service.authenticate(code).orElseThrow().state());
    }

    @Test
    void quotaMailRespectsCooldown() {
        for (int i = 0; i < 3; i++) {
            startRun(requestCode("ana@example.com"));
            clock.advance(Duration.ofMinutes(5));
        }
        service.requestCode("Ana", "ana@example.com", "v1", IP);
        service.requestCode("Ana", "ana@example.com", "v1", "198.51.100.1");
        assertEquals(1, sender.quotas.size(), "a second quota mail inside the cooldown would let anyone flood the inbox");

        clock.advance(Duration.ofMinutes(5));
        service.requestCode("Ana", "ana@example.com", "v1", IP);
        assertEquals(2, sender.quotas.size());
    }

    @Test
    void failedAttachAbandonsRunAndReleasesCode() {
        String code = requestCode("ana@example.com");
        store.failNextAttach();

        assertThrows(IllegalStateException.class, () -> startRun(code));

        assertEquals(1, abandonedRuns.size(), "the orphaned run is cancelled");
        assertEquals(AccessCodeState.UNUSED, service.authenticate(code).orElseThrow().state());
    }

    @Test
    void spendingRechecksTheQuota() {
        String code = requestCode("ana@example.com");
        store.setQuotaOverride("ana@example.com", 0);

        assertThrows(AccessCodeUsedException.class, () -> startRun(code));
        assertEquals(AccessCodeState.UNUSED, service.authenticate(code).orElseThrow().state());
    }

    @Test
    void parallelStartRunHasOneWinner() throws Exception {
        String code = requestCode("ana@example.com");
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            List<Future<String>> attempts = new ArrayList<>();
            for (int i = 0; i < 2; i++) {
                attempts.add(pool.submit(() -> {
                    start.await();
                    try {
                        return startRun(code);
                    } catch (AccessCodeUsedException rejected) {
                        return null;
                    }
                }));
            }
            start.countDown();
            long winners = 0;
            for (Future<String> attempt : attempts) {
                if (attempt.get() != null) {
                    winners++;
                }
            }
            assertEquals(1, winners);
        } finally {
            pool.shutdownNow();
        }
    }
}
