# Technical Debt Test Generation Report

Generated test cases to pay off self-admitted technical debt (SATD).

## LoginService.java -> authenticate()

- **Debt Type:** `DESIGN`
- **Line Number:** `9`
- **Status:** `GENERATED`
- **Comment:** `* TODO DEBT2TEST-1: refactor this; legacy MD5 hashing is an ugly workaround and      * password hashing should be redesigned around bcrypt (workFactor=12) or Argon2.`

```java
/**
 * TODO DEBT2TEST-1: refactor this; legacy MD5 hashing is an ugly workaround and
 * password hashing should be redesigned around bcrypt (workFactor=12) or Argon2.
 */
public boolean authenticate(String username, String password) {
    if (username == null || username.isEmpty()) {
        return false;
    }
    String passwordHash = legacyMd5Hash(password);
    return validateHash(username, passwordHash);
}
```

### Ticket Context

- **Status:** `MATCHED`
- **Provider:** `jira`
- **Key:** `DEBT2TEST-1`
- **URL:** `https://debt2test.atlassian.net/browse/DEBT2TEST-1`
- **Summary:** Migrate MD5 to bcrypt

**Description:**

```
Authentication currently hashes passwords with MD5, which is cryptographically broken and unsuitable for password storage.

*Debt context (SATD):* {{LoginService.authenticate()}} — comment flags this as legacy code from 2015 with ~15% performance overhead on login attempts.

*Acceptance Criteria:*

* Replace {{legacyMd5Hash}} with bcrypt (workFactor=12) or Argon2.
* Existing stored hashes are migrated or re-hashed on next successful login.
* Unit tests cover authentication with the new hashing scheme.
```

**Issue References:**

- `DEBT2TEST-1` (source: `comment`)

### Generated Test Case

```java
package com.example.auth;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Characterization and design tests for {@code LoginService.authenticate()}.
 *
 * These tests pin the current MD5-based behaviour (TODO DEBT2TEST-1) so that the
 * refactor to bcrypt (workFactor=12) or Argon2 can be verified against the same
 * contract. The bcrypt-specific tests are disabled until the migration lands.
 *
 * Assumption: {@code validateHash(String, String)} is overridable (protected or
 * package-private) and is the single point where the stored hash is compared.
 */
class LoginServiceAuthenticateTest {

    /** Test double that records validateHash calls and returns a configurable result. */
    static class RecordingLoginService extends LoginService {
        final Map<String, String> storedHashes = new ConcurrentHashMap<>();
        final List<String[]> validateCalls = new ArrayList<>();

        @Override
        protected boolean validateHash(String username, String passwordHash) {
            validateCalls.add(new String[] {username, passwordHash});
            return passwordHash != null && passwordHash.equals(storedHashes.get(username));
        }

        void store(String username, String passwordHash) {
            storedHashes.put(username, passwordHash);
        }
    }

    private RecordingLoginService service;

    @BeforeEach
    void setUp() {
        service = new RecordingLoginService();
    }

    private static String md5Hex(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("MD5 not available", e);
        }
    }

    @Nested
    @DisplayName("input validation guards")
    class InputValidation {

        @Test
        @DisplayName("null username is rejected without consulting the credential store")
        void nullUsernameReturnsFalse() {
            assertFalse(service.authenticate(null, "password"));
            assertTrue(service.validateCalls.isEmpty(),
                    "validateHash must not be called for a null username");
        }

        @Test
        @DisplayName("empty username is rejected without consulting the credential store")
        void emptyUsernameReturnsFalse() {
            assertFalse(service.authenticate("", "password"));
            assertTrue(service.validateCalls.isEmpty(),
                    "validateHash must not be called for an empty username");
        }
    }

    @Nested
    @DisplayName("legacy MD5 hashing behaviour")
    class LegacyMd5Behaviour {

        @Test
        @DisplayName("passes the MD5 hex digest of the password to validateHash")
        void delegatesMd5DigestToValidator() {
            service.authenticate("alice", "password");

            assertEquals(1, service.validateCalls.size());
            assertEquals("alice", service.validateCalls.get(0)[0]);
            assertEquals("5f4dcc3b5aa765d61d8327deb882cf99", service.validateCalls.get(0)[1],
                    "MD5(\"password\") known test vector");
        }

        @Test
        @DisplayName("digest matches an independently computed MD5 hex string")
        void digestMatchesIndependentComputation() {
            String password = "correct horse battery staple";
            service.authenticate("bob", password);

            assertEquals(md5Hex(password), service.validateCalls.get(0)[1]);
        }

        @Test
        @DisplayName("password hashing is case-sensitive")
        void passwordIsCaseSensitive() {
            service.authenticate("carol", "Password");
            service.authenticate("carol", "password");

            assertNotEquals(service.validateCalls.get(0)[1], service.validateCalls.get(1)[1]);
        }

        @Test
        @DisplayName("same password produces the same hash (deterministic, no salt)")
        void hashIsDeterministic() {
            service.authenticate("dave", "secret");
            service.authenticate("dave", "secret");

            assertEquals(service.validateCalls.get(0)[1], service.validateCalls.get(1)[1]);
        }
    }

    @Nested
    @DisplayName("authentication outcome")
    class Outcome {

        @Test
        @DisplayName("returns true when the stored hash matches the MD5 of the password")
        void correctPasswordAuthenticates() {
            service.store("erin", md5Hex("hunter2"));

            assertTrue(service.authenticate("erin", "hunter2"));
        }

        @Test
        @DisplayName("returns false when the password is wrong")
        void wrongPasswordFails() {
            service.store("frank", md5Hex("hunter2"));

            assertFalse(service.authenticate("frank", "hunter3"));
        }

        @Test
        @DisplayName("returns false for an unknown user")
        void unknownUserFails() {
            assertFalse(service.authenticate("ghost", "anything"));
        }
    }

    /**
     * Acceptance criteria for the migration (bcrypt workFactor=12 or Argon2) are
     * captured below. Enable once the legacyMd5Hash path is replaced.
     */
    @Nested
    @DisplayName("bcrypt migration (pending DEBT2TEST-1)")
    class BcryptMigration {

        @Test
        @Disabled("Pending DEBT2TEST-1: replace MD5 with bcrypt(workFactor=12) or Argon2")
        @DisplayName("stored bcrypt hash authenticates the correct password")
        void bcryptRoundTrip() {
            // Expected post-migration contract:
            // String stored = PasswordHasher.bcrypt("hunter2", 12);
            // service.store("gina", stored);
            // assertTrue(service.authenticate("gina", "hunter2"));
            // assertFalse(service.authenticate("gina", "wrong"));
            fail("Migration not yet implemented");
        }

        @Test
        @Disabled("Pending DEBT2TEST-1: legacy MD5 hashes must be re-hashed on successful login")
        @DisplayName("legacy MD5 hash is upgraded to bcrypt after successful login")
        void legacyHashIsRehashedOnLogin() {
            // Expected post-migration contract:
            // service.store("hank", md5Hex("hunter2"));
            // assertTrue(service.authenticate("hank", "hunter2"));
            // assertTrue(PasswordHasher.isBcrypt(service.storedHashes.get("hank")));
            fail("Migration not yet implemented");
        }
    }
}
```

