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

/**
 * Boots the whole API context without a database (see {@link NoDatabaseTestSupport}) and checks
 * the Swagger document is served. The database wiring itself is covered by
 * {@link DebtApiDatabaseIntegrationTest}.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@NoDatabaseTestSupport
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

    @Test
    void swaggerDocumentsAllRunStatusesAndTheTestFrameworkDefault() {
        String body = restTemplate.getForEntity("/v3/api-docs", String.class).getBody();

        assertTrue(body.contains("\"QUEUED\""));
        assertTrue(body.contains("\"COMPLETED_WITH_ERRORS\""));
        assertTrue(body.contains("JUnit 5"));
        assertTrue(body.contains("\"NOT_FOUND\""));
        assertTrue(body.contains("\"GENERATION_FAILED\""));
    }
}
