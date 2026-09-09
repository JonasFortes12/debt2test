package io.github.jonasfortes12.persistence.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import io.github.jonasfortes12.core.model.ItemStatus;
import io.github.jonasfortes12.core.model.RunStatus;
import io.github.jonasfortes12.persistence.entity.PipelineRunEntity;
import io.github.jonasfortes12.persistence.entity.TechnicalDebtEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Tag("integration")
@Testcontainers
@DataJpaTest
@ImportAutoConfiguration(FlywayAutoConfiguration.class)
@ActiveProfiles("test")
class SchemaIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired private PipelineRunRepository runs;
    @Autowired private TechnicalDebtRepository debts;

    @PersistenceContext private EntityManager entityManager;

    private PipelineRunEntity newRun() {
        PipelineRunEntity run = new PipelineRunEntity();
        run.setId(UUID.randomUUID());
        run.setRepositoryUrl("https://example.com/repo.git");
        run.setStatus(RunStatus.RUNNING);
        run.setStartedAt(Instant.now());
        run.setContextEnabled(true);
        run.setHeuristicFallbackAllowed(true);
        return run;
    }

    private TechnicalDebtEntity newDebt(PipelineRunEntity run, String candidateId) {
        TechnicalDebtEntity debt = new TechnicalDebtEntity();
        debt.setRun(run);
        debt.setCandidateId(candidateId);
        debt.setFilePath("src/main/java/Example.java");
        debt.setMethodName("doWork");
        debt.setLineNumber(12);
        debt.setComment("// TODO");
        debt.setMethodSourceCode("void doWork() { }");
        debt.setSourceRepositoryUrl("https://example.com/repo.git");
        debt.setSourceRelativeFilePath("src/main/java/Example.java");
        debt.setExtractedAt(Instant.now());
        return debt;
    }

    @Test
    void hibernateValidatesAgainstTheFlywaySchema() {
        assertTrue(runs.count() >= 0);
    }

    @Test
    void runIsFoundByItsDomainRunId() {
        PipelineRunEntity run = newRun();
        run.setRunId("run-42");
        runs.saveAndFlush(run);

        assertTrue(runs.findFirstByRunIdOrderByStartedAtDesc("run-42").isPresent());
    }

    @Test
    void repeatRunsOfTheSameConfigurationShareARunIdAcrossRows() {
        PipelineRunEntity first = newRun();
        first.setRunId("run-42");
        runs.saveAndFlush(first);

        PipelineRunEntity second = newRun();
        second.setRunId("run-42");
        runs.saveAndFlush(second);

        assertEquals(2, runs.findAll().stream().filter(r -> "run-42".equals(r.getRunId())).count());
    }

    @Test
    void duplicateCandidateWithinOneRunIsRejected() {
        PipelineRunEntity run = runs.saveAndFlush(newRun());
        debts.saveAndFlush(newDebt(run, "candidate-1"));

        assertThrows(DataIntegrityViolationException.class,
                () -> debts.saveAndFlush(newDebt(run, "candidate-1")));
    }

    @Test
    void sameCandidateIdInDifferentRunsIsAllowed() {
        PipelineRunEntity first = runs.saveAndFlush(newRun());
        PipelineRunEntity second = runs.saveAndFlush(newRun());

        debts.saveAndFlush(newDebt(first, "candidate-1"));
        debts.saveAndFlush(newDebt(second, "candidate-1"));

        assertEquals(2, debts.count());
    }

    @Test
    void confidenceOutsideZeroToOneIsRejected() {
        PipelineRunEntity run = runs.saveAndFlush(newRun());
        TechnicalDebtEntity debt = newDebt(run, "candidate-2");
        debt.setSatd(true);
        debt.setDebtType("DESIGN");
        debt.setConfidence(1.5);
        debt.setItemStatus(ItemStatus.CLASSIFIED);

        assertThrows(DataIntegrityViolationException.class, () -> debts.saveAndFlush(debt));
    }

    @Test
    void deletingARunCascadesToItsDebts() {
        PipelineRunEntity run = runs.saveAndFlush(newRun());
        debts.saveAndFlush(newDebt(run, "candidate-1"));

        entityManager.clear();
        runs.deleteById(run.getId());
        runs.flush();
        entityManager.clear();

        assertEquals(List.of(), debts.findAll());
    }
}