---

## OrderRepository.java -> findOrdersForCustomers()

- **Debt Type:** `DESIGN`
- **Line Number:** `12`
- **Status:** `GENERATED`
- **Comment:** `TODO DEBT2TEST-7: refactor this; one query per customer is an ugly workaround (N+1) and should be redesigned as a single batched IN (...) query.`

```java
// TODO DEBT2TEST-7: refactor this; one query per customer is an ugly workaround (N+1)
// and should be redesigned as a single batched IN (...) query.
public List<String> findOrdersForCustomers(List<String> customerIds) {
    List<String> orders = new ArrayList<>();
    for (String customerId : customerIds) {
        orders.addAll(findOrdersByCustomer(customerId));
    }
    return orders;
}
```

### Ticket Context

- **Status:** `MATCHED`
- **Provider:** `jira`
- **Key:** `DEBT2TEST-7`
- **URL:** `https://debt2test.atlassian.net/browse/DEBT2TEST-7`
- **Summary:** Batch OrderRepository customer lookups to remove N+1 queries

**Description:**

```
OrderRepository.findOrdersForCustomers() loops over customer IDs and issues one simulated query per customer instead of a single batched lookup, causing an N+1 query pattern.

Debt context (SATD): OrderRepository — TODO comment notes this was written for a single-customer admin screen and was never revisited when the bulk "customer orders export" feature reused it, where it now issues hundreds of queries per export.

Acceptance Criteria:

* Replace the per-ID loop with a single batched query (e.g., WHERE customer_id IN (...)).
* findOrdersForCustomers() issues one query regardless of input size.
* Test covers a multi-customer call and asserts the query/call count.
```

**Issue References:**

- `DEBT2TEST-7` (source: `comment`)

### Generated Test Case

```java
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for DEBT2TEST-7: OrderRepository.findOrdersForCustomers() issues one query
 * per customer (N+1). The expected design is a single batched lookup
 * (WHERE customer_id IN (...)) regardless of input size.
 *
 * Assumptions about the refactored design:
 *  - The per-customer query remains available as findOrdersByCustomer(String).
 *  - The batched query is exposed as findOrdersByCustomerIds(List<String>) and is
 *    overridable (public or protected) so query invocations can be counted.
 *
 * These tests are expected to FAIL against the current N+1 implementation and
 * PASS once the loop is replaced with a single batched query.
 */
class OrderRepositoryFindOrdersForCustomersTest {

    private CountingOrderRepository repository;

    @BeforeEach
    void setUp() {
        repository = new CountingOrderRepository();
    }

    @Nested
    @DisplayName("findOrdersForCustomers query count (DEBT2TEST-7)")
    class QueryCountTests {

        @Test
        @DisplayName("multi-customer call returns all orders using a single batched query")
        void multiCustomerCallIssuesSingleBatchedQuery() {
            List<String> customerIds = List.of("C1", "C2", "C3", "C4", "C5");
            repository.seedOrdersFor("C1", "O-1a", "O-1b");
            repository.seedOrdersFor("C2", "O-2a");
            repository.seedOrdersFor("C3");
            repository.seedOrdersFor("C4", "O-4a", "O-4b", "O-4c");
            repository.seedOrdersFor("C5", "O-5a");

            List<String> result = repository.findOrdersForCustomers(customerIds);

            assertEquals(7, result.size(), "all orders for all customers should be returned");
            assertTrue(result.containsAll(List.of("O-1a", "O-1b", "O-2a", "O-4a", "O-4b", "O-4c", "O-5a")));

            assertEquals(0, repository.perCustomerQueryCount,
                    "must not issue one query per customer (N+1)");
            assertEquals(1, repository.batchedQueryCount,
                    "must issue exactly one batched query");
        }

        @Test
        @DisplayName("query count stays at one regardless of input size")
        void queryCountIndependentOfInputSize() {
            List<String> customerIds = IntStream.rangeClosed(1, 500)
                    .mapToObj(i -> "C" + i)
                    .collect(Collectors.toList());
            customerIds.forEach(id -> repository.seedOrdersFor(id, id + "-order"));

            List<String> result = repository.findOrdersForCustomers(customerIds);

            assertEquals(500, result.size());
            assertEquals(1, repository.batchedQueryCount,
                    "a 500-customer export must still use a single batched query");
            assertEquals(0, repository.perCustomerQueryCount,
                    "no per-customer queries should be issued");
        }

        @Test
        @DisplayName("single customer call still uses one batched query")
        void singleCustomerUsesOneBatchedQuery() {
            repository.seedOrdersFor("C1", "O-1a");

            List<String> result = repository.findOrdersForCustomers(List.of("C1"));

            assertEquals(List.of("O-1a"), result);
            assertEquals(1, repository.batchedQueryCount);
            assertEquals(0, repository.perCustomerQueryCount);
        }

        @Test
        @DisplayName("empty input returns empty result without querying")
        void emptyInputIssuesNoQueries() {
            List<String> result = repository.findOrdersForCustomers(Collections.emptyList());

            assertTrue(result.isEmpty());
            assertEquals(0, repository.batchedQueryCount + repository.perCustomerQueryCount,
                    "no query should be issued for empty input");
        }
    }

    /**
     * In-memory repository that counts query invocations. Per-customer lookups and
     * batched lookups are tracked separately so the N+1 pattern is detectable.
     */
    static class CountingOrderRepository extends OrderRepository {

        private final java.util.Map<String, List<String>> store = new java.util.HashMap<>();
        int perCustomerQueryCount = 0;
        int batchedQueryCount = 0;

        void seedOrdersFor(String customerId, String... orderIds) {
            store.computeIfAbsent(customerId, k -> new ArrayList<>())
                    .addAll(List.of(orderIds));
        }

        @Override
        public List<String> findOrdersByCustomer(String customerId) {
            perCustomerQueryCount++;
            return new ArrayList<>(store.getOrDefault(customerId, Collections.emptyList()));
        }

        public List<String> findOrdersByCustomerIds(List<String> customerIds) {
            batchedQueryCount++;
            List<String> orders = new ArrayList<>();
            for (String id : customerIds) {
                orders.addAll(store.getOrDefault(id, Collections.emptyList()));
            }
            return orders;
        }
    }
}
```

