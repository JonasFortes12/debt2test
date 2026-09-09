package io.github.jonasfortes12.persistence.adapter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import io.github.jonasfortes12.core.model.ClassificationOptions;
import io.github.jonasfortes12.core.model.ClassifiedDebt;
import io.github.jonasfortes12.core.model.ContextRequest;
import io.github.jonasfortes12.core.model.ContextStatus;
import io.github.jonasfortes12.core.model.EnrichedSatdDebt;
import io.github.jonasfortes12.core.model.ExternalReference;
import io.github.jonasfortes12.core.model.ExternalTaskSpec;
import io.github.jonasfortes12.core.model.ExtractionOptions;
import io.github.jonasfortes12.core.model.GeneratedTest;
import io.github.jonasfortes12.core.model.ItemStatus;
import io.github.jonasfortes12.core.model.PipelineError;
import io.github.jonasfortes12.core.model.PipelineItemResult;
import io.github.jonasfortes12.core.model.PipelineRequest;
import io.github.jonasfortes12.core.model.PipelineResult;
import io.github.jonasfortes12.core.model.Provenance;
import io.github.jonasfortes12.core.model.ReportOptions;
import io.github.jonasfortes12.core.model.RepositoryRequest;
import io.github.jonasfortes12.core.model.RepositoryWorkspace;
import io.github.jonasfortes12.core.model.RunStatus;
import io.github.jonasfortes12.core.model.SatdCandidate;
import io.github.jonasfortes12.core.model.SourceProvenance;
import io.github.jonasfortes12.core.model.TestGenerationOptions;
import io.github.jonasfortes12.core.model.ValidationStatus;
import io.github.jonasfortes12.persistence.entity.PipelineErrorEntity;
import io.github.jonasfortes12.persistence.entity.PipelineRunEntity;
import io.github.jonasfortes12.persistence.entity.TechnicalDebtEntity;
import io.github.jonasfortes12.persistence.mapper.DomainMapper;
import io.github.jonasfortes12.persistence.repository.DebtContextRepository;
import io.github.jonasfortes12.persistence.repository.GeneratedTestRepository;
import io.github.jonasfortes12.persistence.repository.PipelineErrorRepository;
import io.github.jonasfortes12.persistence.repository.PipelineRunRepository;
import io.github.jonasfortes12.persistence.repository.TechnicalDebtRepository;

@Tag("integration")
@Testcontainers
@DataJpaTest
@ImportAutoConfiguration(FlywayAutoConfiguration.class)
@Import(DatabasePipelineRunStore.class)
@ActiveProfiles("test")
class RunStoreRoundTripIntegrationTest {

    private static final String REPO = "https://example.com/repo.git";

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired private DatabasePipelineRunStore store;
    @Autowired private PipelineRunRepository runs;
    @Autowired private TechnicalDebtRepository debts;
    @Autowired private DebtContextRepository contexts;
    @Autowired private GeneratedTestRepository tests;
    @Autowired private PipelineErrorRepository errors;

    // --- fixtures ---------------------------------------------------------

    private static PipelineRequest request() {
        return new PipelineRequest(
                "run-1",
                new RepositoryRequest(REPO, "main"),
                new ExtractionOptions("run-1"),
                new ClassificationOptions(true),
                new ContextRequest(true),
                new TestGenerationOptions("junit5", "v1"),
                new ReportOptions(Path.of("output")));
    }

    private static RepositoryWorkspace workspace() {
        return new RepositoryWorkspace(Path.of("/tmp/ws"), REPO, "abc123");
    }

    private static SatdCandidate candidate(String id, int line) {
        return new SatdCandidate(
                id,
                "src/main/java/Example.java",
                "doWork",
                line,
                "// TODO fix this",
                "void doWork() { }",
                new SourceProvenance(REPO, "abc123", "src/main/java/Example.java"));
    }

    private static SatdCandidate matchedCandidate() { return candidate("candidate-matched", 12); }
    private static SatdCandidate unmatchedCandidate() { return candidate("candidate-unmatched", 34); }
    private static SatdCandidate notSatdCandidate() { return candidate("candidate-not-satd", 56); }

