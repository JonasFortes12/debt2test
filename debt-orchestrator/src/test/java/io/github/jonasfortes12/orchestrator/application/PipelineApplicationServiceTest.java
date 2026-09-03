package io.github.jonasfortes12.orchestrator.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.github.jonasfortes12.core.error.PipelineException;
import io.github.jonasfortes12.core.model.ClassificationOptions;
import io.github.jonasfortes12.core.model.ClassifiedDebt;
import io.github.jonasfortes12.core.model.ContextRequest;
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
import io.github.jonasfortes12.core.model.ReportOptions;
import io.github.jonasfortes12.core.model.RepositoryRequest;
import io.github.jonasfortes12.core.model.RepositoryWorkspace;
import io.github.jonasfortes12.core.model.RunStatus;
import io.github.jonasfortes12.core.model.SatdCandidate;
import io.github.jonasfortes12.core.model.SourceProvenance;
import io.github.jonasfortes12.core.model.TestGenerationOptions;
import io.github.jonasfortes12.core.model.ValidationStatus;
import io.github.jonasfortes12.core.port.ContextEnricher;
import io.github.jonasfortes12.core.port.DebtClassifier;
import io.github.jonasfortes12.core.port.ReportSink;
import io.github.jonasfortes12.core.port.RepositoryWorkspaceProvider;
import io.github.jonasfortes12.core.port.SatdExtractor;
import io.github.jonasfortes12.core.port.TestGenerator;
import io.github.jonasfortes12.core.result.ClassificationResult;
import io.github.jonasfortes12.core.result.ContextEnrichmentResult;
import io.github.jonasfortes12.core.result.ExtractionResult;
import io.github.jonasfortes12.core.result.TestGenerationResult;

class PipelineApplicationServiceTest {

    @Test
    void executesStagesInOrderCorrelatesByIdAndGeneratesOnlyForSatd() {
        List<String> calls = new ArrayList<>();
        FakeStages stages = FakeStages.successful(calls);

        PipelineResult result = stages.service().run(request("run-1"));

        assertEquals(List.of("prepare", "extract", "classify", "context", "generate", "release", "report"), calls);
        assertEquals(RunStatus.COMPLETED, result.status());
        assertEquals(List.of(stages.candidateC.candidateId(), stages.candidateA.candidateId()), stages.contextInputIds);
        assertEquals(List.of(stages.candidateA.candidateId(), stages.candidateC.candidateId()),
                stages.generationInputIds);

        Map<String, PipelineItemResult> items = itemsById(result);
        assertEquals(stages.classifiedA, items.get(stages.candidateA.candidateId()).classification());
        assertEquals(stages.classifiedC, items.get(stages.candidateC.candidateId()).classification());
        assertEquals(stages.candidateA.candidateId(),
                items.get(stages.candidateA.candidateId()).generatedTest().candidateId());
        assertEquals(stages.candidateC.candidateId(),
                items.get(stages.candidateC.candidateId()).generatedTest().candidateId());
        assertNull(items.get(stages.candidateB.candidateId()).enrichment());
        assertNull(items.get(stages.candidateB.candidateId()).generatedTest());
        assertTrue(result.errors().isEmpty());
    }

    @Test
    void preservesSuccessfulItemsWhenOneGenerationFails() {
        List<String> calls = new ArrayList<>();
        FakeStages stages = FakeStages.successful(calls);
        stages.generationFailureId = stages.candidateC.candidateId();

        PipelineResult result = stages.service().run(request("run-1"));

        Map<String, PipelineItemResult> items = itemsById(result);
        assertEquals(RunStatus.COMPLETED_WITH_ERRORS, result.status());
        assertEquals(ItemStatus.GENERATED, items.get(stages.candidateA.candidateId()).generatedTest().status());
        assertEquals(ItemStatus.GENERATION_FAILED, items.get(stages.candidateC.candidateId()).generatedTest().status());
        assertTrue(result.errors().stream().anyMatch(error -> error.code().equals("LLM_GENERATION_FAILED")));
    }