---

## AuditLogger.java -> logRequest()

- **Debt Type:** `DESIGN`
- **Line Number:** `14`
- **Status:** `GENERATED`
- **Comment:** `* FIXME DEBT2TEST-6: bug, logged entries contain the raw password and card number in plaintext.      * The request is logged via toString() with no redaction step.`

```java
/**
 * FIXME DEBT2TEST-6: bug, logged entries contain the raw password and card number in plaintext.
 * The request is logged via toString() with no redaction step.
 */
public void logRequest(Request request) {
    sink.add(request.toString());
}
```

### Ticket Context

- **Status:** `MATCHED`
- **Provider:** `jira`
- **Key:** `DEBT2TEST-6`
- **URL:** `https://debt2test.atlassian.net/browse/DEBT2TEST-6`
- **Summary:** Redact sensitive data from audit logs

**Description:**

```
AuditLogger.logRequest() writes full request objects to the log via toString(), including raw passwords and credit card numbers, in plaintext.

Debt context (SATD): AuditLogger — FIXME comment flags this as a compliance risk (PCI-DSS/GDPR) introduced during the 2023 audit-trail rollout, since logs are shipped to a third-party aggregator.

Acceptance Criteria:

* Introduce a redaction step (field allowlist or masking) before any object is logged.
* Sensitive fields (password, cardNumber, ssn, etc.) never reach the log sink in plaintext.
* Unit test asserts logged output does not contain raw sensitive values.
```

**Issue References:**

- `DEBT2TEST-6` (source: `comment`)

### Generated Test Case

```java
import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests for DEBT2TEST-6: AuditLogger.logRequest() must not write raw sensitive
 * values (password, card number, SSN) to the log sink.
 *
 * Assumed API (adjust to the actual codebase if names differ):
 *   - AuditLogger(List<String> sink)
 *   - Request(String userId, String password, String cardNumber, String ssn, String action)
 */
class AuditLoggerRedactionTest {

    private static final String RAW_PASSWORD = "S3cr3t!Passw0rd";
    private static final String RAW_CARD = "4111111111111111";
    private static final String RAW_SSN = "123-45-6789";
    private static final String USER_ID = "user-42";
    private static final String ACTION = "TRANSFER_FUNDS";

    private List<String> sink;
    private AuditLogger auditLogger;

    @BeforeEach
    void setUp() {
        sink = new ArrayList<>();
        auditLogger = new AuditLogger(sink);
    }

    @Nested
    @DisplayName("DEBT2TEST-6: sensitive fields never reach the log sink in plaintext")
    class SensitiveDataRedaction {

        @Test
        @DisplayName("raw password is not written to the sink")
        void passwordIsNotLogged() {
            Request request = new Request(USER_ID, RAW_PASSWORD, RAW_CARD, RAW_SSN, ACTION);

            auditLogger.logRequest(request);

            assertFalse(sinkContents().contains(RAW_PASSWORD),
                    "Raw password must not appear in audit log output");
        }

        @Test
        @DisplayName("raw card number is not written to the sink")
        void cardNumberIsNotLogged() {
            Request request = new Request(USER_ID, RAW_PASSWORD, RAW_CARD, RAW_SSN, ACTION);

            auditLogger.logRequest(request);

            assertFalse(sinkContents().contains(RAW_CARD),
                    "Raw card number must not appear in audit log output");
        }

        @Test
        @DisplayName("raw SSN is not written to the sink")
        void ssnIsNotLogged() {
            Request request = new Request(USER_ID, RAW_PASSWORD, RAW_CARD, RAW_SSN, ACTION);

            auditLogger.logRequest(request);

            assertFalse(sinkContents().contains(RAW_SSN),
                    "Raw SSN must not appear in audit log output");
        }

        @Test
        @DisplayName("no sensitive value appears anywhere in the logged output")
        void noSensitiveValueLeaks() {
            Request request = new Request(USER_ID, RAW_PASSWORD, RAW_CARD, RAW_SSN, ACTION);

            auditLogger.logRequest(request);

            String output = sinkContents();
            assertAll(
                    () -> assertFalse(output.contains(RAW_PASSWORD), "password leaked"),
                    () -> assertFalse(output.contains(RAW_CARD), "card number leaked"),
                    () -> assertFalse(output.contains(RAW_SSN), "SSN leaked")
            );
        }

        @Test
        @DisplayName("sensitive values are not logged even when they appear as field names in toString()")
        void rawSensitiveValuesAbsentFromEntryList() {
            Request request = new Request(USER_ID, RAW_PASSWORD, RAW_CARD, RAW_SSN, ACTION);

            auditLogger.logRequest(request);

            assertFalse(sink.isEmpty(), "Expected exactly one audit entry to be written");
            for (String entry : sink) {
                assertFalse(entry.contains(RAW_PASSWORD), "password leaked in entry: " + entry);
                assertFalse(entry.contains(RAW_CARD), "card number leaked in entry: " + entry);
                assertFalse(entry.contains(RAW_SSN), "SSN leaked in entry: " + entry);
            }
        }

        @ParameterizedTest(name = "sensitive value [{0}] is redacted")
        @ValueSource(strings = {RAW_PASSWORD, RAW_CARD, RAW_SSN})
        void eachSensitiveValueIsRedacted(String sensitiveValue) {
            Request request = new Request(USER_ID, RAW_PASSWORD, RAW_CARD, RAW_SSN, ACTION);

            auditLogger.logRequest(request);

            assertFalse(sinkContents().contains(sensitiveValue),
                    "Sensitive value must not appear in plaintext: " + sensitiveValue);
        }
    }

    @Nested
    @DisplayName("Redaction must not remove non-sensitive audit information")
    class NonSensitiveDataPreserved {

        @Test
        @DisplayName("audit entry is still written")
        void entryIsWritten() {
            Request request = new Request(USER_ID, RAW_PASSWORD, RAW_CARD, RAW_SSN, ACTION);

            auditLogger.logRequest(request);

            assertEquals(1, sink.size(), "Exactly one audit entry should be written per request");
        }

        @Test
        @DisplayName("non-sensitive identifying fields remain in the audit entry")
        void nonSensitiveFieldsRetained() {
            Request request = new Request(USER_ID, RAW_PASSWORD, RAW_CARD, RAW_SSN, ACTION);

            auditLogger.logRequest(request);

            String output = sinkContents();
            assertAll(
                    () -> assertTrue(output.contains(USER_ID), "userId should remain in audit log"),
                    () -> assertTrue(output.contains(ACTION), "action should remain in audit log")
            );
        }
    }

    @Test
    @DisplayName("redaction does not mutate the original request object")
    void originalRequestIsNotMutated() {
        Request request = new Request(USER_ID, RAW_PASSWORD, RAW_CARD, RAW_SSN, ACTION);

        auditLogger.logRequest(request);

        assertAll(
                () -> assertEquals(RAW_PASSWORD, request.getPassword()),
                () -> assertEquals(RAW_CARD, request.getCardNumber()),
                () -> assertEquals(RAW_SSN, request.getSsn())
        );
    }

    private String sinkContents() {
        return String.join("\n", sink);
    }
}
```

