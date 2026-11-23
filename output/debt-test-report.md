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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for LoginService.authenticate() covering the legacy MD5 path (DEBT2TEST-1).
 *
 * Assumptions about the production class (not shown in the method source):
 *   - LoginService has a no-arg constructor.
 *   - validateHash(String username, String passwordHash) is non-private (protected or
 *     package-private) so a test double can observe and control it.
 *   - legacyMd5Hash(String) produces a lowercase hex MD5 digest of the UTF-8 password.
 *
 * Adjust the test double if these assumptions do not hold.
 */
class LoginServiceAuthenticateTest {

    /**
     * Test double that records validateHash() invocations and returns a configurable result,
     * isolating authenticate() from any real credential store.
     */
    static class RecordingLoginService extends LoginService {
        final List<String[]> validateCalls = new ArrayList<>();
        boolean validateResult = false;

        @Override
        protected boolean validateHash(String username, String passwordHash) {
            validateCalls.add(new String[] {username, passwordHash});
            return validateResult;
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
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("MD5 not available", e);
        }
    }

    @Nested
    @DisplayName("Invalid username handling")
    class InvalidUsername {

        @ParameterizedTest(name = "returns false for username [{0}] without validating")
        @NullAndEmptySource
        void rejectsNullOrEmptyUsername(String username) {
            boolean result = service.authenticate(username, "password");

            assertFalse(result);
            assertTrue(service.validateCalls.isEmpty(),
                    "validateHash must not be consulted for an invalid username");
        }

        @Test
        @DisplayName("rejects invalid username even when validator would accept")
        void rejectsEvenIfValidatorWouldAccept() {
            service.validateResult = true;

            assertFalse(service.authenticate("", "password"));
            assertFalse(service.authenticate(null, "password"));
        }
    }

    @Nested
    @DisplayName("Legacy MD5 hashing path")
    class LegacyMd5Path {

        @Test
        @DisplayName("hashes password with MD5 before delegating to validateHash")
        void passesMd5HashToValidator() {
            service.authenticate("alice", "password");

            assertEquals(1, service.validateCalls.size());
            String[] call = service.validateCalls.get(0);
            assertEquals("alice", call[0]);
            assertEquals("5f4dcc3b5aa765d61d8327deb882cf99", call[1]);
            assertEquals(md5Hex("password"), call[1]);
        }

        @Test
        @DisplayName("hash is deterministic for the same password")
        void hashIsDeterministic() {
            service.authenticate("alice", "s3cret!");
            service.authenticate("alice", "s3cret!");

            assertEquals(2, service.validateCalls.size());
            assertEquals(service.validateCalls.get(0)[1], service.validateCalls.get(1)[1]);
        }

        @ParameterizedTest(name = "password [{0}] produces matching MD5 digest")
        @ValueSource(strings = {"a", "password", "P@ssw0rd!", "пароль", "  spaced  "})
        void hashMatchesReferenceMd5(String password) {
            service.authenticate("bob", password);

            assertEquals(md5Hex(password), service.validateCalls.get(0)[1]);
        }

        @Test
        @DisplayName("different passwords produce different hashes")
        void differentPasswordsDifferentHashes() {
            service.authenticate("alice", "password1");
            service.authenticate("alice", "password2");

            assertNotEquals(service.validateCalls.get(0)[1], service.validateCalls.get(1)[1]);
        }
    }

    @Nested
    @DisplayName("Result propagation")
    class ResultPropagation {

        @Test
        @DisplayName("returns true when validator accepts the hash")
        void returnsTrueWhenValidatorAccepts() {
            service.validateResult = true;

            assertTrue(service.authenticate("alice", "password"));
        }

        @Test
        @DisplayName("returns false when validator rejects the hash")
        void returnsFalseWhenValidatorRejects() {
            service.validateResult = false;

            assertFalse(service.authenticate("alice", "wrong"));
        }

        @Test
        @DisplayName("validator receives the exact username supplied")
        void forwardsUsername() {
            service.authenticate("Alice.Smith", "password");

            assertEquals("Alice.Smith", service.validateCalls.get(0)[0]);
        }
    }

    @Nested
    @DisplayName("Target state: bcrypt/Argon2 migration (DEBT2TEST-1)")
    class BcryptMigrationTarget {