    @Test
    void fatalWorkspaceFailureSkipsAllDownstreamCalls() {
        List<String> calls = new ArrayList<>();
        FakeStages stages = FakeStages.successful(calls);
        PipelineError failure = new PipelineError(
                "repository", "WORKSPACE_PREPARATION_FAILED", "workspace unavailable", null, false);
        stages.prepareFailure = new PipelineException(failure);

        PipelineResult result = stages.service().run(request("run-1"));

        assertEquals(List.of("prepare"), calls);
        assertEquals(RunStatus.FAILED, result.status());
        assertNull(result.workspace());
        assertNull(result.reportArtifact());
        assertEquals("repository", result.errors().get(0).stage());
        assertEquals("WORKSPACE_PREPARATION_FAILED", result.errors().get(0).code());
        assertFalse(result.errors().get(0).message().contains("workspace unavailable"));
    }

    @Test
    void releaseFailureIsSanitizedAndDoesNotDiscardSuccessfulItems() {
        List<String> calls = new ArrayList<>();
        FakeStages stages = FakeStages.successful(calls);
        stages.releaseFailure = new PipelineException(new PipelineError(
                "repository", "WORKSPACE_CLEANUP_FAILED", "credential=top-secret", null, false));

        PipelineResult result = stages.service().run(request("run-1"));

        assertEquals(List.of("prepare", "extract", "classify", "context", "generate", "release", "report"), calls);
        assertEquals(RunStatus.FAILED, result.status());
        assertTrue(itemsById(result).get(stages.candidateA.candidateId()).generatedTest() != null);
        PipelineError releaseError = result.errors().stream()
                .filter(error -> error.code().equals("WORKSPACE_RELEASE_FAILED"))
                .findFirst()
                .orElseThrow();
        assertEquals("repository", releaseError.stage());
        assertFalse(releaseError.message().contains("top-secret"));
    }

    @Test
    void reportFailureIsSanitizedAndLeavesAssembledItemsAvailable() {
        List<String> calls = new ArrayList<>();
        FakeStages stages = FakeStages.successful(calls);
        stages.reportFailure = new RuntimeException("api-key=top-secret");

        PipelineResult result = stages.service().run(request("run-1"));

        assertEquals(RunStatus.FAILED, result.status());
        assertTrue(itemsById(result).get(stages.candidateA.candidateId()).generatedTest() != null);
        PipelineError reportError = result.errors().stream()
                .filter(error -> error.code().equals("REPORT_WRITE_FAILED"))
                .findFirst()
                .orElseThrow();
        assertFalse(reportError.message().contains("top-secret"));
    }

    @Test
    void missingClassificationBecomesCorrelationErrorInsteadOfDroppingCandidate() {
        List<String> calls = new ArrayList<>();
        FakeStages stages = FakeStages.successful(calls);
        stages.classification = new ClassificationResult(List.of(stages.classifiedA), List.of());

        PipelineResult result = stages.service().run(request("run-1"));

        PipelineItemResult missing = itemsById(result).get(stages.candidateB.candidateId());
        assertTrue(missing != null);
        assertEquals(ItemStatus.SKIPPED, missing.classification().status());
        assertEquals("UNCLASSIFIED", missing.classification().debtType());
        assertFalse(missing.classification().errors().isEmpty());
        assertFalse(stages.contextInputIds.contains(stages.candidateB.candidateId()));
        assertFalse(stages.generationInputIds.contains(stages.candidateB.candidateId()));
        assertTrue(missing.errors().stream()
                .anyMatch(error -> error.code().equals("CLASSIFICATION_RESULT_MISSING")));
        assertTrue(result.errors().stream()
                .anyMatch(error -> error.code().equals("CLASSIFICATION_RESULT_MISSING")));
    }

    @Test
    void preservesEmbeddedErrorsFromUnknownAndDuplicateClassifications() {
        List<String> calls = new ArrayList<>();
        FakeStages stages = FakeStages.successful(calls);
        ClassifiedDebt unknown = withErrors(stages.classifiedD, List.of(error(
                "classification", "UNKNOWN_CLASSIFICATION_FATAL", stages.candidateD.candidateId(), false,
                "provider secret")));
        ClassifiedDebt duplicate = withErrors(stages.classifiedA, List.of(error(
                "classification", "DUPLICATE_CLASSIFICATION_FATAL", stages.candidateA.candidateId(), false,
                "duplicate provider secret")));
        stages.classification = new ClassificationResult(
                List.of(unknown, stages.classifiedB, stages.classifiedC, stages.classifiedA, duplicate), List.of());

        PipelineResult result = stages.service().run(request("run-1"));

        assertEquals(RunStatus.FAILED, result.status());
        assertFalse(itemsById(result).containsKey(stages.candidateD.candidateId()));
        assertTrue(result.errors().stream().anyMatch(error -> error.code().equals("UNKNOWN_CLASSIFICATION_FATAL")));
        assertTrue(result.errors().stream().anyMatch(error -> error.code().equals("DUPLICATE_CLASSIFICATION_FATAL")));
    }