    private static ClassifiedDebt satd(SatdCandidate candidate) {
        return new ClassifiedDebt(candidate, true, "DESIGN", 0.91,
                new Provenance("weka", "debthunter", "1.0"), ItemStatus.CLASSIFIED, List.of());
    }

    private static ClassifiedDebt matchedClassified() { return satd(matchedCandidate()); }
    private static ClassifiedDebt unmatchedClassified() { return satd(unmatchedCandidate()); }

    private static ClassifiedDebt notSatdClassified() {
        return new ClassifiedDebt(notSatdCandidate(), false, "NON_SATD", 0.12,
                new Provenance("weka", "debthunter", "1.0"), ItemStatus.NOT_SATD, List.of());
    }

    private static EnrichedSatdDebt matchedEnriched() {
        return new EnrichedSatdDebt(
                matchedClassified(),
                new ExternalTaskSpec("jira", "DUBBO-1234", "Fix it", "Long description",
                        List.of("criterion one"), List.of("bug"), "https://jira/DUBBO-1234"),
                List.of(new ExternalReference("DUBBO-1234", "comment")),
                ContextStatus.MATCHED,
                List.of());
    }

    private static EnrichedSatdDebt unmatchedEnriched() {
        return new EnrichedSatdDebt(
                unmatchedClassified(), null, List.of(), ContextStatus.NOT_FOUND, List.of());
    }

    private static GeneratedTest generatedTest() {
        return new GeneratedTest("candidate-matched", "class ExampleTest { }", "junit5",
                "openai", "gpt-test", "v1", ItemStatus.GENERATED, ValidationStatus.NOT_RUN, List.of());
    }

    private static GeneratedTest failedTest() {
        return new GeneratedTest("candidate-unmatched", null, "junit5", "openai", "gpt-test", "v1",
                ItemStatus.GENERATION_FAILED, ValidationStatus.NOT_RUN,
                List.of(new PipelineError("generation", "LLM_GENERATION_FAILED", "boom",
                        "candidate-unmatched", true)));
    }

    private static PipelineResult completedResult() {
        List<PipelineItemResult> items = List.of(
                new PipelineItemResult(matchedCandidate(), matchedClassified(), matchedEnriched(),
                        generatedTest(), List.of()),
                new PipelineItemResult(unmatchedCandidate(), unmatchedClassified(), unmatchedEnriched(),
                        failedTest(), List.of()),
                new PipelineItemResult(notSatdCandidate(), notSatdClassified(), null, null, List.of()));
        List<PipelineError> runErrors = List.of(
                new PipelineError("repository", "WORKSPACE_RELEASE_FAILED", "release failed", null, true),
                new PipelineError("generation", "LLM_GENERATION_FAILED", "boom", "candidate-unmatched", true));
        return new PipelineResult("run-1", RunStatus.COMPLETED_WITH_ERRORS, workspace(), items,
                runErrors, null);
    }

    // --- tests ------------------------------------------------------------

    @Test
    void aCompleteRunSurvivesARoundTrip() {
        String executionId = UUID.randomUUID().toString();

        store.runStarted(executionId, request());
        store.workspaceReady(executionId, workspace());
        store.runIdResolved(executionId, "run-1");
        store.candidatesExtracted(executionId,
                List.of(matchedCandidate(), unmatchedCandidate(), notSatdCandidate()));
        store.candidatesClassified(executionId,
                List.of(matchedClassified(), unmatchedClassified(), notSatdClassified()));
        store.contextEnriched(executionId, List.of(matchedEnriched(), unmatchedEnriched()));
        store.testsGenerated(executionId, List.of(generatedTest(), failedTest()));
        store.runFinished(executionId, completedResult());

        PipelineRunEntity run = runs.findById(UUID.fromString(executionId)).orElseThrow();
        assertEquals("run-1", run.getRunId());
        assertEquals(RunStatus.COMPLETED_WITH_ERRORS, run.getStatus());
        assertEquals("abc123", run.getResolvedRevision());
        assertEquals(3, run.getCandidateCount());
        assertEquals(2, run.getSatdCount());
        assertEquals(1, run.getGeneratedTestCount());
        assertNotNull(run.getFinishedAt());

        List<TechnicalDebtEntity> stored = debts.findByRun_Id(run.getId(), Pageable.unpaged()).getContent();
        assertEquals(3, stored.size(), "non-SATD candidates must be stored too");

        TechnicalDebtEntity matched = byCandidateId(stored, "candidate-matched");
        assertEquals(0.91, matched.getConfidence());
        assertEquals("weka", matched.getClassifierProvider());
        assertEquals(ContextStatus.MATCHED,
                contexts.findByTechnicalDebt_Id(matched.getId()).orElseThrow().getContextStatus());
        assertEquals(ItemStatus.GENERATED,
                tests.findByTechnicalDebt_Id(matched.getId()).orElseThrow().getStatus());

        TechnicalDebtEntity notSatd = byCandidateId(stored, "candidate-not-satd");
        assertFalse(notSatd.getSatd());
        assertTrue(contexts.findByTechnicalDebt_Id(notSatd.getId()).isEmpty());

        List<PipelineErrorEntity> storedErrors = errors.findByRun_Id(run.getId());
        assertEquals(2, storedErrors.size());
        assertTrue(storedErrors.stream().anyMatch(e -> e.getTechnicalDebt() != null),
                "an item-level error must be linked to its debt row");
        assertTrue(storedErrors.stream().anyMatch(e -> e.getTechnicalDebt() == null),
                "a run-level error must have no debt link");
    }

