package io.github.jonasfortes12.persistence.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

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
import io.github.jonasfortes12.core.model.SatdCandidate;
import io.github.jonasfortes12.core.model.SourceProvenance;
import io.github.jonasfortes12.core.model.ValidationStatus;
import io.github.jonasfortes12.persistence.entity.DebtContextEntity;
import io.github.jonasfortes12.persistence.entity.GeneratedTestEntity;
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
}