    @Test
    void nonRecoverableStageErrorMakesRunFailed() {
        List<String> calls = new ArrayList<>();
        FakeStages stages = FakeStages.successful(calls);
        stages.extractionResult = new ExtractionResult(
                List.of(stages.candidateA, stages.candidateB, stages.candidateC),
                List.of(error("extraction", "EXTRACTION_FATAL", null, false, "fatal extraction result")));

        PipelineResult result = stages.service().run(request("run-1"));

        assertEquals(RunStatus.FAILED, result.status());
        assertTrue(result.errors().stream().anyMatch(item -> item.code().equals("EXTRACTION_FATAL")));
        assertEquals(3, result.items().size());
    }

    @Test
    void unexpectedPreparationExceptionIsSanitized() {
        List<String> calls = new ArrayList<>();
        FakeStages stages = FakeStages.successful(calls);
        stages.prepareFailure = new RuntimeException("repository secret");

        PipelineResult result = stages.service().run(request("run-1"));

        assertEquals(List.of("prepare"), calls);
        assertEquals(RunStatus.FAILED, result.status());
        assertEquals(1, result.errors().size());
        assertEquals("WORKSPACE_PREPARATION_FAILED", result.errors().get(0).code());
        assertEquals("repository", result.errors().get(0).stage());
        assertFalse(result.errors().get(0).recoverable());
        assertNull(result.errors().get(0).candidateId());
        assertFalse(result.errors().get(0).message().contains("secret"));
    }

    @Test
    void malformedPreparationPipelineErrorIsReboundAndSanitized() {
        List<String> calls = new ArrayList<>();
        FakeStages stages = FakeStages.successful(calls);
        stages.prepareFailure = new PipelineException(new PipelineError(
                "attacker", "LEAK", "credential=top-secret", stages.candidateA.candidateId(), true));

        PipelineResult result = stages.service().run(request("run-1"));

        PipelineError preparationError = result.errors().get(0);
        assertEquals("repository", preparationError.stage());
        assertEquals("WORKSPACE_PREPARATION_FAILED", preparationError.code());
        assertFalse(preparationError.recoverable());
        assertNull(preparationError.candidateId());
        assertFalse(preparationError.message().contains("top-secret"));
    }

    @Test
    void unexpectedClassifierExceptionReportsPartialResultAndSkipsUnavailableStages() {
        List<String> calls = new ArrayList<>();
        FakeStages stages = FakeStages.successful(calls);
        stages.classifierFailure = new RuntimeException("classifier secret");

        PipelineResult result = stages.service().run(request("run-1"));

        assertEquals(List.of("prepare", "extract", "classify", "release", "report"), calls);
        assertEquals(RunStatus.FAILED, result.status());
        assertEquals(3, result.items().size());
        assertTrue(result.items().stream().allMatch(item -> item.classification() != null));
        assertTrue(result.items().stream().allMatch(item -> item.enrichment() == null && item.generatedTest() == null));
        assertStageFailureIsSanitized(result, "classification", "CLASSIFICATION_STAGE_FAILED", "classifier secret");
    }

    @Test
    void unexpectedExtractorExceptionReportsEmptyPartialResult() {
        List<String> calls = new ArrayList<>();
        FakeStages stages = FakeStages.successful(calls);
        stages.extractorFailure = new RuntimeException("extractor secret");

        PipelineResult result = stages.service().run(request("run-1"));

        assertEquals(List.of("prepare", "extract", "release", "report"), calls);
        assertEquals(RunStatus.FAILED, result.status());
        assertTrue(result.items().isEmpty());
        assertStageFailureIsSanitized(result, "extraction", "EXTRACTION_STAGE_FAILED", "extractor secret");
    }

