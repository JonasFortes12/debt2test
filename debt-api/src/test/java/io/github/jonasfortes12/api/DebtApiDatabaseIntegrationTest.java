package io.github.jonasfortes12.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import io.github.jonasfortes12.persistence.access.AccessCodeStore;

/** Boots the real API against a real PostgreSQL: the only test that proves the wiring end to end. */
@Tag("integration")
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
class DebtApiDatabaseIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("DEBT_DB_URL", POSTGRES::getJdbcUrl);
        registry.add("DEBT_DB_USERNAME", POSTGRES::getUsername);
        registry.add("DEBT_DB_PASSWORD", POSTGRES::getPassword);
    }

    @Autowired
    private AccessCodeStore accessCodeStore;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void contextBootsAgainstRealDatabaseAndMigratesAccessTables() {
        assertNotNull(accessCodeStore);
        assertTrue(accessCodeStore.findByHash("0".repeat(64)).isEmpty(), "access tables exist and are queryable");
        assertEquals(HttpStatus.OK, restTemplate.getForEntity("/v3/api-docs", String.class).getStatusCode());
    }
}
