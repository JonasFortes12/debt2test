package io.github.jonasfortes12.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;

/**
 * DEBT_PERSISTENCE_ENABLED=true with no reachable database mirrors how PersistenceContextTest
 * exercises the CLI's own "enabled but unavailable" path: the child persistence context fails to
 * connect, openIfEnabled catches that and returns an unavailable-but-present PersistenceContext,
 * and this outer context boots normally with no real database and no Docker involved.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@TestPropertySource(properties = {
        "DEBT_PERSISTENCE_ENABLED=true",
        "DEBT_DB_URL=jdbc:postgresql://localhost:1/unreachable-test-db",
        "DEBT_DB_USERNAME=test",
        "DEBT_DB_PASSWORD=test"
})
class DebtApiApplicationSmokeTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void contextLoadsAndSwaggerDocumentsAllFourRoutes() {
        ResponseEntity<String> response = restTemplate.getForEntity("/v3/api-docs", String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        String body = response.getBody();
        assertTrue(body.contains("/api/pipeline/runs"));
        assertTrue(body.contains("/api/pipeline/runs/{executionId}"));
        assertTrue(body.contains("/api/pipeline/runs/{executionId}/report"));
        assertTrue(body.contains("/api/pipeline/runs/{executionId}/cancel"));
    }

    @Test
    void swaggerDocumentsRequestExamples() {
        String body = restTemplate.getForEntity("/v3/api-docs", String.class).getBody();

        assertTrue(body.contains("https://github.com/JonasFortes12/mock-debt-project"));
        assertTrue(body.contains("3f2b8c1e-9a4d-4e7b-8f6a-2c1d5e9b7a40"));
    }
}