    @Test
    void theStoredGraphMapsBackToEquivalentDomainRecords() {
        String executionId = UUID.randomUUID().toString();
        store.runStarted(executionId, request());
        store.runIdResolved(executionId, "run-1");
        store.candidatesExtracted(executionId, List.of(matchedCandidate()));
        store.candidatesClassified(executionId, List.of(matchedClassified()));
        store.contextEnriched(executionId, List.of(matchedEnriched()));
        store.testsGenerated(executionId, List.of(generatedTest()));

        TechnicalDebtEntity stored = debts.findAll().get(0);

        SatdCandidate candidate = DomainMapper.toCandidate(stored);
        assertEquals(matchedCandidate().candidateId(), candidate.candidateId());
        assertEquals(matchedCandidate().lineNumber(), candidate.lineNumber());
        assertEquals(matchedCandidate().methodSourceCode(), candidate.methodSourceCode());

        ClassifiedDebt classified = DomainMapper.toClassifiedDebt(stored, candidate);
        assertEquals(0.91, classified.confidence());
        assertEquals("weka", classified.provenance().provider());
        assertEquals("debthunter", classified.provenance().strategy());
        assertEquals(ItemStatus.CLASSIFIED, classified.status());

        EnrichedSatdDebt enriched = DomainMapper.toEnrichedDebt(
                contexts.findByTechnicalDebt_Id(stored.getId()).orElseThrow(), classified);
        assertEquals(ContextStatus.MATCHED, enriched.contextStatus());
        assertEquals("DUBBO-1234", enriched.externalTask().key());
        assertEquals(List.of("criterion one"), enriched.externalTask().acceptanceCriteria());

        GeneratedTest test = DomainMapper.toGeneratedTest(
                tests.findByTechnicalDebt_Id(stored.getId()).orElseThrow(), stored.getCandidateId());
        assertEquals("gpt-test", test.model());
        assertEquals("v1", test.promptVersion());
    }

    @Test
    void replayingAStageDoesNotDuplicateRows() {
        String executionId = UUID.randomUUID().toString();
        store.runStarted(executionId, request());
        store.candidatesExtracted(executionId, List.of(matchedCandidate()));
        store.candidatesExtracted(executionId, List.of(matchedCandidate()));

        assertEquals(1, debts.count(), "the (run_id, candidate_id) key makes hooks idempotent");
    }

    @Test
    void classificationUpdatesTheExistingRowRatherThanInserting() {
        String executionId = UUID.randomUUID().toString();
        store.runStarted(executionId, request());
        store.candidatesExtracted(executionId, List.of(matchedCandidate()));
        store.candidatesClassified(executionId, List.of(matchedClassified()));

        assertEquals(1, debts.count());
        assertTrue(debts.findAll().get(0).getSatd());
    }

    private static TechnicalDebtEntity byCandidateId(List<TechnicalDebtEntity> all, String id) {
        return all.stream()
                .filter(debt -> id.equals(debt.getCandidateId()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("no stored debt for " + id));
    }
}