---

## CacheManagerTest.java -> shouldReportCorrectSize()

- **Debt Type:** `TEST`
- **Line Number:** `36`
- **Status:** `GENERATED`
- **Comment:** `TODO DEBT2TEST-4: add tests, no test covers eviction under memory pressure because the cache has no eviction policy or maxSize limit yet.`

```java
// TODO DEBT2TEST-4: add tests, no test covers eviction under memory pressure because
// the cache has no eviction policy or maxSize limit yet.
@Test
void shouldReportCorrectSize() {
    cacheManager.put("a", 1);
    cacheManager.put("b", 2);
    assertEquals(2, cacheManager.size());
}
```

### Ticket Context

- **Status:** `MATCHED`
- **Provider:** `jira`
- **Key:** `DEBT2TEST-4`
- **URL:** `https://debt2test.atlassian.net/browse/DEBT2TEST-4`
- **Summary:** Implement bounded LRU cache

**Description:**

```
{{CacheManager}} uses an unbounded {{HashMap}} with no eviction policy, risking {{OutOfMemoryError}} in production. Reported by DevOps after an incident on 2024-01-15.

*Debt context (SATD):* {{CacheManager}} — TODO comment calls for an LRU cache with a max size limit.

*Acceptance Criteria:*

* Replace the unbounded {{HashMap}} with a bounded LRU cache (e.g., {{LinkedHashMap}} in access-order mode, or a dedicated LRU structure).
* Cache exposes a configurable {{maxSize}}.
* Unit tests confirm eviction occurs once {{maxSize}} is exceeded.
```

**Issue References:**

- `DEBT2TEST-4` (source: `comment`)

### Generated Test Case