    @Test
    void unexpectedContextExceptionSynthesizesFailedSatdContextAndSkipsGeneration() {
        List<String> calls = new ArrayList<>();
        FakeStages stages = FakeStages.successful(calls);
        stages.contextFailure = new RuntimeException("context secret");

        PipelineResult result = stages.service().run(request("run-1"));

        assertEquals(List.of("prepare", "extract", "classify", "context", "release", "report"), calls);
        assertEquals(RunStatus.FAILED, result.status());
        Map<String, PipelineItemResult> items = itemsById(result);
        assertEquals(ContextStatus.FAILED, items.get(stages.candidateA.candidateId()).enrichment().contextStatus());
        assertEquals(ContextStatus.FAILED, items.get(stages.candidateC.candidateId()).enrichment().contextStatus());
        assertNull(items.get(stages.candidateA.candidateId()).generatedTest());
        assertNull(items.get(stages.candidateB.candidateId()).enrichment());
        assertStageFailureIsSanitized(result, "context", "CONTEXT_STAGE_FAILED", "context secret");
    }

    @Test
    void unexpectedGenerationExceptionPreservesContextAndReportsMissingGeneration() {
        List<String> calls = new ArrayList<>();
        FakeStages stages = FakeStages.successful(calls);
        stages.generatorFailure = new RuntimeException("generation secret");

        PipelineResult result = stages.service().run(request("run-1"));

        assertEquals(List.of("prepare", "extract", "classify", "context", "generate", "release", "report"), calls);
        assertEquals(RunStatus.FAILED, result.status());
        Map<String, PipelineItemResult> items = itemsById(result);
        assertNotNull(items.get(stages.candidateA.candidateId()).enrichment());
        assertNull(items.get(stages.candidateA.candidateId()).generatedTest());
        assertTrue(items.get(stages.candidateA.candidateId()).errors().stream()
                .anyMatch(error -> error.code().equals("GENERATION_RESULT_MISSING")));
        assertStageFailureIsSanitized(result, "generation", "GENERATION_STAGE_FAILED", "generation secret");
    }

    @Test
    void nullWorkspaceIsFatalAndIsNotReleasedOrReported() {
        List<String> calls = new ArrayList<>();
        FakeStages stages = FakeStages.successful(calls);
        stages.preparedWorkspace = null;

        PipelineResult result = stages.service().run(request("run-1"));

        assertEquals(List.of("prepare"), calls);
        assertEquals(RunStatus.FAILED, result.status());
        assertEquals("WORKSPACE_PREPARATION_FAILED", result.errors().get(0).code());
        assertNull(result.workspace());
    }

    @Test
    void nullStageResultIsFatalAndReportsPartialResult() {
        List<String> calls = new ArrayList<>();
        FakeStages stages = FakeStages.successful(calls);
        stages.extractionResult = null;

        PipelineResult result = stages.service().run(request("run-1"));

        assertEquals(List.of("prepare", "extract", "release", "report"), calls);
        assertEquals(RunStatus.FAILED, result.status());
        assertTrue(result.errors().stream().anyMatch(error -> error.code().equals("EXTRACTION_RESULT_MISSING")));
        assertNotNull(stages.reportedResult);
    }

    @Test
    void nullClassifierResultPreservesExtractedCandidates() {
        List<String> calls = new ArrayList<>();
        FakeStages stages = FakeStages.successful(calls);
        stages.classification = null;

        PipelineResult result = stages.service().run(request("run-1"));

        assertEquals(List.of("prepare", "extract", "classify", "release", "report"), calls);
        assertEquals(RunStatus.FAILED, result.status());
        assertEquals(3, result.items().size());
        assertTrue(result.errors().stream().anyMatch(error -> error.code().equals("CLASSIFICATION_RESULT_MISSING")));
    }

    @Test
    void nullContextResultSynthesizesFailuresAndDoesNotGenerateWithoutContext() {
        List<String> calls = new ArrayList<>();
        FakeStages stages = FakeStages.successful(calls);
        stages.contextResult = null;

        PipelineResult result = stages.service().run(request("run-1"));

        assertEquals(List.of("prepare", "extract", "classify", "context", "release", "report"), calls);
        assertEquals(RunStatus.FAILED, result.status());
        assertTrue(result.items().stream().filter(item -> item.classification().satd())
                .allMatch(item -> item.enrichment().contextStatus() == ContextStatus.FAILED));
    }

