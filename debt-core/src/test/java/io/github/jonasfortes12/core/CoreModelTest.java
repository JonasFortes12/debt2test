package io.github.jonasfortes12.core;

import io.github.jonasfortes12.core.error.PipelineException;
import io.github.jonasfortes12.core.model.ClassifiedDebt;
import io.github.jonasfortes12.core.model.ClassificationOptions;
import io.github.jonasfortes12.core.model.ContextStatus;
import io.github.jonasfortes12.core.model.EnrichedSatdDebt;
import io.github.jonasfortes12.core.model.ExternalReference;
import io.github.jonasfortes12.core.model.ExternalTaskSpec;
import io.github.jonasfortes12.core.model.GeneratedTest;
import io.github.jonasfortes12.core.model.ItemStatus;
import io.github.jonasfortes12.core.model.PipelineError;
import io.github.jonasfortes12.core.model.PipelineItemResult;
import io.github.jonasfortes12.core.model.PipelineRequest;
import io.github.jonasfortes12.core.model.PipelineResult;
import io.github.jonasfortes12.core.model.Provenance;
import io.github.jonasfortes12.core.model.ReportArtifact;
import io.github.jonasfortes12.core.model.RepositoryRequest;
import io.github.jonasfortes12.core.model.RepositoryWorkspace;
import io.github.jonasfortes12.core.model.RunStatus;
import io.github.jonasfortes12.core.model.SatdCandidate;
import io.github.jonasfortes12.core.model.SourceProvenance;
import io.github.jonasfortes12.core.model.ValidationStatus;
import io.github.jonasfortes12.core.result.ClassificationResult;
import io.github.jonasfortes12.core.result.ContextEnrichmentResult;
import io.github.jonasfortes12.core.result.ExtractionResult;
import io.github.jonasfortes12.core.result.TestGenerationResult;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CoreModelTest {

    @Test
    void candidateCopiesAndValidatesItsFields() {
        SatdCandidate candidate = sampleCandidate();

        assertEquals("run-1:src/A.java:10:run", candidate.candidateId());
        assertEquals("src/A.java", candidate.filePath());
        assertEquals("run", candidate.methodName());
        assertThrows(IllegalArgumentException.class, () -> new SatdCandidate(
                "id", "src/A.java", "run", 0, "comment", "source",
                candidate.sourceProvenance()));
        assertThrows(IllegalArgumentException.class, () -> new SatdCandidate(
                null, "src/A.java", "run", 10, "comment", "source",
                candidate.sourceProvenance()));
        assertThrows(IllegalArgumentException.class, () -> new SatdCandidate(
                "id", "src/A.java", "run", 10, " ", "source",
                candidate.sourceProvenance()));
        assertThrows(IllegalArgumentException.class, () -> new SatdCandidate(
                "id", "src/A.java", "run", 10, "comment", "source", null));
    }

    @Test
    void repositoryPathsMustRemainRelative() {
        assertThrows(IllegalArgumentException.class, () -> new SatdCandidate(
                "id", "/src/A.java", "run", 10, "comment", "source",
                candidateSource("src/A.java")));
        assertThrows(IllegalArgumentException.class, () -> new SatdCandidate(
                "id", "../src/A.java", "run", 10, "comment", "source",
                candidateSource("src/A.java")));
        assertThrows(IllegalArgumentException.class, () -> new SatdCandidate(
                "id", "..", "run", 10, "comment", "source",
                candidateSource("src/A.java")));
        assertThrows(IllegalArgumentException.class, () -> new SatdCandidate(
                "id", ".", "run", 10, "comment", "source",
                candidateSource("src/A.java")));
        assertThrows(IllegalArgumentException.class, () -> new SatdCandidate(
                "id", "src/..", "run", 10, "comment", "source",
                candidateSource("src/A.java")));
        assertThrows(IllegalArgumentException.class, () -> new SourceProvenance(
                "repo", "main", "/src/A.java"));
        assertThrows(IllegalArgumentException.class, () -> new SourceProvenance(
                "repo", "main", "../src/A.java"));
        assertThrows(IllegalArgumentException.class, () -> new SourceProvenance(
                "repo", "main", ".."));
        assertThrows(IllegalArgumentException.class, () -> new SourceProvenance(
                "repo", "main", "."));
        assertThrows(IllegalArgumentException.class, () -> new SourceProvenance(
                "repo", "main", "src/.."));
        assertThrows(IllegalArgumentException.class, () -> new SourceProvenance(
                "repo", "main", "C:\\workspace\\A.java"));

        SatdCandidate windowsPathCandidate = new SatdCandidate(
                "id", "src\\A.java", "run", 10, "comment", "source",
                candidateSource("src\\A.java"));
        assertEquals("src/A.java", windowsPathCandidate.filePath());
        assertEquals("src/A.java", windowsPathCandidate.sourceProvenance().relativeFilePath());
    }

    @Test
    void classificationEnforcesConfidenceBoundsAndStatuses() {
        ClassifiedDebt notSatd = new ClassifiedDebt(
                sampleCandidate(), false, "NONE", 0.0,
                sampleProvenance(), ItemStatus.NOT_SATD, List.of());
        ClassifiedDebt fullyConfident = new ClassifiedDebt(
                sampleCandidate(), true, "DESIGN_DEBT", 1.0,
                sampleProvenance(), ItemStatus.CLASSIFIED, List.of());
        PipelineError unavailableError = new PipelineError(
                "classification", "CLASSIFICATION_RESULT_MISSING", "classification unavailable",
                sampleCandidate().candidateId(), true);
        ClassifiedDebt unavailable = new ClassifiedDebt(
                sampleCandidate(), false, "UNCLASSIFIED", null,
                new Provenance("classifier", "unavailable", "unavailable"),
                ItemStatus.SKIPPED, List.of(unavailableError));

        assertEquals(0.0, notSatd.confidence());
        assertEquals(1.0, fullyConfident.confidence());
        assertEquals(ItemStatus.SKIPPED, unavailable.status());
        assertThrows(IllegalArgumentException.class, () -> new ClassifiedDebt(
                sampleCandidate(), false, "UNCLASSIFIED", null,
                new Provenance("classifier", "unavailable", "unavailable"), ItemStatus.SKIPPED, List.of()));
        assertThrows(IllegalArgumentException.class, () -> classifiedWithConfidence(-0.01));
        assertThrows(IllegalArgumentException.class, () -> classifiedWithConfidence(1.01));
        assertThrows(IllegalArgumentException.class, () -> classifiedWithConfidence(Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> classifiedWithConfidence(Double.POSITIVE_INFINITY));
        assertThrows(IllegalArgumentException.class, () -> new ClassifiedDebt(
                sampleCandidate(), true, "DESIGN_DEBT", null,
                sampleProvenance(), ItemStatus.NOT_SATD, List.of()));
        assertThrows(IllegalArgumentException.class, () -> new ClassifiedDebt(
                sampleCandidate(), false, "NONE", null,
                sampleProvenance(), ItemStatus.CLASSIFIED, List.of()));
        assertThrows(IllegalArgumentException.class, () -> new ClassifiedDebt(
                sampleCandidate(), false, "NONE", null,
                new Provenance("classifier", "unavailable", "unavailable"), ItemStatus.SKIPPED, List.of()));
        assertThrows(IllegalArgumentException.class, () -> new ClassifiedDebt(
                sampleCandidate(), false, "UNCLASSIFIED", null,
                sampleProvenance(), ItemStatus.SKIPPED, List.of()));
    }

    @Test
    void listBearingRecordsDefensivelyCopyAndExposeImmutableLists() {
        PipelineError error = new PipelineError("extract", "FAILED", "failed", "candidate-1", true);
        List<PipelineError> errors = new ArrayList<>(List.of(error));
        ClassifiedDebt classified = new ClassifiedDebt(
                sampleCandidate(), true, "DESIGN_DEBT", null,
                new Provenance("DebtHunter", "heuristic-fallback", "unavailable"),
                ItemStatus.CLASSIFIED, errors);
        errors.add(new PipelineError("extract", "OTHER", "other", null, true));

        assertEquals(1, classified.errors().size());
        assertThrows(UnsupportedOperationException.class, () -> classified.errors().add(error));

        List<String> acceptanceCriteria = new ArrayList<>(List.of("Add a test"));
        List<String> labels = new ArrayList<>(List.of("satd"));
        ExternalTaskSpec task = new ExternalTaskSpec(
                "tracker", "ABC-1", "Summary", null, acceptanceCriteria, labels, null);
        acceptanceCriteria.add("Add documentation");
        labels.add("java");

        assertEquals(List.of("Add a test"), task.acceptanceCriteria());
        assertEquals(List.of("satd"), task.labels());
        assertThrows(UnsupportedOperationException.class, () -> task.labels().add("other"));

        List<ExternalReference> references = new ArrayList<>(List.of(new ExternalReference("ABC-1", "comment")));
        EnrichedSatdDebt enriched = new EnrichedSatdDebt(
                classified, task, references, ContextStatus.MATCHED, errors);
        references.add(new ExternalReference("ABC-2", "comment"));

        assertEquals(1, enriched.references().size());
        assertEquals(2, enriched.errors().size());
        assertThrows(UnsupportedOperationException.class,
                () -> enriched.references().add(new ExternalReference("ABC-3", "comment")));
        assertThrows(UnsupportedOperationException.class, () -> enriched.errors().add(error));

        assertThrows(IllegalArgumentException.class,
                () -> new EnrichedSatdDebt(classified, null, List.of(), ContextStatus.MATCHED, List.of()));
        assertThrows(IllegalArgumentException.class,
                () -> new EnrichedSatdDebt(classified, task, List.of(), ContextStatus.MATCHED, List.of()));
        assertThrows(IllegalArgumentException.class,
                () -> new EnrichedSatdDebt(classified, task, List.of(), ContextStatus.NOT_FOUND, List.of()));
        assertThrows(IllegalArgumentException.class,
                () -> new EnrichedSatdDebt(classified, task, List.of(), ContextStatus.SKIPPED, List.of()));
        assertThrows(IllegalArgumentException.class,
                () -> new EnrichedSatdDebt(classified, task, List.of(), ContextStatus.FAILED, List.of()));
        assertEquals(ContextStatus.FAILED,
                new EnrichedSatdDebt(classified, null, List.of(), ContextStatus.FAILED, List.of(error)).contextStatus());

        GeneratedTest generated = new GeneratedTest(
                "run-1:src/A.java:10:run", null, "JUnit 5", "mock", "mock-model", "v1",
                ItemStatus.GENERATION_FAILED, ValidationStatus.NOT_RUN,
                List.of(new PipelineError("generation", "LLM_GENERATION_FAILED", "failed", "candidate-1", true)));
        assertNull(generated.sourceCode());
        assertThrows(UnsupportedOperationException.class, () -> generated.errors().add(error));

        PipelineItemResult item = new PipelineItemResult(
                sampleCandidate(), classified, enriched, generated, errors);
        ExtractionResult extraction = new ExtractionResult(List.of(sampleCandidate()), errors);
        ClassificationResult classification = new ClassificationResult(List.of(classified), errors);
        ContextEnrichmentResult context = new ContextEnrichmentResult(List.of(enriched), errors);
        TestGenerationResult generation = new TestGenerationResult(List.of(generated), errors);
        ReportArtifact artifact = new ReportArtifact(new ArrayList<>(List.of(Path.of("report.json"))));

        assertThrows(UnsupportedOperationException.class, () -> item.errors().add(error));
        assertThrows(UnsupportedOperationException.class, () -> extraction.errors().add(error));
        assertThrows(UnsupportedOperationException.class, () -> classification.classifications().add(classified));
        assertThrows(UnsupportedOperationException.class, () -> context.enrichments().add(enriched));
        assertThrows(UnsupportedOperationException.class, () -> context.errors().add(error));
        assertThrows(UnsupportedOperationException.class, () -> generation.generatedTests().add(generated));
        assertThrows(UnsupportedOperationException.class, () -> artifact.paths().add(Path.of("other.json")));
    }

    @Test
    void pipelineItemRequiresMatchingCandidateIdsButAllowsPartialStages() {
        SatdCandidate candidate = sampleCandidate();
        ClassifiedDebt classification = classifiedDebt(candidate, true, ItemStatus.CLASSIFIED);
        SatdCandidate otherCandidate = candidateWithId("other-candidate");
        ClassifiedDebt otherClassification = classifiedDebt(otherCandidate, true, ItemStatus.CLASSIFIED);
        EnrichedSatdDebt otherEnrichment = new EnrichedSatdDebt(
                otherClassification, null, List.of(), ContextStatus.FAILED,
                List.of(new PipelineError("context", "FAILED", "failed", "other-candidate", true)));
        GeneratedTest otherGenerated = new GeneratedTest(
                "other-candidate", "class Generated {}", "JUnit 5", "mock", "model", "v1",
                ItemStatus.GENERATED, ValidationStatus.NOT_RUN, List.of());

        assertThrows(IllegalArgumentException.class,
                () -> new PipelineItemResult(candidate, otherClassification, null, null, List.of()));
        assertThrows(IllegalArgumentException.class,
                () -> new PipelineItemResult(candidate, classification, otherEnrichment, null, List.of()));
        assertThrows(IllegalArgumentException.class,
                () -> new PipelineItemResult(candidate, classification, null, otherGenerated, List.of()));

        PipelineItemResult partial = new PipelineItemResult(
                candidate, classification, null, null, List.of());
        assertNull(partial.enrichment());
        assertNull(partial.generatedTest());
    }

    @Test
    void generatedTestStatusesAndValidationStatesAreConsistent() {
        GeneratedTest successful = new GeneratedTest(
                "candidate-1", "class Generated {}", "JUnit 5", "mock", "model", "v1",
                ItemStatus.GENERATED, ValidationStatus.VALIDATED, List.of());
        GeneratedTest failed = new GeneratedTest(
                "candidate-1", null, "JUnit 5", "mock", "model", "v1",
                ItemStatus.GENERATION_FAILED, ValidationStatus.NOT_RUN,
                List.of(new PipelineError("generation", "LLM_GENERATION_FAILED", "failed", "candidate-1", true)));

        assertEquals(ItemStatus.GENERATED, successful.status());
        assertEquals(ItemStatus.GENERATION_FAILED, failed.status());
        assertThrows(IllegalArgumentException.class, () -> new GeneratedTest(
                "candidate-1", null, "JUnit 5", "mock", "model", "v1",
                ItemStatus.GENERATION_FAILED, ValidationStatus.VALIDATED, List.of()));
        assertThrows(IllegalArgumentException.class, () -> new GeneratedTest(
                "candidate-1", null, "JUnit 5", "mock", "model", "v1",
                ItemStatus.GENERATION_FAILED, ValidationStatus.VALIDATION_FAILED, List.of()));
        assertThrows(IllegalArgumentException.class, () -> new GeneratedTest(
                "candidate-1", null, "JUnit 5", "mock", "model", "v1",
                ItemStatus.GENERATION_FAILED, ValidationStatus.NOT_RUN, List.of()));
        assertThrows(IllegalArgumentException.class, () -> new GeneratedTest(
                "candidate-1", "class Generated {}", "JUnit 5", "mock", "model", "v1",
                ItemStatus.GENERATED, ValidationStatus.NOT_RUN,
                List.of(new PipelineError("generation", "LLM_GENERATION_FAILED", "failed", "candidate-1", true))));
        assertThrows(IllegalArgumentException.class, () -> new GeneratedTest(
                "candidate-1", "class Generated {}", "JUnit 5", "mock", "model", "v1",
                ItemStatus.CLASSIFIED, ValidationStatus.NOT_RUN, List.of()));
        assertThrows(IllegalArgumentException.class, () -> new GeneratedTest(
                "candidate-1", null, "JUnit 5", "mock", "model", "v1",
                ItemStatus.GENERATED, ValidationStatus.NOT_RUN, List.of()));
    }

    @Test
    void nullCollectionsAreRejected() {
        SatdCandidate candidate = sampleCandidate();
        ClassifiedDebt classification = classifiedDebt(candidate, true, ItemStatus.CLASSIFIED);
        EnrichedSatdDebt enrichment = new EnrichedSatdDebt(
                classification, null, List.of(), ContextStatus.FAILED,
                List.of(new PipelineError("context", "FAILED", "failed", candidate.candidateId(), true)));
        GeneratedTest generated = new GeneratedTest(
                candidate.candidateId(), "class Generated {}", "JUnit 5", "mock", "model", "v1",
                ItemStatus.GENERATED, ValidationStatus.NOT_RUN, List.of());

        assertThrows(NullPointerException.class, () -> new ClassifiedDebt(
                candidate, true, "DESIGN_DEBT", null, sampleProvenance(), ItemStatus.CLASSIFIED, null));
        assertThrows(NullPointerException.class, () -> new ExternalTaskSpec(
                "tracker", "ABC-1", "Summary", null, null, List.of(), null));
        assertThrows(NullPointerException.class, () -> new ExternalTaskSpec(
                "tracker", "ABC-1", "Summary", null, List.of(), null, null));
        assertThrows(IllegalArgumentException.class, () -> new ExternalTaskSpec(
                " ", "ABC-1", "Summary", null, List.of(), List.of(), null));
        assertThrows(NullPointerException.class, () -> new EnrichedSatdDebt(
                classification, null, null, ContextStatus.FAILED, List.of()));
        assertThrows(NullPointerException.class, () -> new EnrichedSatdDebt(
                classification, null, List.of(), ContextStatus.FAILED, null));
        assertThrows(NullPointerException.class, () -> new GeneratedTest(
                candidate.candidateId(), "class Generated {}", "JUnit 5", "mock", "model", "v1",
                ItemStatus.GENERATED, ValidationStatus.NOT_RUN, null));
        assertThrows(NullPointerException.class, () -> new PipelineItemResult(
                candidate, classification, enrichment, generated, null));
        assertThrows(NullPointerException.class, () -> new ExtractionResult(null, List.of()));
        assertThrows(NullPointerException.class, () -> new ExtractionResult(List.of(candidate), null));
        assertThrows(NullPointerException.class, () -> new ClassificationResult(null, List.of()));
        assertThrows(NullPointerException.class, () -> new ClassificationResult(List.of(classification), null));
        assertThrows(NullPointerException.class, () -> new ContextEnrichmentResult(null, List.of()));
        assertThrows(NullPointerException.class, () -> new ContextEnrichmentResult(List.of(enrichment), null));
        assertThrows(NullPointerException.class, () -> new TestGenerationResult(null, List.of()));
        assertThrows(NullPointerException.class, () -> new TestGenerationResult(List.of(generated), null));
        assertThrows(NullPointerException.class, () -> new PipelineResult(
                "run-1", RunStatus.QUEUED, null, null, List.of(), null));
        assertThrows(NullPointerException.class, () -> new PipelineResult(
                "run-1", RunStatus.QUEUED, null, List.of(), null, null));
        assertThrows(NullPointerException.class, () -> new ReportArtifact(null));
        assertThrows(IllegalArgumentException.class, () -> new ReportArtifact(List.of()));
    }

    @Test
    void stageResultsPermitNullEntriesButRemainDefensivelyCopiedAndImmutable() {
        ClassifiedDebt classification = classifiedDebt(sampleCandidate(), true, ItemStatus.CLASSIFIED);
        EnrichedSatdDebt enrichment = new EnrichedSatdDebt(
                classification, null, List.of(), ContextStatus.FAILED,
                List.of(new PipelineError("context", "FAILED", "failed", classification.candidateId(), true)));
        GeneratedTest generated = new GeneratedTest(
                classification.candidateId(), "class Generated {}", "JUnit 5", "mock", "model", "v1",
                ItemStatus.GENERATED, ValidationStatus.NOT_RUN, List.of());

        List<SatdCandidate> candidates = new ArrayList<>(Arrays.asList(sampleCandidate(), null));
        ExtractionResult extraction = new ExtractionResult(candidates, List.of());
        candidates.clear();
        assertEquals(2, extraction.candidates().size());
        assertNull(extraction.candidates().get(1));
        assertThrows(UnsupportedOperationException.class, () -> extraction.candidates().add(null));

        List<ClassifiedDebt> classifications = new ArrayList<>(Arrays.asList(classification, null));
        ClassificationResult classifiedResult = new ClassificationResult(classifications, List.of());
        classifications.clear();
        assertNull(classifiedResult.classifications().get(1));
        assertThrows(UnsupportedOperationException.class, () -> classifiedResult.classifications().add(null));

        List<EnrichedSatdDebt> enrichments = new ArrayList<>(Arrays.asList(enrichment, null));
        ContextEnrichmentResult context = new ContextEnrichmentResult(enrichments, List.of());
        enrichments.clear();
        assertNull(context.enrichments().get(1));
        assertThrows(UnsupportedOperationException.class, () -> context.enrichments().add(null));

        List<GeneratedTest> generatedTests = new ArrayList<>(Arrays.asList(generated, null));
        TestGenerationResult generation = new TestGenerationResult(generatedTests, List.of());
        generatedTests.clear();
        assertNull(generation.generatedTests().get(1));
        assertThrows(UnsupportedOperationException.class, () -> generation.generatedTests().add(null));
    }

    @Test
    void pipelineRequestRequiresMatchingRunIdsAndRequiredValues() {
        PipelineRequest request = sampleRequest();

        assertEquals("run-1", request.runId());
        assertEquals("run-1", request.extraction().runId());
        assertThrows(IllegalArgumentException.class, () -> new PipelineRequest(
                "run-2", request.repository(), request.extraction(), request.classification(),
                request.context(), request.testGeneration(), request.report()));
        assertThrows(IllegalArgumentException.class, () -> new PipelineRequest(
                "run-1", null, request.extraction(), request.classification(),
                request.context(), request.testGeneration(), request.report()));
    }

    @Test
    void pipelineResultSupportsPreReportAndImmutableUpdates() {
        PipelineError error = new PipelineError("report", "WRITE_FAILED", "write failed", null, false);
        PipelineResult result = new PipelineResult(
                "run-1", RunStatus.FAILED, null, List.of(), List.of(error), null);
        ReportArtifact artifact = new ReportArtifact(List.of(Path.of("report.json")));

        PipelineResult reported = result.withReportArtifact(artifact);
        PipelineResult completed = reported.withStatus(RunStatus.COMPLETED_WITH_ERRORS);
        PipelineError additionalError = new PipelineError("context", "NOT_FOUND", "not found", "candidate-1", true);
        PipelineResult withAdditionalError = completed.withAdditionalError(additionalError);

        assertNull(result.workspace());
        assertNull(result.reportArtifact());
        assertEquals(artifact, reported.reportArtifact());
        assertEquals(RunStatus.COMPLETED_WITH_ERRORS, completed.status());
        assertEquals(2, withAdditionalError.errors().size());
        assertNotSame(result, reported);
        assertNotSame(reported, completed);
        assertThrows(NullPointerException.class, () -> result.withReportArtifact(null));
        assertThrows(UnsupportedOperationException.class, () -> withAdditionalError.errors().add(error));
    }

    @Test
    void statusAndErrorValuesPreserveTheirContract() {
        assertArrayEquals(new RunStatus[]{
                RunStatus.QUEUED, RunStatus.RUNNING, RunStatus.COMPLETED,
                RunStatus.COMPLETED_WITH_ERRORS, RunStatus.FAILED, RunStatus.CANCELLED
        }, RunStatus.values());
        assertArrayEquals(new ItemStatus[]{
                ItemStatus.EXTRACTED, ItemStatus.CLASSIFIED, ItemStatus.NOT_SATD,
                ItemStatus.CONTEXT_MATCHED, ItemStatus.CONTEXT_NOT_FOUND,
                ItemStatus.CONTEXT_FAILED, ItemStatus.GENERATED, ItemStatus.GENERATION_FAILED,
                ItemStatus.VALIDATED, ItemStatus.VALIDATION_FAILED, ItemStatus.SKIPPED
        }, ItemStatus.values());
        assertArrayEquals(new ContextStatus[]{
                ContextStatus.MATCHED, ContextStatus.NOT_FOUND, ContextStatus.FAILED, ContextStatus.SKIPPED
        }, ContextStatus.values());
        assertArrayEquals(new ValidationStatus[]{
                ValidationStatus.NOT_RUN, ValidationStatus.VALIDATED, ValidationStatus.VALIDATION_FAILED
        }, ValidationStatus.values());

        PipelineError error = new PipelineError("classification", "MODEL_FAILED", "model failed", null, false);
        assertEquals("classification", error.stage());
        assertEquals("MODEL_FAILED", error.code());
        assertEquals("model failed", error.message());
        assertNull(error.candidateId());
        assertFalse(error.recoverable());
        assertThrows(IllegalArgumentException.class,
                () -> new PipelineError("classification", "MODEL_FAILED", "model failed", " ", false));
        assertEquals(error, new PipelineException(error).error());
        assertTrue(new PipelineException(error, new IllegalStateException()).getCause() instanceof IllegalStateException);
    }

    private static SatdCandidate sampleCandidate() {
        return new SatdCandidate(
                "run-1:src/A.java:10:run",
                "src/A.java",
                "run",
                10,
                "TODO: simplify",
                "void run() {}",
                new SourceProvenance("https://example.test/repo", "main", "src/A.java"));
    }

    private static PipelineRequest sampleRequest() {
        return new PipelineRequest(
                "run-1",
                new RepositoryRequest("https://example.test/repo", "main"),
                new io.github.jonasfortes12.core.model.ExtractionOptions("run-1"),
                new ClassificationOptions(true),
                new io.github.jonasfortes12.core.model.ContextRequest(true),
                new io.github.jonasfortes12.core.model.TestGenerationOptions("JUnit 5", "v1"),
                new io.github.jonasfortes12.core.model.ReportOptions(Path.of("output")));
    }

    private static SourceProvenance candidateSource(String relativeFilePath) {
        return new SourceProvenance("https://example.test/repo", "main", relativeFilePath);
    }

    private static Provenance sampleProvenance() {
        return new Provenance("DebtHunter", "heuristic-fallback", "unavailable");
    }

    private static ClassifiedDebt classifiedDebt(
            SatdCandidate candidate, boolean satd, ItemStatus status) {
        return new ClassifiedDebt(candidate, satd, satd ? "DESIGN_DEBT" : "NONE", null,
                sampleProvenance(), status, List.of());
    }

    private static ClassifiedDebt classifiedWithConfidence(double confidence) {
        return new ClassifiedDebt(sampleCandidate(), true, "DESIGN_DEBT", confidence,
                sampleProvenance(), ItemStatus.CLASSIFIED, List.of());
    }

    private static SatdCandidate candidateWithId(String candidateId) {
        SatdCandidate candidate = sampleCandidate();
        return new SatdCandidate(candidateId, candidate.filePath(), candidate.methodName(),
                candidate.lineNumber(), candidate.comment(), candidate.methodSourceCode(),
                candidate.sourceProvenance());
    }
}