```java
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for CacheManager's bounded LRU behavior (resolves DEBT2TEST-4).
 *
 * Assumed CacheManager API (adjust if the real signatures differ):
 *   - CacheManager(int maxSize)
 *   - void put(K key, V value)
 *   - V get(K key)
 *   - boolean containsKey(K key)
 *   - int size()
 *   - int getMaxSize()
 *
 * Eviction is expected to be least-recently-used: reads and writes refresh an
 * entry's recency, and inserting beyond maxSize evicts the least recently used entry.
 */
class CacheManagerTest {

    private static final int MAX_SIZE = 3;

    private CacheManager<String, Integer> cacheManager;

    @BeforeEach
    void setUp() {
        cacheManager = new CacheManager<>(MAX_SIZE);
    }

    @Test
    void shouldReportCorrectSize() {
        cacheManager.put("a", 1);
        cacheManager.put("b", 2);
        assertEquals(2, cacheManager.size());
    }

    @Test
    void shouldExposeConfiguredMaxSize() {
        assertEquals(MAX_SIZE, cacheManager.getMaxSize());
    }

    @Test
    void shouldRejectNonPositiveMaxSize() {
        assertThrows(IllegalArgumentException.class, () -> new CacheManager<String, Integer>(0));
        assertThrows(IllegalArgumentException.class, () -> new CacheManager<String, Integer>(-1));
    }

    @Nested
    class Eviction {

        @Test
        void shouldNotEvictWhileWithinMaxSize() {
            cacheManager.put("a", 1);
            cacheManager.put("b", 2);
            cacheManager.put("c", 3);

            assertEquals(MAX_SIZE, cacheManager.size());
            assertTrue(cacheManager.containsKey("a"));
            assertTrue(cacheManager.containsKey("b"));
            assertTrue(cacheManager.containsKey("c"));
        }

        @Test
        void shouldEvictOldestEntryWhenMaxSizeExceeded() {
            cacheManager.put("a", 1);
            cacheManager.put("b", 2);
            cacheManager.put("c", 3);
            cacheManager.put("d", 4);

            assertEquals(MAX_SIZE, cacheManager.size());
            assertFalse(cacheManager.containsKey("a"), "least recently used entry should be evicted");
            assertTrue(cacheManager.containsKey("b"));
            assertTrue(cacheManager.containsKey("c"));
            assertTrue(cacheManager.containsKey("d"));
        }

        @Test
        void shouldEvictLeastRecentlyUsedNotLeastRecentlyInserted() {
            cacheManager.put("a", 1);
            cacheManager.put("b", 2);
            cacheManager.put("c", 3);

            // Touch "a" so "b" becomes the least recently used entry
            assertEquals(1, cacheManager.get("a"));

            cacheManager.put("d", 4);

            assertTrue(cacheManager.containsKey("a"), "recently read entry must survive");
            assertFalse(cacheManager.containsKey("b"), "least recently used entry should be evicted");
            assertTrue(cacheManager.containsKey("c"));
            assertTrue(cacheManager.containsKey("d"));
        }

        @Test
        void shouldNotEvictWhenUpdatingExistingKeyAtCapacity() {
            cacheManager.put("a", 1);
            cacheManager.put("b", 2);
            cacheManager.put("c", 3);

            cacheManager.put("a", 100);

            assertEquals(MAX_SIZE, cacheManager.size());
            assertEquals(100, cacheManager.get("a"));
            assertTrue(cacheManager.containsKey("b"));
            assertTrue(cacheManager.containsKey("c"));
        }

        @Test
        void shouldReturnNullForEvictedKey() {
            cacheManager.put("a", 1);
            cacheManager.put("b", 2);
            cacheManager.put("c", 3);
            cacheManager.put("d", 4);

            assertNull(cacheManager.get("a"));
        }

        @Test
        void shouldKeepSizeBoundedUnderSustainedMemoryPressure() {
            int totalInserts = 10_000;

            for (int i = 0; i < totalInserts; i++) {
                cacheManager.put("key-" + i, i);
                assertTrue(cacheManager.size() <= MAX_SIZE,
                        "size must never exceed maxSize, was " + cacheManager.size() + " at insert " + i);
            }

            assertEquals(MAX_SIZE, cacheManager.size());

            // Only the most recent MAX_SIZE entries should remain
            for (int i = totalInserts - MAX_SIZE; i < totalInserts; i++) {
                assertTrue(cacheManager.containsKey("key-" + i), "recent key-" + i + " should be retained");
            }
            assertFalse(cacheManager.containsKey("key-0"), "oldest key should be evicted");
        }

        @Test
        void shouldEvictInInsertionOrderWhenNoReadsOccur() {
            for (int i = 0; i < MAX_SIZE + 5; i++) {
                cacheManager.put("k" + i, i);
            }

            for (int i = 0; i < 5; i++) {
                assertFalse(cacheManager.containsKey("k" + i), "k" + i + " should have been evicted");
            }
            for (int i = 5; i < MAX_SIZE + 5; i++) {
                assertTrue(cacheManager.containsKey("k" + i), "k" + i + " should be retained");
            }
        }
    }
}
```

---

## ConnectionPoolTest.java -> shouldNotExceedPoolSizeOnRelease()

- **Debt Type:** `TEST`
- **Line Number:** `32`
- **Status:** `GENERATED`
- **Comment:** `TODO DEBT2TEST-3: add tests for concurrent access; it is untested because the pool lacks synchronization and a concurrency test would be flaky.`

```java
// TODO DEBT2TEST-3: add tests for concurrent access; it is untested because the pool
// lacks synchronization and a concurrency test would be flaky.
@Test
void shouldNotExceedPoolSizeOnRelease() throws InterruptedException {
    ConnectionPool.DbConnection conn = connectionPool.getConnection();
    connectionPool.releaseConnection(conn);
    connectionPool.releaseConnection(conn);
    assertTrue(true);
}
```

### Ticket Context

- **Status:** `MATCHED`
- **Provider:** `jira`
- **Key:** `DEBT2TEST-3`
- **URL:** `https://debt2test.atlassian.net/browse/DEBT2TEST-3`
- **Summary:** Fix ConnectionPool race condition

**Description:**

```
{{ConnectionPool.getConnection()}} and {{releaseConnection()}} access the shared {{availableConnections}} queue without synchronization, risking duplicate leases, lost connections, and pool exhaustion under load.

*Debt context (SATD):* {{ConnectionPool}} — BUG comment blocks deployment to high-concurrency environments (10k+ RPS).

*Acceptance Criteria:*

* Replace {{LinkedList}} with {{ConcurrentLinkedQueue}}, or add proper synchronization around both methods.
* Add a concurrency test that exercises concurrent {{getConnection}}/{{releaseConnection}} calls without lost or duplicated connections.
```

**Issue References:**

- `DEBT2TEST-3` (source: `comment`)

### Generated Test Case