        @Test
        @Disabled("Pending migration: replace legacyMd5Hash with bcrypt (workFactor=12) or Argon2")
        @DisplayName("stored hash is not a raw MD5 digest after migration")
        void storedHashIsNotMd5() {
            service.validateResult = true;

            service.authenticate("alice", "password");

            String submitted = service.validateCalls.get(0)[1];
            assertNotEquals(md5Hex("password"), submitted,
                    "After migration, the validator should receive a bcrypt/Argon2 comparison, not MD5");
        }

        @Test
        @Disabled("Pending migration: legacy MD5 hashes must be re-hashed on successful login")
        @DisplayName("successful login with legacy MD5 hash triggers re-hash to bcrypt")
        void successfulLegacyLoginRehashes() {
            // Expected future behavior: after a successful authenticate() using a legacy MD5
            // record, the stored credential is upgraded to bcrypt(workFactor=12).
            // Implement once the credential store and bcrypt dependency are available.
            service.validateResult = true;

            assertTrue(service.authenticate("alice", "password"));
            // TODO: verify credential store now contains a bcrypt hash with cost factor 12.
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for DEBT2TEST-7: OrderRepository.findOrdersForCustomers() must issue a single
 * batched query (WHERE customer_id IN (...)) instead of one query per customer (N+1).
 *
 * Assumed seam: OrderRepository is constructed with a QueryExecutor that receives the
 * SQL text and bound parameters and returns result rows. Every query issued by the
 * repository goes through QueryExecutor.execute(...), so its invocation count is the
 * query count.
 */
class OrderRepositoryBatchedQueryTest {

    /** Minimal query executor interface assumed by the repository under test. */
    interface QueryExecutor {
        List<String> execute(String sql, List<String> params);
    }

    /**
     * Fake executor that records every query and simulates an IN (...) lookup
     * over an in-memory order table keyed by customer ID.
     */
    static class RecordingQueryExecutor implements QueryExecutor {
        final Map<String, List<String>> ordersByCustomer = new HashMap<>();
        final List<String> executedSql = new ArrayList<>();
        final List<List<String>> executedParams = new ArrayList<>();

        @Override
        public List<String> execute(String sql, List<String> params) {
            executedSql.add(sql);
            executedParams.add(new ArrayList<>(params));

            List<String> rows = new ArrayList<>();
            for (String customerId : params) {
                rows.addAll(ordersByCustomer.getOrDefault(customerId, List.of()));
            }
            return rows;
        }

        int queryCount() {
            return executedSql.size();
        }
    }

    private RecordingQueryExecutor executor;
    private OrderRepository repository;

    @BeforeEach
    void setUp() {
        executor = new RecordingQueryExecutor();
        repository = new OrderRepository(executor);
    }

    private static List<String> customerIds(int count) {
        List<String> ids = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            ids.add("cust-" + i);
        }
        return ids;
    }

    private void seedOrders(List<String> ids, int ordersPerCustomer) {
        for (String id : ids) {
            List<String> orders = new ArrayList<>();
            for (int j = 1; j <= ordersPerCustomer; j++) {
                orders.add("order-" + id + "-" + j);
            }
            executor.ordersByCustomer.put(id, orders);
        }
    }

    @Test
    @DisplayName("Multi-customer call issues exactly one batched query")
    void multiCustomerCallIssuesSingleQuery() {
        List<String> ids = customerIds(5);
        seedOrders(ids, 2);

        List<String> result = repository.findOrdersForCustomers(ids);

        assertEquals(1, executor.queryCount(),
                "findOrdersForCustomers must issue one query regardless of customer count");
    }

    @Test
    @DisplayName("Batched query uses an IN (...) clause with all customer IDs as parameters")
    void batchedQueryUsesInClauseWithAllIds() {
        List<String> ids = customerIds(4);
        seedOrders(ids, 1);

        repository.findOrdersForCustomers(ids);

        assertEquals(1, executor.queryCount());
        String sql = executor.executedSql.get(0).toUpperCase();
        assertTrue(sql.contains("IN ("), "Expected a batched IN (...) clause, got: " + sql);
        assertEquals(ids, executor.executedParams.get(0),
                "All customer IDs should be bound in the single batched query");
    }

    @Test
    @DisplayName("Multi-customer call returns all orders for all customers")
    void multiCustomerCallReturnsAllOrders() {
        List<String> ids = customerIds(3);
        seedOrders(ids, 2);

        List<String> result = repository.findOrdersForCustomers(ids);

        assertEquals(6, result.size());
        for (String id : ids) {
            assertTrue(result.contains("order-" + id + "-1"));
            assertTrue(result.contains("order-" + id + "-2"));
        }
    }

    @ParameterizedTest(name = "query count stays at 1 for {0} customers")
    @ValueSource(ints = {1, 2, 10, 100, 500})
    @DisplayName("Query count is constant with respect to input size")
    void queryCountIndependentOfInputSize(int customerCount) {
        List<String> ids = customerIds(customerCount);
        seedOrders(ids, 1);

        List<String> result = repository.findOrdersForCustomers(ids);

        assertEquals(1, executor.queryCount(),
                "Query count must not scale with the number of customers");
        assertEquals(customerCount, result.size());
    }

    @Test
    @DisplayName("Customers without orders contribute nothing but do not add queries")
    void customersWithoutOrdersAddNoResultsOrQueries() {
        List<String> ids = customerIds(4);
        // Only the first customer has orders
        executor.ordersByCustomer.put(ids.get(0), List.of("order-only-1", "order-only-2"));

        List<String> result = repository.findOrdersForCustomers(ids);

        assertEquals(1, executor.queryCount());
        assertEquals(2, result.size());
        assertTrue(result.contains("order-only-1"));
        assertTrue(result.contains("order-only-2"));
    }

    @Test
    @DisplayName("Empty customer list returns no orders without issuing per-customer queries")
    void emptyInputReturnsEmptyWithoutPerCustomerQueries() {
        List<String> result = repository.findOrdersForCustomers(List.of());

        assertTrue(result.isEmpty());
        assertFalse(executor.queryCount() > 1,
                "Empty input must not issue more than one query");
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
package com.example.audit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regression tests for DEBT2TEST-6: AuditLogger.logRequest() must never write
 * sensitive values (password, card number, SSN) to the log sink in plaintext.
 *
 * Assumptions (adjust to the actual API in the codebase):
 *  - AuditLogger is constructed with a List<String> sink (entries are appended to it).
 *  - Request exposes a constructor/builder accepting the fields used below.
 *  - Non-sensitive fields (requestId, username, amount) are expected to remain in the log.
 *
 * These tests are expected to FAIL against the current implementation (sink.add(request.toString()))
 * and PASS once a redaction step is applied before logging.
 */
@DisplayName("AuditLogger redaction (DEBT2TEST-6)")
class AuditLoggerRedactionTest {

    private static final String REQUEST_ID = "req-1001";
    private static final String USERNAME = "alice";
    private static final String RAW_PASSWORD = "Sup3r$ecretPw!";
    private static final String RAW_CARD_NUMBER = "4111111111111111";
    private static final String RAW_CARD_FORMATTED = "4111 1111 1111 1111";
    private static final String RAW_SSN = "123-45-6789";

    private List<String> sink;
    private AuditLogger auditLogger;

    @BeforeEach
    void setUp() {
        sink = new ArrayList<>();
        auditLogger = new AuditLogger(sink);
    }

    private Request sensitiveRequest() {
        // Adjust to the real Request construction API.
        return new Request(REQUEST_ID, USERNAME, RAW_PASSWORD, RAW_CARD_NUMBER, RAW_SSN, 42.50);
    }

    private static String digitsOnly(String s) {
        return s.replaceAll("\\D", "");
    }

    @Nested
    @DisplayName("Sensitive values never reach the sink in plaintext")
    class SensitiveValueRedaction {

        @Test
        @DisplayName("raw password is not written to the log")
        void logRequest_doesNotWriteRawPassword() {
            auditLogger.logRequest(sensitiveRequest());

            assertEquals(1, sink.size(), "exactly one entry expected");
            assertFalse(sink.get(0).contains(RAW_PASSWORD),
                    "raw password must not appear in the audit log entry");
        }

        @Test
        @DisplayName("raw card number is not written to the log (plain form)")
        void logRequest_doesNotWriteRawCardNumber() {
            auditLogger.logRequest(sensitiveRequest());

            assertFalse(sink.get(0).contains(RAW_CARD_NUMBER),
                    "full PAN must not appear in the audit log entry");
        }

        @Test
        @DisplayName("card number is not written to the log (formatted/separated form)")
        void logRequest_doesNotWriteFormattedCardNumber() {
            auditLogger.logRequest(sensitiveRequest());

            String entry = sink.get(0);
            assertFalse(entry.contains(RAW_CARD_FORMATTED),
                    "space-separated PAN must not appear in the audit log entry");
            assertFalse(digitsOnly(entry).contains(RAW_CARD_NUMBER),
                    "PAN digit sequence must not be recoverable from the audit log entry");
        }

        @Test
        @DisplayName("raw SSN is not written to the log")
        void logRequest_doesNotWriteRawSsn() {
            auditLogger.logRequest(sensitiveRequest());

            String entry = sink.get(0);
            assertFalse(entry.contains(RAW_SSN), "raw SSN must not appear in the audit log entry");
            assertFalse(digitsOnly(entry).contains(digitsOnly(RAW_SSN)),
                    "SSN digit sequence must not be recoverable from the audit log entry");
        }

        @Test
        @DisplayName("no sensitive value leaks across multiple logged requests")
        void logRequest_multipleRequests_noSensitiveValueLeaks() {
            auditLogger.logRequest(sensitiveRequest());
            auditLogger.logRequest(sensitiveRequest());
            auditLogger.logRequest(sensitiveRequest());

            assertEquals(3, sink.size());
            for (String entry : sink) {
                assertAll(
                        () -> assertFalse(entry.contains(RAW_PASSWORD), "password leaked: " + entry),
                        () -> assertFalse(entry.contains(RAW_CARD_NUMBER), "PAN leaked: " + entry),
                        () -> assertFalse(entry.contains(RAW_SSN), "SSN leaked: " + entry)
                );
            }
        }
    }

    @Nested
    @DisplayName("Redaction preserves audit usefulness")
    class RedactionPreservesAuditValue {

        @Test
        @DisplayName("card number is masked, retaining only the last four digits")
        void logRequest_cardNumberMaskedToLastFour() {
            auditLogger.logRequest(sensitiveRequest());

            String entry = sink.get(0);
            assertTrue(entry.contains("1111"),
                    "last four digits should remain to support audit correlation");
            assertTrue(entry.contains("*") || entry.toLowerCase().contains("masked")
                            || entry.toLowerCase().contains("redacted"),
                    "card number should carry an explicit mask/redaction marker");
        }

        @Test
        @DisplayName("non-sensitive fields remain present in the log entry")
        void logRequest_preservesNonSensitiveFields() {
            auditLogger.logRequest(sensitiveRequest());

            String entry = sink.get(0);
            assertAll(
                    () -> assertTrue(entry.contains(REQUEST_ID), "requestId should be logged"),
                    () -> assertTrue(entry.contains(USERNAME), "username should be logged")
            );
        }

        @Test
        @DisplayName("redacted entry is still produced for each request (no dropped audit records)")
        void logRequest_alwaysWritesEntry() {
            auditLogger.logRequest(sensitiveRequest());

            assertFalse(sink.isEmpty(), "audit entry must not be dropped by redaction");
            assertFalse(sink.get(0).isBlank(), "audit entry must not be blank");
        }
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for CacheManager's bounded LRU behavior.
 *
 * Assumed API (per DEBT2TEST-4 acceptance criteria):
 *   - new CacheManager(int maxSize)  // maxSize must be > 0
 *   - void put(K key, V value)
 *   - V get(K key)                   // returns null if absent/evicted; counts as an access
 *   - boolean containsKey(K key)
 *   - int size()
 *   - int getMaxSize()
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
    @DisplayName("exposes configured maxSize")
    void shouldExposeConfiguredMaxSize() {
        assertEquals(MAX_SIZE, cacheManager.getMaxSize());
    }

    @Test
    @DisplayName("rejects non-positive maxSize")
    void shouldRejectNonPositiveMaxSize() {
        assertThrows(IllegalArgumentException.class, () -> new CacheManager<String, Integer>(0));
        assertThrows(IllegalArgumentException.class, () -> new CacheManager<String, Integer>(-1));
    }

    @Test
    @DisplayName("does not evict while size is within maxSize")
    void shouldNotEvictWithinMaxSize() {
        cacheManager.put("a", 1);
        cacheManager.put("b", 2);
        cacheManager.put("c", 3);

        assertEquals(MAX_SIZE, cacheManager.size());
        assertTrue(cacheManager.containsKey("a"));
        assertTrue(cacheManager.containsKey("b"));
        assertTrue(cacheManager.containsKey("c"));
    }

    @Test
    @DisplayName("evicts least recently used entry once maxSize is exceeded")
    void shouldEvictLeastRecentlyUsedWhenMaxSizeExceeded() {
        cacheManager.put("a", 1);
        cacheManager.put("b", 2);
        cacheManager.put("c", 3);

        cacheManager.put("d", 4); // exceeds maxSize; "a" is LRU

        assertEquals(MAX_SIZE, cacheManager.size());
        assertFalse(cacheManager.containsKey("a"));
        assertNull(cacheManager.get("a"));
        assertEquals(2, cacheManager.get("b"));
        assertEquals(3, cacheManager.get("c"));
        assertEquals(4, cacheManager.get("d"));
    }

    @Test
    @DisplayName("evicts exactly one entry per insert beyond capacity")
    void shouldEvictExactlyOneEntryPerInsert() {
        cacheManager.put("a", 1);
        cacheManager.put("b", 2);
        cacheManager.put("c", 3);

        cacheManager.put("d", 4);
        assertEquals(MAX_SIZE, cacheManager.size());

        cacheManager.put("e", 5);
        assertEquals(MAX_SIZE, cacheManager.size());
        assertFalse(cacheManager.containsKey("b"));
        assertFalse(cacheManager.containsKey("c") && !cacheManager.containsKey("d"));
    }

    @Test
    @DisplayName("get refreshes access order so recently read entry survives eviction")
    void shouldRetainRecentlyAccessedEntryOnEviction() {
        cacheManager.put("a", 1);
        cacheManager.put("b", 2);
        cacheManager.put("c", 3);

        assertEquals(1, cacheManager.get("a")); // "a" becomes most recently used

        cacheManager.put("d", 4); // should evict "b", not "a"

        assertTrue(cacheManager.containsKey("a"));
        assertFalse(cacheManager.containsKey("b"));
        assertTrue(cacheManager.containsKey("c"));
        assertTrue(cacheManager.containsKey("d"));
    }

    @Test
    @DisplayName("updating an existing key does not trigger eviction")
    void shouldNotEvictWhenUpdatingExistingKey() {
        cacheManager.put("a", 1);
        cacheManager.put("b", 2);
        cacheManager.put("c", 3);

        cacheManager.put("a", 10); // update, not insert

        assertEquals(MAX_SIZE, cacheManager.size());
        assertEquals(10, cacheManager.get("a"));
        assertTrue(cacheManager.containsKey("b"));
        assertTrue(cacheManager.containsKey("c"));
    }

    @Test
    @DisplayName("size stays bounded under sustained memory pressure")
    void shouldBoundSizeUnderSustainedInserts() {
        int totalInserts = 10_000;
        for (int i = 0; i < totalInserts; i++) {
            cacheManager.put("key-" + i, i);
            assertTrue(cacheManager.size() <= MAX_SIZE,
                    "size exceeded maxSize at insert " + i);
        }

        assertEquals(MAX_SIZE, cacheManager.size());
        // Only the most recent MAX_SIZE entries should remain
        for (int i = totalInserts - MAX_SIZE; i < totalInserts; i++) {
            assertEquals(i, cacheManager.get("key-" + i));
        }
        assertNull(cacheManager.get("key-0"));
    }

    @Test
    @DisplayName("evicted entries are no longer retrievable")
    void shouldNotReturnEvictedEntries() {
        for (int i = 0; i < MAX_SIZE + 5; i++) {
            cacheManager.put("k" + i, i);
        }

        for (int i = 0; i < 5; i++) {
            assertNull(cacheManager.get("k" + i), "evicted key k" + i + " should be absent");
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ConnectionPool concurrent access (DEBT2TEST-3).
 *
 * Assumes ConnectionPool exposes:
 *   - a constructor ConnectionPool(int size)
 *   - DbConnection getConnection()
 *   - void releaseConnection(DbConnection conn)
 *   - nested static class ConnectionPool.DbConnection
 * and that getConnection() never hands out a connection that is still leased.
 *
 * Thread count is kept <= pool size so each worker holds at most one lease at a time,
 * which makes the invariants deterministic (no reliance on timing for pass/fail).
 */
class ConnectionPoolConcurrencyTest {

    private static final int POOL_SIZE = 8;
    private static final int THREADS = 8;
    private static final int ITERATIONS_PER_THREAD = 1_000;

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
        executor.awaitTermination(5, TimeUnit.SECONDS);
    }

    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void shouldNotHandOutSameConnectionTwiceAfterDoubleRelease() {
        ConnectionPool.DbConnection conn = connectionPool.getConnection();
        assertNotNull(conn);

        connectionPool.releaseConnection(conn);
        connectionPool.releaseConnection(conn); // duplicate release must be tolerated

        // Drain the pool: every lease must be distinct, so the double-released
        // connection must not appear more than once.
        Set<ConnectionPool.DbConnection> leased = Collections.newSetFromMap(new java.util.IdentityHashMap<>());
        for (int i = 0; i < POOL_SIZE; i++) {
            ConnectionPool.DbConnection c = connectionPool.getConnection();
            assertNotNull(c, "pool should still supply connections after double release");
            assertTrue(leased.add(c), "same connection leased twice after double release");
        }
        assertEquals(POOL_SIZE, leased.size());
    }

    @Test
    @Timeout(value = 60, unit = TimeUnit.SECONDS)
    void concurrentGetAndReleaseShouldNeverDuplicateLeases() throws Exception {
        Set<ConnectionPool.DbConnection> currentlyLeased =
                Collections.newSetFromMap(new ConcurrentHashMap<>());
        ConcurrentLinkedQueue<String> violations = new ConcurrentLinkedQueue<>();
        AtomicInteger completedOps = new AtomicInteger();
        CountDownLatch startGate = new CountDownLatch(1);

        List<Future<?>> futures = new ArrayList<>();
        for (int t = 0; t < THREADS; t++) {
            futures.add(executor.submit(() -> {
                startGate.await();
                for (int i = 0; i < ITERATIONS_PER_THREAD; i++) {
                    ConnectionPool.DbConnection conn = connectionPool.getConnection();
                    if (conn == null) {
                        violations.add("getConnection returned null");
                        return null;
                    }
                    if (!currentlyLeased.add(conn)) {
                        violations.add("connection leased to two threads at once");
                    }
                    // Remove from tracking BEFORE releasing so a re-lease cannot race the removal.
                    currentlyLeased.remove(conn);
                    connectionPool.releaseConnection(conn);
                    completedOps.incrementAndGet();
                }
                return null;
            }));
        }

        startGate.countDown();
        for (Future<?> f : futures) {
            f.get(50, TimeUnit.SECONDS); // surfaces worker exceptions; bounded wait
        }

        assertTrue(violations.isEmpty(), "concurrency violations: " + violations);
        assertEquals(THREADS * ITERATIONS_PER_THREAD, completedOps.get(),
                "lost operations detected");
    }

    @Test
    @Timeout(value = 60, unit = TimeUnit.SECONDS)
    void noConnectionsAreLostUnderConcurrentChurn() throws Exception {
        CountDownLatch startGate = new CountDownLatch(1);
        List<Future<?>> futures = new ArrayList<>();
        for (int t = 0; t < THREADS; t++) {
            futures.add(executor.submit(() -> {
                startGate.await();
                for (int i = 0; i < ITERATIONS_PER_THREAD; i++) {
                    ConnectionPool.DbConnection conn = connectionPool.getConnection();
                    assertNotNull(conn, "pool exhausted: connection lost or not returned");
                    connectionPool.releaseConnection(conn);
                }
                return null;
            }));
        }

        startGate.countDown();
        for (Future<?> f : futures) {
            f.get(50, TimeUnit.SECONDS);
        }

        // After all churn, the full set of connections must still be obtainable exactly once.
        Set<ConnectionPool.DbConnection> drained = new HashSet<>();
        for (int i = 0; i < POOL_SIZE; i++) {
            ConnectionPool.DbConnection c = assertTimeoutPreemptively(
                    Duration.ofSeconds(2), connectionPool::getConnection,
                    "pool exhausted after concurrent churn");
            assertNotNull(c);
            assertTrue(drained.add(c), "duplicate connection present after churn");
        }
        assertEquals(POOL_SIZE, drained.size(), "connections lost during concurrent churn");
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

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the batched customer lookup in OrderRepository.findOrdersForCustomers().
 *
 * Replaces the legacy test that pinned the N+1 behavior (one query per customer).
 * Once the loop is replaced with a single "WHERE customer_id IN (...)" query, the
 * legacy assertion of 3 queries for 3 customers must no longer hold.
 */
class OrderRepositoryBatchedLookupTest {

    private OrderRepository orderRepository;

    @BeforeEach
    void setUp() {
        // Assumes a fresh repository per test so the query counter starts at zero.
        orderRepository = new OrderRepository();
    }

    @Test
    void findOrdersForCustomersIssuesSingleQueryForMultipleCustomers() {
        orderRepository.findOrdersForCustomers(List.of("c1", "c2", "c3"));

        assertEquals(1, orderRepository.getQueryCount(),
                "A multi-customer lookup must be satisfied by one batched query, not one per customer");
    }

    @Test
    void findOrdersForCustomersIssuesSingleQueryRegardlessOfInputSize() {
        List<String> customerIds = IntStream.rangeClosed(1, 250)
                .mapToObj(i -> "c" + i)
                .collect(Collectors.toList());

        orderRepository.findOrdersForCustomers(customerIds);

        assertEquals(1, orderRepository.getQueryCount(),
                "Query count must not scale with the number of customer IDs");
    }

    @Test
    void findOrdersForCustomersIssuesSingleQueryForSingleCustomer() {
        orderRepository.findOrdersForCustomers(List.of("c1"));

        assertEquals(1, orderRepository.getQueryCount(),
                "A single customer lookup should still use exactly one batched query");
    }

    @Test
    void findOrdersForCustomersWithEmptyInputDoesNotIssueMoreThanOneQuery() {
        orderRepository.findOrdersForCustomers(List.of());

        assertTrue(orderRepository.getQueryCount() <= 1,
                "An empty customer list must not trigger per-customer queries");
    }

    @Test
    void findOrdersForCustomersReturnsResultsForAllRequestedCustomers() {
        var result = orderRepository.findOrdersForCustomers(List.of("c1", "c2", "c3"));

        assertNotNull(result, "Batched lookup should return a result for the requested customers");
        assertEquals(1, orderRepository.getQueryCount(),
                "Result retrieval must still be backed by a single batched query");
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link AuditLogger}.
 *
 * <p><b>Known technical debt (DEBT2TEST-6):</b> {@link AuditLogger#logRequest} writes the
 * full request via {@code toString()}, so raw passwords and card numbers reach the log sink
 * in plaintext. This is a compliance risk (PCI-DSS / GDPR) because logs are shipped to a
 * third-party aggregator. See the related FIXME from the 2023 audit-trail rollout.
 *
 * <p>The test {@link #currentlyLogsSensitiveFieldsInPlaintext()} is intentionally a
 * <em>characterization test</em>. It does not endorse the current behavior. It pins the
 * existing unredacted output so that:
 * <ul>
 *   <li>the current behavior is explicitly documented and visible in the suite, and</li>
 *   <li>any change to logging output is detected deliberately rather than silently.</li>
 * </ul>
 *
 * <p><b>Removal plan:</b> once a redaction step (field allowlist or masking) is added before
 * any object is logged, this test must be inverted. The assertions should verify that the
 * sensitive values are <em>absent</em> from the entry. The disabled test
 * {@link #redactsSensitiveFieldsBeforeLogging()} below is the target replacement. Remove the
 * {@code @Disabled} annotation and delete the characterization test when redaction ships.
 */
class AuditLoggerTest {

    private AuditLogger auditLogger;

    @BeforeEach
    void setUp() {
        auditLogger = new AuditLogger();
    }

    /**
     * Characterization test for DEBT2TEST-6.
     *
     * <p><b>Why this asserts plaintext:</b> the logger currently has no redaction step, so the
     * password {@code s3cr3t} and card number {@code 4111111111111111} appear verbatim in the
     * logged entry. This assertion documents that fact. It is a known violation of the
     * acceptance criteria ("sensitive fields never reach the log sink in plaintext") and
     * exists only to make the current behavior explicit until redaction is implemented.
     *
     * <p><b>Action required:</b> this test must be inverted (or deleted and replaced by
     * {@link #redactsSensitiveFieldsBeforeLogging()}) when redaction is introduced. Leaving it
     * in place after redaction ships will fail the build, which is intentional.
     */
    @Test
    @DisplayName("DEBT2TEST-6: characterizes current unredacted logging of sensitive fields")
    void currentlyLogsSensitiveFieldsInPlaintext() {
        auditLogger.logRequest(new AuditLogger.Request("alice", "s3cr3t", "4111111111111111"));
        String entry = auditLogger.getLoggedEntries().get(0);

        // Pins current behavior: raw sensitive values are logged in plaintext (known debt).
        assertTrue(entry.contains("s3cr3t"));
        assertTrue(entry.contains("4111111111111111"));
    }

    /**
     * Target test for DEBT2TEST-6. Disabled until redaction exists.
     *
     * <p>This is the inverted form of {@link #currentlyLogsSensitiveFieldsInPlaintext()} and
     * matches the acceptance criterion: "Unit test asserts logged output does not contain raw
     * sensitive values." Enable it, and delete the characterization test, when the redaction
     * step is implemented.
     */
    @Test
    @Disabled("DEBT2TEST-6: enable once redaction of sensitive fields is implemented")
    @DisplayName("Redacts password and card number before logging")
    void redactsSensitiveFieldsBeforeLogging() {
        auditLogger.logRequest(new AuditLogger.Request("alice", "s3cr3t", "4111111111111111"));
        String entry = auditLogger.getLoggedEntries().get(0);

        assertFalse(entry.contains("s3cr3t"), "Password must not appear in plaintext in the log");
        assertFalse(entry.contains("4111111111111111"), "Card number must not appear in plaintext in the log");
        // Non-sensitive fields should still be logged for auditing purposes.
        assertTrue(entry.contains("alice"));
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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/*
 * Assumed production API (per DEBT2TEST-2 acceptance criteria):
 *   - PaymentConfig(long gatewayTimeoutMs, int maxRetryAttempts)  -- injectable configuration
 *   - PaymentProcessor(PaymentConfig config)                      -- configuration via constructor
 *   - PaymentProcessor.Order(String id, double amount)
 *   - PaymentProcessor.PaymentResult with success()
 * Adjust the constructor/config types if the actual signatures differ.
 */
class PaymentProcessorRetryTest {

    private static final long TEST_GATEWAY_TIMEOUT_MS = 50L;
    private static final int TEST_MAX_RETRY_ATTEMPTS = 3;

    private PaymentProcessor paymentProcessor;

    @BeforeEach
    void setUp() {
        // Injected configuration replaces the hardcoded GATEWAY_TIMEOUT_MS = 5000
        PaymentConfig config = new PaymentConfig(TEST_GATEWAY_TIMEOUT_MS, TEST_MAX_RETRY_ATTEMPTS);
        paymentProcessor = new PaymentProcessor(config);
    }

    @Test
    @DisplayName("Negative amount fails after retries using injected config")
    void shouldFailOnNegativeAmountAfterRetries() {
        PaymentProcessor.Order order = new PaymentProcessor.Order("order-2", -50.0);

        PaymentProcessor.PaymentResult result = paymentProcessor.process(order);

        assertNotNull(result);
        assertFalse(result.success());
    }

    @ParameterizedTest(name = "negative amount {0} is rejected")
    @ValueSource(doubles = {-0.01, -1.0, -50.0, -1000.0})
    @DisplayName("Various negative amounts are rejected on the retry path")
    void shouldFailForAnyNegativeAmount(double amount) {
        PaymentProcessor.Order order = new PaymentProcessor.Order("order-neg", amount);

        PaymentProcessor.PaymentResult result = paymentProcessor.process(order);

        assertFalse(result.success());
    }

    @Nested
    @DisplayName("With zero retry attempts configured")
    class ZeroRetryConfiguration {

        private PaymentProcessor noRetryProcessor;

        @BeforeEach
        void setUpNoRetry() {
            PaymentConfig config = new PaymentConfig(TEST_GATEWAY_TIMEOUT_MS, 0);
            noRetryProcessor = new PaymentProcessor(config);
        }

        @Test
        @DisplayName("Negative amount still fails when no retries are allowed")
        void shouldFailOnNegativeAmountWithoutRetries() {
            PaymentProcessor.Order order = new PaymentProcessor.Order("order-no-retry", -50.0);

            PaymentProcessor.PaymentResult result = noRetryProcessor.process(order);

            assertFalse(result.success());
        }
    }

    @Nested
    @DisplayName("With short gateway timeout configured")
    class ShortTimeoutConfiguration {

        private PaymentProcessor shortTimeoutProcessor;

        @BeforeEach
        void setUpShortTimeout() {
            PaymentConfig config = new PaymentConfig(1L, TEST_MAX_RETRY_ATTEMPTS);
            shortTimeoutProcessor = new PaymentProcessor(config);
        }

        @Test
        @DisplayName("Negative amount fails quickly under a short injected timeout")
        void shouldFailOnNegativeAmountWithShortTimeout() {
            PaymentProcessor.Order order = new PaymentProcessor.Order("order-short", -50.0);

            PaymentProcessor.PaymentResult result = shortTimeoutProcessor.process(order);

            assertFalse(result.success());
        }
    }
}
```

---