    @Test
    void nullGenerationResultLeavesExpectedTestsMissing() {
        List<String> calls = new ArrayList<>();
        FakeStages stages = FakeStages.successful(calls);
        stages.generationResult = null;
        stages.generatorReturnsNull = true;

        PipelineResult result = stages.service().run(request("run-1"));

        assertEquals(RunStatus.FAILED, result.status());
        assertTrue(result.errors().stream().anyMatch(error -> error.code().equals("GENERATION_RESULT_MISSING")));
        assertTrue(result.items().stream().filter(item -> item.classification().satd())
                .allMatch(item -> item.generatedTest() == null));
    }

    @Test
    void preservesEmbeddedErrorsFromRejectedContextAndGenerationOutputs() {
        List<String> calls = new ArrayList<>();
        FakeStages stages = FakeStages.successful(calls);
        stages.contextResult = new ContextEnrichmentResult(List.of(
                stages.enrichedA,
                withErrors(stages.enrichedA, List.of(error(
                        "context", "DUPLICATE_CONTEXT_FATAL", stages.candidateA.candidateId(), false,
                        "context provider secret"))),
                withErrors(stages.enrichedB, List.of(error(
                        "context", "NON_SATD_CONTEXT_FATAL", stages.candidateB.candidateId(), false,
                        "context provider secret"))),
                withErrors(stages.enrichedD, List.of(error(
                        "context", "UNKNOWN_CONTEXT_FATAL", stages.candidateD.candidateId(), false,
                        "context provider secret")))),
                List.of());
        stages.generationResult = new TestGenerationResult(List.of(
                stages.generated(stages.candidateA.candidateId()),
                stages.failedGenerated(stages.candidateA.candidateId(), error(
                        "generation", "DUPLICATE_GENERATION_FATAL", stages.candidateA.candidateId(), false,
                        "generation provider secret")),
                stages.failedGenerated(stages.candidateB.candidateId(), error(
                        "generation", "NON_SATD_GENERATION_FATAL", stages.candidateB.candidateId(), false,
                        "generation provider secret")),
                stages.failedGenerated(stages.candidateD.candidateId(), error(
                        "generation", "UNKNOWN_GENERATION_FATAL", stages.candidateD.candidateId(), false,
                        "generation provider secret"))),
                List.of());

        PipelineResult result = stages.service().run(request("run-1"));

        assertEquals(RunStatus.FAILED, result.status());
        assertTrue(result.errors().stream().anyMatch(error -> error.code().equals("DUPLICATE_CONTEXT_FATAL")));
        assertTrue(result.errors().stream().anyMatch(error -> error.code().equals("NON_SATD_CONTEXT_FATAL")));
        assertTrue(result.errors().stream().anyMatch(error -> error.code().equals("UNKNOWN_CONTEXT_FATAL")));
        assertTrue(result.errors().stream().anyMatch(error -> error.code().equals("DUPLICATE_GENERATION_FATAL")));
        assertTrue(result.errors().stream().anyMatch(error -> error.code().equals("NON_SATD_GENERATION_FATAL")));
        assertTrue(result.errors().stream().anyMatch(error -> error.code().equals("UNKNOWN_GENERATION_FATAL")));
    }

    @Test
    void missingLaterOutputsRemainVisibleOnTheirCandidateItems() {
        List<String> calls = new ArrayList<>();
        FakeStages stages = FakeStages.successful(calls);
        stages.contextResult = new ContextEnrichmentResult(List.of(stages.enrichedA), List.of());
        stages.generationResult = new TestGenerationResult(List.of(), List.of());

        PipelineResult result = stages.service().run(request("run-1"));

        Map<String, PipelineItemResult> items = itemsById(result);
        assertTrue(items.get(stages.candidateA.candidateId()).errors().stream()
                .anyMatch(error -> error.code().equals("GENERATION_RESULT_MISSING")));
        assertTrue(items.get(stages.candidateC.candidateId()).errors().stream()
                .anyMatch(error -> error.code().equals("CONTEXT_RESULT_MISSING")));
        assertEquals(RunStatus.COMPLETED_WITH_ERRORS, result.status());
    }

    @Test
    void nullReportArtifactIsFatalAfterRelease() {
        List<String> calls = new ArrayList<>();
        FakeStages stages = FakeStages.successful(calls);
        stages.reportArtifact = null;
        stages.reportReturnsNull = true;

        PipelineResult result = stages.service().run(request("run-1"));

        assertEquals(List.of("prepare", "extract", "classify", "context", "generate", "release", "report"), calls);
        assertEquals(RunStatus.FAILED, result.status());
        assertTrue(result.errors().stream().anyMatch(error -> error.code().equals("REPORT_ARTIFACT_MISSING")));
        assertNull(result.reportArtifact());
    }