```java
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * Concurrency tests for ConnectionPool (DEBT2TEST-3).
 *
 * Assumptions about the ConnectionPool API (adjust if the real API differs):
 *   - constructor: new ConnectionPool(int size)
 *   - DbConnection is a nested type ConnectionPool.DbConnection
 *   - getConnection() returns a leased connection (it may block or return null when empty)
 *   - releaseConnection(DbConnection) returns a connection to the pool
 *
 * Identity of leased connections is tracked via ConcurrentHashMap key sets, which rely on
 * equals/hashCode. If DbConnection overrides equals, switch to an identity-based set.
 */
@Timeout(value = 60, unit = SECONDS)
class ConnectionPoolConcurrencyTest {

    private static final int POOL_SIZE = 10;
    private static final int THREADS = 8;
    private static final int ITERATIONS_PER_THREAD = 500;
    private static final long ACQUIRE_PROBE_MILLIS = 200;

    private ConnectionPool connectionPool;
    private ExecutorService executor;

    @BeforeEach
    void setUp() {
        connectionPool = new ConnectionPool(POOL_SIZE);
        executor = Executors.newFixedThreadPool(THREADS);
    }

    @AfterEach
    void tearDown() throws InterruptedException {
        executor.shutdownNow();
        executor.awaitTermination(5, SECONDS);
    }

    @Test
    void shouldNeverLeaseSameConnectionToTwoThreadsConcurrently() throws Exception {
        Set<ConnectionPool.DbConnection> currentlyLeased = ConcurrentHashMap.newKeySet();
        AtomicInteger duplicateLeases = new AtomicInteger();
        AtomicReference<Throwable> workerFailure = new AtomicReference<>();
        CountDownLatch start = new CountDownLatch(1);
        List<Future<?>> futures = new ArrayList<>();

        for (int t = 0; t < THREADS; t++) {
            futures.add(executor.submit(() -> {
                start.await();
                for (int i = 0; i < ITERATIONS_PER_THREAD; i++) {
                    ConnectionPool.DbConnection conn = connectionPool.getConnection();
                    if (conn == null) {
                        continue;
                    }
                    if (!currentlyLeased.add(conn)) {
                        duplicateLeases.incrementAndGet();
                    }
                    currentlyLeased.remove(conn);
                    connectionPool.releaseConnection(conn);
                }
                return null;
            }));
        }

        start.countDown();
        awaitAll(futures, workerFailure);

        assertEquals(0, duplicateLeases.get(),
                "The same connection was leased to more than one thread at the same time");
    }

    @Test
    void shouldNotLoseConnectionsUnderConcurrentAcquireAndRelease() throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        AtomicReference<Throwable> workerFailure = new AtomicReference<>();
        List<Future<?>> futures = new ArrayList<>();

        for (int t = 0; t < THREADS; t++) {
            futures.add(executor.submit(() -> {
                start.await();
                for (int i = 0; i < ITERATIONS_PER_THREAD; i++) {
                    ConnectionPool.DbConnection conn = connectionPool.getConnection();
                    if (conn != null) {
                        connectionPool.releaseConnection(conn);
                    }
                }
                return null;
            }));
        }

        start.countDown();
        awaitAll(futures, workerFailure);

        // After the workload every connection must be back in the pool: exactly POOL_SIZE
        // distinct connections must be obtainable, and no more.
        Set<ConnectionPool.DbConnection> drained = drainPool(POOL_SIZE + 1);
        assertEquals(POOL_SIZE, drained.size(),
                "Connections were lost or duplicated after concurrent acquire/release");
    }

    @Test
    void shouldNotExceedPoolSizeOnRelease() throws Exception {
        ConnectionPool.DbConnection conn = connectionPool.getConnection();
        assertNotNull(conn, "Pool should lease a connection when it is fresh");

        connectionPool.releaseConnection(conn);
        connectionPool.releaseConnection(conn); // double release must not inflate the pool

        Set<ConnectionPool.DbConnection> drained = drainPool(POOL_SIZE + 1);
        assertEquals(POOL_SIZE, drained.size(),
                "Double release must not add duplicate entries to the pool");
    }

    @Test
    void shouldServeAllConnectionsWhenReleasedConcurrentlyByManyThreads() throws Exception {
        // Lease the whole pool on the main thread, then release everything concurrently.
        List<ConnectionPool.DbConnection> leased = new ArrayList<>();
        for (int i = 0; i < POOL_SIZE; i++) {
            ConnectionPool.DbConnection conn = connectionPool.getConnection();
            assertNotNull(conn, "Pool should lease connection " + i);
            leased.add(conn);
        }

        CountDownLatch start = new CountDownLatch(1);
        AtomicReference<Throwable> workerFailure = new AtomicReference<>();
        List<Future<?>> futures = new ArrayList<>();
        for (ConnectionPool.DbConnection conn : leased) {
            futures.add(executor.submit(() -> {
                start.await();
                connectionPool.releaseConnection(conn);
                return null;
            }));
        }

        start.countDown();
        awaitAll(futures, workerFailure);

        Set<ConnectionPool.DbConnection> drained = drainPool(POOL_SIZE + 1);
        assertEquals(POOL_SIZE, drained.size(),
                "Concurrent releases lost connections or created duplicates");
    }

    /**
     * Acquires up to maxAttempts connections, each probed on a worker thread with a short
     * timeout so that a blocking pool cannot hang the test. Returns the distinct connections
     * obtained. Note: a timed-out probe is cancelled with interruption; if getConnection()
     * later completes anyway, that connection is not returned to the test.
     */
    private Set<ConnectionPool.DbConnection> drainPool(int maxAttempts) throws Exception {
        Set<ConnectionPool.DbConnection> distinct = ConcurrentHashMap.newKeySet();
        for (int i = 0; i < maxAttempts; i++) {
            ConnectionPool.DbConnection conn = tryAcquire();
            if (conn == null) {
                break;
            }
            distinct.add(conn);
        }
        return distinct;
    }

    private ConnectionPool.DbConnection tryAcquire() throws Exception {
        Future<ConnectionPool.DbConnection> probe = executor.submit(connectionPool::getConnection);
        try {
            return probe.get(ACQUIRE_PROBE_MILLIS, MILLISECONDS);
        } catch (TimeoutException e) {
            probe.cancel(true);
            return null;
        } catch (ExecutionException e) {
            throw new AssertionError("getConnection() failed unexpectedly", e.getCause());
        }
    }

    private void awaitAll(List<Future<?>> futures, AtomicReference<Throwable> workerFailure)
            throws Exception {
        for (Future<?> future : futures) {
            try {
                future.get(30, SECONDS);
            } catch (ExecutionException e) {
                workerFailure.compareAndSet(null, e.getCause());
            }
        }
        if (workerFailure.get() != null) {
            throw new AssertionError("Worker thread failed", workerFailure.get());
        }
    }
}
```

---

## OrderRepositoryTest.java -> currentlyIssuesOneQueryPerCustomer()

