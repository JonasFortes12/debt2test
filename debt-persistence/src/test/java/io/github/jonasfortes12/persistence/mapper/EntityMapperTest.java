package io.github.jonasfortes12.persistence.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import io.github.jonasfortes12.core.model.ClassifiedDebt;
import io.github.jonasfortes12.core.model.ContextStatus;
import io.github.jonasfortes12.core.model.EnrichedSatdDebt;
import io.github.jonasfortes12.core.model.ExternalReference;
import io.github.jonasfortes12.core.model.ExternalTaskSpec;
import io.github.jonasfortes12.core.model.GeneratedTest;
import io.github.jonasfortes12.core.model.ItemStatus;
import io.github.jonasfortes12.core.model.PipelineError;
import io.github.jonasfortes12.core.model.Provenance;
import io.github.jonasfortes12.core.model.RunReportItem;
import io.github.jonasfortes12.core.model.RunSnapshot;
import io.github.jonasfortes12.core.model.RunStatus;
import io.github.jonasfortes12.core.model.SatdCandidate;
import io.github.jonasfortes12.core.model.SourceProvenance;
import io.github.jonasfortes12.core.model.ValidationStatus;
import io.github.jonasfortes12.persistence.entity.DebtContextEntity;
import io.github.jonasfortes12.persistence.entity.GeneratedTestEntity;
import io.github.jonasfortes12.persistence.entity.PipelineErrorEntity;
import io.github.jonasfortes12.persistence.entity.PipelineRunEntity;
import io.github.jonasfortes12.persistence.entity.TechnicalDebtEntity;

class EntityMapperTest {

    private static final String CREDENTIALED_URL = "https://user:secret@example.com/repo.git";

    private static SatdCandidate candidate() {
        return new SatdCandidate(
                "candidate-1",
                "src/main/java/Example.java",
                "doWork",
                12,
                "// TODO fix this",
                "void doWork() { }",
                new SourceProvenance(CREDENTIALED_URL, "abc123", "src/main/java/Example.java"));
    }

    private static ClassifiedDebt classified() {
        return new ClassifiedDebt(
                candidate(), true, "DESIGN", 0.91,
                new Provenance("weka", "debthunter", "1.0"), ItemStatus.CLASSIFIED, List.of());
    }

    @Test
    void candidateMapsEveryExtractionField() {
        PipelineRunEntity run = new PipelineRunEntity();

        TechnicalDebtEntity entity = EntityMapper.toEntity(candidate(), run);

        assertEquals("candidate-1", entity.getCandidateId());
        assertEquals("src/main/java/Example.java", entity.getFilePath());
        assertEquals("doWork", entity.getMethodName());
        assertEquals(12, entity.getLineNumber());
        assertEquals("// TODO fix this", entity.getComment());
        assertEquals("void doWork() { }", entity.getMethodSourceCode());
        assertEquals("abc123", entity.getSourceRevision());
        assertEquals(run, entity.getRun());
        assertNull(entity.getSatd(), "classification columns stay null until the classifier runs");
    }

    @Test
    void candidateRepositoryUrlIsSanitizedBeforeItReachesTheEntity() {
        TechnicalDebtEntity entity = EntityMapper.toEntity(candidate(), new PipelineRunEntity());

        assertFalse(entity.getSourceRepositoryUrl().contains("secret"),
                "credentials must never be persisted");
    }

    @Test
    void classificationFillsProvenanceAndConfidence() {
        TechnicalDebtEntity entity = EntityMapper.toEntity(candidate(), new PipelineRunEntity());

        EntityMapper.applyClassification(entity, classified());

        assertTrue(entity.getSatd());
        assertEquals("DESIGN", entity.getDebtType());
        assertEquals(0.91, entity.getConfidence());
        assertEquals("weka", entity.getClassifierProvider());
        assertEquals("debthunter", entity.getClassifierStrategy());
        assertEquals("1.0", entity.getClassifierVersion());
        assertEquals(ItemStatus.CLASSIFIED, entity.getItemStatus());
    }

    @Test
    void matchedContextMapsTaskAndReferences() {
        EnrichedSatdDebt enriched = new EnrichedSatdDebt(
                classified(),
                new ExternalTaskSpec("jira", "DUBBO-1234", "Fix it", "Long description",
                        List.of("criterion one"), List.of("bug"), "https://jira/DUBBO-1234"),
                List.of(new ExternalReference("DUBBO-1234", "comment")),
                ContextStatus.MATCHED,
                List.of());

        DebtContextEntity entity = EntityMapper.toContextEntity(enriched, new TechnicalDebtEntity());

        assertEquals(ContextStatus.MATCHED, entity.getContextStatus());
        assertEquals("jira", entity.getProvider());
        assertEquals("DUBBO-1234", entity.getTaskKey());
        assertEquals(List.of("criterion one"), entity.getAcceptanceCriteria());
        assertEquals(List.of("bug"), entity.getLabels());
        assertEquals(1, entity.getReferences().size());
        assertEquals("DUBBO-1234", entity.getReferences().get(0).getReferenceValue());
    }