    @Test
    void missingReportArtifactPathIsFatalAndSanitized() {
        List<String> calls = new ArrayList<>();
        FakeStages stages = FakeStages.successful(calls);
        stages.reportArtifact = new ReportArtifact(List.of(
                FakeStages.validReportPath(), Path.of("missing-report.json")));

        PipelineResult result = stages.service().run(request("run-1"));

        assertEquals(RunStatus.FAILED, result.status());
        assertNull(result.reportArtifact());
        assertTrue(result.errors().stream().anyMatch(error -> error.code().equals("REPORT_ARTIFACT_MISSING")));
    }

    @Test
    void emptyReportArtifactPathIsFatal() throws IOException {
        List<String> calls = new ArrayList<>();
        FakeStages stages = FakeStages.successful(calls);
        Path emptyReport = Files.createTempFile("empty-report-", ".json");
        stages.reportArtifact = new ReportArtifact(List.of(emptyReport));

        PipelineResult result = stages.service().run(request("run-1"));

        assertEquals(RunStatus.FAILED, result.status());
        assertNull(result.reportArtifact());
        assertTrue(result.errors().stream().anyMatch(error -> error.code().equals("REPORT_ARTIFACT_MISSING")));
    }

    @Test
    void resolvesAutomaticRunIdBeforeExtraction() {
        List<String> calls = new ArrayList<>();
        FakeStages stages = FakeStages.successful(calls);
        PipelineApplicationService service = new PipelineApplicationService(
                stages.new WorkspaceFake(),
                stages.new ExtractorFake(),
                stages.new ClassifierFake(),
                stages.new ContextFake(),
                stages.new GeneratorFake(),
                stages.new ReportFake(),
                (request, workspace) -> "resolved-run");

        PipelineRequest automaticRequest = request(PipelineApplicationService.AUTOMATIC_RUN_ID);
        automaticRequest = new PipelineRequest(
                automaticRequest.runId(), automaticRequest.repository(), automaticRequest.extraction(),
                automaticRequest.classification(), automaticRequest.context(), automaticRequest.testGeneration(),
                automaticRequest.report());
        PipelineResult result = service.run(automaticRequest);

        assertEquals("resolved-run", result.runId());
        assertEquals("resolved-run", stages.extractionRunId);
    }

    private static PipelineError error(
            String stage, String code, String candidateId, boolean recoverable, String message) {
        return new PipelineError(stage, code, message, candidateId, recoverable);
    }

    private static ClassifiedDebt withErrors(ClassifiedDebt classified, List<PipelineError> errors) {
        return new ClassifiedDebt(
                classified.candidate(),
                classified.satd(),
                classified.debtType(),
                classified.confidence(),
                classified.provenance(),
                classified.status(),
                errors);
    }

    private static EnrichedSatdDebt withErrors(EnrichedSatdDebt enriched, List<PipelineError> errors) {
        return new EnrichedSatdDebt(
                enriched.classifiedDebt(),
                enriched.externalTask(),
                enriched.references(),
                enriched.contextStatus(),
                errors);
    }

    private static void assertStageFailureIsSanitized(
            PipelineResult result, String stage, String code, String secret) {
        PipelineError stageError = result.errors().stream()
                .filter(error -> error.stage().equals(stage) && error.code().equals(code))
                .findFirst()
                .orElseThrow();
        assertFalse(stageError.recoverable());
        assertFalse(stageError.message().contains(secret));
    }

    private static PipelineRequest request(String runId) {
        return new PipelineRequest(
                runId,
                new RepositoryRequest("https://example.test/repository", "main"),
                new io.github.jonasfortes12.core.model.ExtractionOptions(runId),
                new ClassificationOptions(true),
                new ContextRequest(true),
                new TestGenerationOptions("junit", "v1"),
                new ReportOptions(Path.of("output")));
    }

    private static Map<String, PipelineItemResult> itemsById(PipelineResult result) {
        Map<String, PipelineItemResult> items = new HashMap<>();
        for (PipelineItemResult item : result.items()) {
            items.put(item.candidate().candidateId(), item);
        }
        return items;
    }