- **Debt Type:** `TEST`
- **Line Number:** `25`
- **Status:** `GENERATED`
- **Comment:** `TODO DEBT2TEST-7: add tests for a single batched lookup; no test covers it, this one only pins the current one-query-per-customer behavior.`

```java
// TODO DEBT2TEST-7: add tests for a single batched lookup; no test covers it, this one
// only pins the current one-query-per-customer behavior.
@Test
void currentlyIssuesOneQueryPerCustomer() {
    orderRepository.findOrdersForCustomers(List.of("c1", "c2", "c3"));
    assertEquals(3, orderRepository.getQueryCount());
}
```

### Ticket Context

- **Status:** `MATCHED`
- **Provider:** `jira`
- **Key:** `DEBT2TEST-7`
- **URL:** `https://debt2test.atlassian.net/browse/DEBT2TEST-7`
- **Summary:** Batch OrderRepository customer lookups to remove N+1 queries

**Description:**

```
OrderRepository.findOrdersForCustomers() loops over customer IDs and issues one simulated query per customer instead of a single batched lookup, causing an N+1 query pattern.

Debt context (SATD): OrderRepository — TODO comment notes this was written for a single-customer admin screen and was never revisited when the bulk "customer orders export" feature reused it, where it now issues hundreds of queries per export.

Acceptance Criteria:

* Replace the per-ID loop with a single batched query (e.g., WHERE customer_id IN (...)).
* findOrdersForCustomers() issues one query regardless of input size.
* Test covers a multi-customer call and asserts the query/call count.
```

**Issue References:**

- `DEBT2TEST-7` (source: `comment`)

### Generated Test Case

```java
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderRepositoryBatchedLookupTest {

    private OrderRepository orderRepository;

    @BeforeEach
    void setUp() {
        // Fresh instance so the query counter starts at zero for every test.
        orderRepository = new OrderRepository();
    }

    @Test
    void multiCustomerLookupIssuesExactlyOneBatchedQuery() {
        orderRepository.findOrdersForCustomers(List.of("c1", "c2", "c3"));

        assertEquals(1, orderRepository.getQueryCount(),
                "Expected a single batched query (e.g. WHERE customer_id IN (...)) for multiple customers");
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 10, 50, 500})
    void queryCountIsConstantRegardlessOfInputSize(int customerCount) {
        List<String> customerIds = IntStream.rangeClosed(1, customerCount)
                .mapToObj(i -> "c" + i)
                .collect(Collectors.toList());

        orderRepository.findOrdersForCustomers(customerIds);

        assertEquals(1, orderRepository.getQueryCount(),
                "Query count must not scale with the number of customers (" + customerCount + ")");
    }

    @Test
    void batchedLookupDoesNotThrowForMultipleCustomers() {
        List<String> customerIds = List.of("c1", "c2", "c3", "c4");

        assertDoesNotThrow(() -> orderRepository.findOrdersForCustomers(customerIds));
    }

    @Test
    void emptyCustomerListDoesNotIssuePerCustomerQueries() {
        orderRepository.findOrdersForCustomers(List.of());

        assertTrue(orderRepository.getQueryCount() <= 1,
                "An empty input should issue at most one (or zero) queries");
    }
}
```

---

## AuditLoggerTest.java -> currentlyLogsSensitiveFieldsInPlaintext()

- **Debt Type:** `DOCUMENTATION`
- **Line Number:** `24`
- **Status:** `GENERATED`
- **Comment:** `TODO DEBT2TEST-6: document why this test asserts the plaintext password; explain that it pins the current unredacted logging and must be inverted once redaction exists.`

```java
// TODO DEBT2TEST-6: document why this test asserts the plaintext password; explain that
// it pins the current unredacted logging and must be inverted once redaction exists.
@Test
void currentlyLogsSensitiveFieldsInPlaintext() {
    auditLogger.logRequest(new AuditLogger.Request("alice", "s3cr3t", "4111111111111111"));
    String entry = auditLogger.getLoggedEntries().get(0);
    assertTrue(entry.contains("s3cr3t"));
    assertTrue(entry.contains("4111111111111111"));
}
```

### Ticket Context

- **Status:** `MATCHED`
- **Provider:** `jira`
- **Key:** `DEBT2TEST-6`
- **URL:** `https://debt2test.atlassian.net/browse/DEBT2TEST-6`
- **Summary:** Redact sensitive data from audit logs

**Description:**

```
AuditLogger.logRequest() writes full request objects to the log via toString(), including raw passwords and credit card numbers, in plaintext.

Debt context (SATD): AuditLogger — FIXME comment flags this as a compliance risk (PCI-DSS/GDPR) introduced during the 2023 audit-trail rollout, since logs are shipped to a third-party aggregator.

Acceptance Criteria:

* Introduce a redaction step (field allowlist or masking) before any object is logged.
* Sensitive fields (password, cardNumber, ssn, etc.) never reach the log sink in plaintext.
* Unit test asserts logged output does not contain raw sensitive values.
```

**Issue References:**

- `DEBT2TEST-6` (source: `comment`)

### Generated Test Case

