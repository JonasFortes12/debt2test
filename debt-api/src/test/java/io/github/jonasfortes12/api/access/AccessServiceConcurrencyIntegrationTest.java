package io.github.jonasfortes12.api.access;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import io.github.jonasfortes12.api.access.mail.AccessCodeSender;

/** The access policy against a real PostgreSQL, where the races the unit tests cannot show happen. */
@Tag("integration")
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class AccessServiceConcurrencyIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("DEBT_DB_URL", POSTGRES::getJdbcUrl);
        registry.add("DEBT_DB_USERNAME", POSTGRES::getUsername);
        registry.add("DEBT_DB_PASSWORD", POSTGRES::getPassword);
    }

    @Autowired
    private AccessService accessService;
    @Autowired
    private JdbcTemplate jdbc;
    @MockitoBean
    private AccessCodeSender sender;

    @Test
    void parallelRequestsForOneEmailIssueOneCodeWithoutErrors() throws Exception {
        String email = "race-" + UUID.randomUUID() + "@example.com";
        int threads = 8;
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        try {
            List<Future<?>> requests = new ArrayList<>();
            for (int i = 0; i < threads; i++) {
                String ip = "198.51.100." + i;
                requests.add(pool.submit(() -> {
                    start.await();
                    accessService.requestCode("Race", email, "v1", ip);
                    return null;
                }));
            }
            start.countDown();
            for (Future<?> request : requests) {
                request.get();
            }
        } finally {
            pool.shutdownNow();
        }

        Integer issued = jdbc.queryForObject("SELECT count(*) FROM access_code c JOIN participant p "
                + "ON p.id = c.participant_id WHERE p.email = ?", Integer.class, email);
        assertEquals(1, issued, "the cooldown holds under concurrency, so one code is issued");
    }

    @Test
    void spendingRechecksTheQuotaInTheDatabase() {
        String email = "quota-" + UUID.randomUUID() + "@example.com";
        accessService.requestCode("Quota", email, "v1", "203.0.113.50");
        ArgumentCaptor<String> code = ArgumentCaptor.forClass(String.class);
        verify(sender, atLeastOnce()).sendCode(anyString(), code.capture(), any(), anyInt(), anyString());
        jdbc.update("UPDATE participant SET run_quota_override = 0 WHERE email = ?", email);

        assertThrows(AccessCodeUsedException.class,
                () -> accessService.startRun(code.getValue(), () -> UUID.randomUUID().toString(), id -> { }));
    }
}