    private static SatdCandidate candidate(String id, String file, String method, int line) {
        return new SatdCandidate(
                id,
                file,
                method,
                line,
                "TODO: simplify",
                "void " + method + "() {}",
                new SourceProvenance("https://example.test/repository", "main", file));
    }

    private static final class FakeStages {
        private final List<String> calls;
        private final RepositoryWorkspace workspace = new RepositoryWorkspace(Path.of("/tmp/fake-workspace"),
                "https://example.test/repository", "main");
        private final SatdCandidate candidateA = candidate("run-1:src/A.java:10:save", "src/A.java", "save", 10);
        private final SatdCandidate candidateB = candidate("run-1:src/B.java:20:load", "src/B.java", "load", 20);
        private final SatdCandidate candidateC = candidate("run-1:src/C.java:30:delete", "src/C.java", "delete", 30);
        private final SatdCandidate candidateD = candidate("run-1:src/D.java:40:archive", "src/D.java", "archive", 40);
        private final ClassifiedDebt classifiedA;
        private final ClassifiedDebt classifiedB;
        private final ClassifiedDebt classifiedC;
        private final ClassifiedDebt classifiedD;
        private final EnrichedSatdDebt enrichedA;
        private final EnrichedSatdDebt enrichedB;
        private final EnrichedSatdDebt enrichedC;
        private final EnrichedSatdDebt enrichedD;
        private RepositoryWorkspace preparedWorkspace = workspace;
        private ExtractionResult extractionResult = new ExtractionResult(
                List.of(candidateA, candidateB, candidateC), List.of());
        private ClassificationResult classification;
        private ContextEnrichmentResult contextResult;
        private TestGenerationResult generationResult;
        private String generationFailureId;
        private RuntimeException prepareFailure;
        private RuntimeException extractorFailure;
        private RuntimeException classifierFailure;
        private RuntimeException contextFailure;
        private RuntimeException generatorFailure;
        private RuntimeException releaseFailure;
        private RuntimeException reportFailure;
        private boolean generatorReturnsNull;
        private boolean reportReturnsNull;
        private ReportArtifact reportArtifact = new ReportArtifact(List.of(validReportPath()));
        private PipelineResult reportedResult;
        private List<String> contextInputIds = List.of();
        private List<String> generationInputIds = List.of();
        private String extractionRunId;

        private FakeStages(List<String> calls) {
            this.calls = calls;
            classifiedA = classified(candidateA, true, "DESIGN");
            classifiedB = classified(candidateB, false, "NONE");
            classifiedC = classified(candidateC, true, "TEST");
            classifiedD = classified(candidateD, true, "DEFECT");
            enrichedA = enriched(classifiedA, "TASK-A");
            enrichedB = enriched(classifiedB, "TASK-B");
            enrichedC = enriched(classifiedC, "TASK-C");
            enrichedD = enriched(classifiedD, "TASK-D");
            contextResult = new ContextEnrichmentResult(List.of(enrichedA, enrichedC), List.of());
            classification = new ClassificationResult(
                    List.of(classifiedB, classifiedC, classifiedA), List.of());
        }

        private static FakeStages successful(List<String> calls) {
            return new FakeStages(calls);
        }

        private PipelineApplicationService service() {
            return new PipelineApplicationService(
                    new WorkspaceFake(),
                    new ExtractorFake(),
                    new ClassifierFake(),
                    new ContextFake(),
                    new GeneratorFake(),
                    new ReportFake());
        }

        private ClassifiedDebt classified(SatdCandidate candidate, boolean satd, String debtType) {
            return new ClassifiedDebt(
                    candidate,
                    satd,
                    debtType,
                    null,
                    new Provenance("fake", "test", "1"),
                    satd ? ItemStatus.CLASSIFIED : ItemStatus.NOT_SATD,
                    List.of());
        }

        private EnrichedSatdDebt enriched(ClassifiedDebt classified, String key) {
            return new EnrichedSatdDebt(
                    classified,
                    new ExternalTaskSpec("fake", key, "summary", "description", List.of(), List.of(),
                            "https://example.test/" + key),
                    List.of(new ExternalReference(key, "comment")),
                    ContextStatus.MATCHED,
                    List.of());
        }

        private GeneratedTest generated(String candidateId) {
            return new GeneratedTest(
                    candidateId,
                    "test-" + candidateId,
                    "junit",
                    "fake",
                    "model",
                    "v1",
                    ItemStatus.GENERATED,
                    ValidationStatus.NOT_RUN,
                    List.of());
        }