    @Test
    void unmatchedContextLeavesTaskColumnsNull() {
        EnrichedSatdDebt enriched = new EnrichedSatdDebt(
                classified(), null, List.of(), ContextStatus.NOT_FOUND, List.of());

        DebtContextEntity entity = EntityMapper.toContextEntity(enriched, new TechnicalDebtEntity());

        assertEquals(ContextStatus.NOT_FOUND, entity.getContextStatus());
        assertNull(entity.getProvider());
        assertNull(entity.getTaskKey());
        assertTrue(entity.getReferences().isEmpty());
    }

    @Test
    void failedGenerationMapsWithNullSourceCode() {
        GeneratedTest failed = new GeneratedTest(
                "candidate-1", null, "junit5", "openai", "gpt-test", "v1",
                ItemStatus.GENERATION_FAILED, ValidationStatus.NOT_RUN,
                List.of(new PipelineError(
                        "generation", "LLM_GENERATION_FAILED", "boom", "candidate-1", true)));

        GeneratedTestEntity entity = EntityMapper.toTestEntity(failed, new TechnicalDebtEntity());

        assertNull(entity.getSourceCode());
        assertEquals(ItemStatus.GENERATION_FAILED, entity.getStatus());
        assertEquals(ValidationStatus.NOT_RUN, entity.getValidationStatus());
        assertEquals("openai", entity.getProvider());
        assertEquals("gpt-test", entity.getModel());
        assertEquals("v1", entity.getPromptVersion());
    }

    @Test
    void toSnapshotMapsRunColumnsAndSplitsReportPaths() {
        PipelineRunEntity run = new PipelineRunEntity();
        run.setId(UUID.randomUUID());
        run.setRunId("run-1");
        run.setStatus(RunStatus.COMPLETED);
        run.setStartedAt(Instant.parse("2026-09-15T10:00:00Z"));
        run.setFinishedAt(Instant.parse("2026-09-15T10:05:00Z"));
        run.setCandidateCount(3);
        run.setSatdCount(2);
        run.setGeneratedTestCount(1);
        run.setReportPaths("output/debt-report.json\noutput/debt-test-report.json");

        RunSnapshot snapshot = EntityMapper.toSnapshot(run);

        assertEquals(run.getId().toString(), snapshot.executionId());
        assertEquals("run-1", snapshot.runId());
        assertEquals(RunStatus.COMPLETED, snapshot.status());
        assertEquals(3, snapshot.candidateCount());
        assertEquals(List.of("output/debt-report.json", "output/debt-test-report.json"), snapshot.reportPaths());
    }

    @Test
    void toSnapshotReturnsEmptyReportPathsWhenNoneAreRecordedYet() {
        PipelineRunEntity run = new PipelineRunEntity();
        run.setId(UUID.randomUUID());
        run.setStatus(RunStatus.RUNNING);
        run.setStartedAt(Instant.now());

        RunSnapshot snapshot = EntityMapper.toSnapshot(run);

        assertTrue(snapshot.reportPaths().isEmpty());
    }

    @Test
    void toReportItemMapsContextAndGeneratedTestAssociations() {
        TechnicalDebtEntity debt = new TechnicalDebtEntity();
        debt.setCandidateId("candidate-1");
        debt.setFilePath("src/A.java");
        debt.setMethodName("save");
        debt.setLineNumber(10);
        debt.setComment("// TODO fix");
        debt.setSatd(true);
        debt.setDebtType("DESIGN");
        debt.setConfidence(0.9);

        DebtContextEntity context = new DebtContextEntity();
        context.setContextStatus(ContextStatus.MATCHED);
        context.setTaskKey("JIRA-1");
        context.setSummary("Fix it");
        context.setUrl("https://jira/JIRA-1");
        debt.setContext(context);

        GeneratedTestEntity test = new GeneratedTestEntity();
        test.setSourceCode("class Test {}");
        test.setProvider("openai");
        test.setModel("gpt-test");
        test.setStatus(ItemStatus.GENERATED);
        debt.setGeneratedTest(test);

        RunReportItem item = EntityMapper.toReportItem(debt, List.of());

        assertEquals("candidate-1", item.candidateId());
        assertEquals("JIRA-1", item.taskKey());
        assertEquals("Fix it", item.taskSummary());
        assertEquals("class Test {}", item.generatedTestSourceCode());
        assertEquals("GENERATED", item.testStatus());
        assertTrue(item.errorMessages().isEmpty());
    }

    @Test
    void toReportItemLeavesContextAndTestFieldsNullWhenAssociationsAreMissing() {
        TechnicalDebtEntity debt = new TechnicalDebtEntity();
        debt.setCandidateId("candidate-2");
        debt.setFilePath("src/B.java");
        debt.setMethodName("load");
        debt.setLineNumber(5);
        debt.setComment("// FIXME");
        debt.setSatd(false);
        debt.setDebtType("NON_SATD");

        PipelineErrorEntity error = new PipelineErrorEntity();
        error.setMessage("classification unavailable");

        RunReportItem item = EntityMapper.toReportItem(debt, List.of(error));

        assertNull(item.contextStatus());
        assertNull(item.taskKey());
        assertNull(item.generatedTestSourceCode());
        assertNull(item.testStatus());
        assertEquals(List.of("classification unavailable"), item.errorMessages());
    }
}