```java
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Characterization tests for {@link AuditLogger}.
 *
 * <p><b>Known compliance debt (DEBT2TEST-6):</b> {@code AuditLogger.logRequest()} currently
 * serializes the full request object via {@code toString()}. Sensitive fields such as
 * passwords and card numbers are written to the audit log in plaintext. Logs are shipped to a
 * third-party aggregator, which creates a PCI-DSS and GDPR compliance risk. This was introduced
 * during the 2023 audit-trail rollout.
 *
 * <p>The tests in this class pin that current, unredacted behavior so the defect is visible in
 * the test suite rather than silently assumed to be correct.
 */
class AuditLoggerTest {

    private AuditLogger auditLogger;

    @BeforeEach
    void setUp() {
        auditLogger = new AuditLogger();
    }

    /**
     * Pins the current unredacted logging behavior.
     *
     * <p><b>Why this test asserts plaintext sensitive values:</b> it is a characterization test.
     * It documents that {@code logRequest()} currently writes the raw password ("s3cr3t") and raw
     * card number ("4111111111111111") into the logged entry. It does not assert that this is
     * desirable. It exists so that the existing behavior is explicitly recorded and so that any
     * change to logging is detected.
     *
     * <p><b>Why it must be inverted:</b> once a redaction step (field allowlist or masking) is
     * introduced before any object is logged, this test becomes a regression that enforces the
     * defect. It must then be replaced with assertions that sensitive values do NOT appear in
     * plaintext, for example:
     * <pre>{@code
     * assertFalse(entry.contains("s3cr3t"));
     * assertFalse(entry.contains("4111111111111111"));
     * assertTrue(entry.contains("alice"));
     * }</pre>
     *
     * <p>Until that change lands, a failure of this test that is caused by redaction is expected
     * and should be treated as the signal to update this test, not as a reason to revert the
     * redaction.
     */
    @Test
    void currentlyLogsSensitiveFieldsInPlaintext() {
        auditLogger.logRequest(new AuditLogger.Request("alice", "s3cr3t", "4111111111111111"));
        String entry = auditLogger.getLoggedEntries().get(0);

        // Current (defective) behavior: raw sensitive values appear in the log entry.
        assertTrue(entry.contains("s3cr3t"));
        assertTrue(entry.contains("4111111111111111"));
    }
}
```

---

## PaymentProcessorTest.java -> shouldFailOnNegativeAmountAfterRetries()

- **Debt Type:** `TEST`
- **Line Number:** `24`
- **Status:** `GENERATED`
- **Comment:** `TODO DEBT2TEST-2: add tests for the retry path; it is untested directly because the hardcoded gateway timeout cannot be injected.`

```java
// TODO DEBT2TEST-2: add tests for the retry path; it is untested directly because the
// hardcoded gateway timeout cannot be injected.
@Test
void shouldFailOnNegativeAmountAfterRetries() {
    PaymentProcessor.Order order = new PaymentProcessor.Order("order-2", -50.0);
    PaymentProcessor.PaymentResult result = paymentProcessor.process(order);
    assertFalse(result.success());
}
```

### Ticket Context

- **Status:** `MATCHED`
- **Provider:** `jira`
- **Key:** `DEBT2TEST-2`
- **URL:** `https://debt2test.atlassian.net/browse/DEBT2TEST-2`
- **Summary:** Refactor payment timeout configuration

**Description:**

```
The payment gateway timeout is hardcoded ({{GATEWAY_TIMEOUT_MS = 5000}}) directly in {{PaymentProcessor}}, mixing configuration with business logic and making the retry path untestable without manual intervention.

*Debt context (SATD):* {{PaymentProcessor}} — FIXME comment calls for extracting the timeout into a separate configuration class.

*Acceptance Criteria:*

* Extract {{GATEWAY_TIMEOUT_MS}} and {{MAX_RETRY_ATTEMPTS}} into an injectable configuration object.
* {{PaymentProcessor}} receives configuration via constructor/DI instead of static constants.
* Retry/timeout behavior is covered by unit tests using injected configuration values.
```

**Issue References:**

- `DEBT2TEST-2` (source: `comment`)

### Generated Test Case

```java
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

/*
 * Retry-path tests for PaymentProcessor (DEBT2TEST-2).
 *
 * Assumptions about the existing API (based on the method under test):
 *   - PaymentProcessor has an accessible no-arg constructor.
 *   - PaymentProcessor.Order(String id, double amount) exists.
 *   - PaymentProcessor.process(Order) returns PaymentResult.
 *   - PaymentProcessor.PaymentResult exposes success().
 *
 * The retry count and gateway timeout are still hardcoded (GATEWAY_TIMEOUT_MS = 5000,
 * MAX_RETRY_ATTEMPTS). These tests exercise the retry path through observable
 * behavior only. Once the configuration object is injected, add tests that
 * supply custom timeout and retry values and verify the exact attempt count.
 */
class PaymentProcessorRetryTest {

    private static final long HARDCODED_GATEWAY_TIMEOUT_MS = 5000L;

    private PaymentProcessor paymentProcessor;

    @BeforeEach
    void setUp() {
        paymentProcessor = new PaymentProcessor();
    }

    @Test
    void shouldFailOnNegativeAmountAfterRetries() {
        PaymentProcessor.Order order = new PaymentProcessor.Order("order-2", -50.0);
        PaymentProcessor.PaymentResult result = paymentProcessor.process(order);
        assertFalse(result.success());
    }

    @ParameterizedTest
    @ValueSource(doubles = {-0.01, -1.0, -50.0, -1000.0})
    void shouldFailForAnyNegativeAmountAfterRetries(double amount) {
        PaymentProcessor.Order order = new PaymentProcessor.Order("order-neg", amount);
        PaymentProcessor.PaymentResult result = paymentProcessor.process(order);
        assertFalse(result.success(),
                "Negative amount " + amount + " must not succeed after retries");
    }

    @Test
    void shouldNotThrowWhenRetryingNegativeAmount() {
        PaymentProcessor.Order order = new PaymentProcessor.Order("order-throw", -50.0);
        assertDoesNotThrow(() -> paymentProcessor.process(order));
    }

    @Test
    void shouldReturnWithinGatewayTimeoutWhenRetryingNegativeAmount() {
        PaymentProcessor.Order order = new PaymentProcessor.Order("order-timing", -50.0);
        assertTimeoutPreemptively(Duration.ofMillis(HARDCODED_GATEWAY_TIMEOUT_MS), () -> {
            PaymentProcessor.PaymentResult result = paymentProcessor.process(order);
            assertFalse(result.success());
        });
    }

    @Test
    void shouldReturnFailedResultObjectNotNullForNegativeAmount() {
        PaymentProcessor.Order order = new PaymentProcessor.Order("order-nonnull", -50.0);
        PaymentProcessor.PaymentResult result = paymentProcessor.process(order);
        assertNotNull(result);
        assertFalse(result.success());
    }
}
```

---