        private GeneratedTest failedGenerated(String candidateId, PipelineError error) {
            return new GeneratedTest(
                    candidateId,
                    null,
                    "junit",
                    "fake",
                    "model",
                    "v1",
                    ItemStatus.GENERATION_FAILED,
                    ValidationStatus.NOT_RUN,
                    List.of(error));
        }

        private GeneratedTest failed(String candidateId) {
            PipelineError error = new PipelineError(
                    "generation", "LLM_GENERATION_FAILED", "generation failed", candidateId, true);
            return new GeneratedTest(
                    candidateId,
                    null,
                    "junit",
                    "fake",
                    "model",
                    "v1",
                    ItemStatus.GENERATION_FAILED,
                    ValidationStatus.NOT_RUN,
                    List.of(error));
        }

        private final class WorkspaceFake implements RepositoryWorkspaceProvider {
            @Override
            public RepositoryWorkspace prepare(RepositoryRequest request) {
                calls.add("prepare");
                if (prepareFailure != null) {
                    throw prepareFailure;
                }
                return preparedWorkspace;
            }

            @Override
            public void release(RepositoryWorkspace workspace) {
                calls.add("release");
                if (releaseFailure != null) {
                    throw releaseFailure;
                }
            }
        }

        private final class ExtractorFake implements SatdExtractor {
            @Override
            public ExtractionResult extract(RepositoryWorkspace workspace,
                    io.github.jonasfortes12.core.model.ExtractionOptions options) {
                calls.add("extract");
                extractionRunId = options.runId();
                if (extractorFailure != null) {
                    throw extractorFailure;
                }
                return extractionResult;
            }
        }

        private final class ClassifierFake implements DebtClassifier {
            @Override
            public ClassificationResult classify(List<SatdCandidate> candidates, ClassificationOptions options) {
                calls.add("classify");
                if (classifierFailure != null) {
                    throw classifierFailure;
                }
                return classification;
            }
        }

        private final class ContextFake implements ContextEnricher {
            @Override
            public ContextEnrichmentResult enrich(List<ClassifiedDebt> debts, ContextRequest request) {
                calls.add("context");
                contextInputIds = debts.stream().map(ClassifiedDebt::candidateId).toList();
                if (contextFailure != null) {
                    throw contextFailure;
                }
                return contextResult;
            }
        }

        private final class GeneratorFake implements TestGenerator {
            @Override
            public TestGenerationResult generate(List<EnrichedSatdDebt> debts, TestGenerationOptions options) {
                calls.add("generate");
                generationInputIds = debts.stream().map(EnrichedSatdDebt::candidateId).toList();
                if (generatorFailure != null) {
                    throw generatorFailure;
                }
                if (generatorReturnsNull) {
                    return null;
                }
                if (generationResult != null) {
                    return generationResult;
                }
                List<GeneratedTest> generated = new ArrayList<>();
                List<EnrichedSatdDebt> reordered = List.of(enrichedC, enrichedA);
                for (EnrichedSatdDebt debt : reordered) {
                    if (debts.stream().anyMatch(input -> input.candidateId().equals(debt.candidateId()))) {
                        generated.add(debt.candidateId().equals(generationFailureId)
                                ? failed(debt.candidateId())
                                : generated(debt.candidateId()));
                    }
                }
                List<PipelineError> errors = generated.stream()
                        .flatMap(test -> test.errors().stream())
                        .toList();
                return new TestGenerationResult(generated, errors);
            }
        }

        private final class ReportFake implements ReportSink {
            @Override
            public ReportArtifact write(PipelineResult result, ReportOptions options) {
                calls.add("report");
                reportedResult = result;
                if (reportFailure != null) {
                    throw reportFailure;
                }
                if (reportReturnsNull) {
                    return null;
                }
                return reportArtifact == null
                        ? new ReportArtifact(List.of(Path.of("report.json")))
                        : reportArtifact;
            }
        }

        private static Path validReportPath() {
            try {
                Path report = Files.createTempFile("pipeline-report-", ".json");
                Files.writeString(report, "report", StandardCharsets.UTF_8);
                return report;
            } catch (IOException exception) {
                throw new AssertionError("could not create fake report", exception);
            }
        }
    }
}
