package io.github.jonasfortes12.orchestrator.application;

import io.github.jonasfortes12.core.error.PipelineException;
import io.github.jonasfortes12.core.model.ClassifiedDebt;
import io.github.jonasfortes12.core.model.ContextStatus;
import io.github.jonasfortes12.core.model.EnrichedSatdDebt;
import io.github.jonasfortes12.core.model.ExtractionOptions;
import io.github.jonasfortes12.core.model.GeneratedTest;
import io.github.jonasfortes12.core.model.ItemStatus;
import io.github.jonasfortes12.core.model.PipelineError;
import io.github.jonasfortes12.core.model.PipelineItemResult;
import io.github.jonasfortes12.core.model.PipelineRequest;
import io.github.jonasfortes12.core.model.PipelineResult;
import io.github.jonasfortes12.core.model.Provenance;
import io.github.jonasfortes12.core.model.ReportArtifact;
import io.github.jonasfortes12.core.model.RepositoryWorkspace;
import io.github.jonasfortes12.core.model.RunStatus;
import io.github.jonasfortes12.core.model.SatdCandidate;
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

import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Locale;
import java.util.stream.Collectors;

public final class PipelineApplicationService {

    private final RepositoryWorkspaceProvider workspaceProvider;
    private final SatdExtractor extractor;
    private final DebtClassifier classifier;
    private final ContextEnricher contextEnricher;
    private final TestGenerator testGenerator;
    private final ReportSink reportSink;
    private final RunIdResolver runIdResolver;

    public static final String AUTOMATIC_RUN_ID = "auto";

    public PipelineApplicationService(
            RepositoryWorkspaceProvider workspaceProvider,
            SatdExtractor extractor,
            DebtClassifier classifier,
            ContextEnricher contextEnricher,
            TestGenerator testGenerator,
            ReportSink reportSink) {
        this(workspaceProvider, extractor, classifier, contextEnricher, testGenerator, reportSink,
                (request, workspace) -> request.runId());
    }

    public PipelineApplicationService(
            RepositoryWorkspaceProvider workspaceProvider,
            SatdExtractor extractor,
            DebtClassifier classifier,
            ContextEnricher contextEnricher,
            TestGenerator testGenerator,
            ReportSink reportSink,
            RunIdResolver runIdResolver) {
        this.workspaceProvider = Objects.requireNonNull(workspaceProvider, "workspaceProvider must not be null");
        this.extractor = Objects.requireNonNull(extractor, "extractor must not be null");
        this.classifier = Objects.requireNonNull(classifier, "classifier must not be null");
        this.contextEnricher = Objects.requireNonNull(contextEnricher, "contextEnricher must not be null");
        this.testGenerator = Objects.requireNonNull(testGenerator, "testGenerator must not be null");
        this.reportSink = Objects.requireNonNull(reportSink, "reportSink must not be null");
        this.runIdResolver = Objects.requireNonNull(runIdResolver, "runIdResolver must not be null");
    }

    public PipelineResult run(PipelineRequest request) {
        Objects.requireNonNull(request, "request must not be null");

        RepositoryWorkspace workspace;
        try {
            workspace = workspaceProvider.prepare(request.repository());
            if (workspace == null) {
                return failedPreparation(request.runId());
            }
        } catch (Exception ignored) {
            return failedPreparation(request.runId());
        }

        PipelineRequest executionRequest = request;
        PipelineError runIdError = null;
        if (AUTOMATIC_RUN_ID.equals(request.runId())) {
            try {
                executionRequest = withRunId(request, runIdResolver.resolve(request, workspace));
            } catch (Exception ignored) {
                runIdError = stageFailure("orchestration", "RUN_ID_RESOLUTION_FAILED");
            }
        }

        ExecutionState state = new ExecutionState();
        if (runIdError != null) {
            state.addError(runIdError);
        }
        PipelineResult assembled;
        PipelineError releaseError;
        try {
            executeStages(executionRequest, workspace, state);
            assembled = assemble(executionRequest.runId(), workspace, state);
        } finally {
            releaseError = releaseFailure(workspace);
        }

        if (releaseError != null) {
            assembled = assembled.withAdditionalError(releaseError);
            assembled = assembled.withStatus(statusFor(assembled.errors()));
        }

        try {
            ReportArtifact artifact = reportSink.write(assembled, executionRequest.report());
            if (!validArtifact(artifact)) {
                return assembled.withStatus(RunStatus.FAILED)
                        .withAdditionalError(new PipelineError(
                                "report",
                                "REPORT_ARTIFACT_MISSING",
                                "pipeline report did not produce an artifact",
                                null,
                                false));
            }
            return assembled.withReportArtifact(artifact);
        } catch (PipelineException failure) {
            if ("REPORT_ROLLBACK_FAILED".equals(failure.error().code())) {
                return assembled.withStatus(RunStatus.FAILED)
                        .withAdditionalError(new PipelineError(
                                "report",
                                "REPORT_ROLLBACK_FAILED",
                                "pipeline report rollback could not be completed",
                                null,
                                false));
            }
            return assembled.withStatus(RunStatus.FAILED)
                    .withAdditionalError(new PipelineError(
                            "report",
                            "REPORT_WRITE_FAILED",
                            "pipeline report could not be written",
                            null,
                            false));
        } catch (Exception ignored) {
            return assembled.withStatus(RunStatus.FAILED)
                    .withAdditionalError(new PipelineError(
                            "report",
                            "REPORT_WRITE_FAILED",
                            "pipeline report could not be written",
                            null,
                            false));
        }
    }

    private static boolean validArtifact(ReportArtifact artifact) {
        if (artifact == null || artifact.paths() == null || artifact.paths().isEmpty()) {
            return false;
        }
        for (java.nio.file.Path path : artifact.paths()) {
            try {
                if (path == null || !Files.exists(path) || !Files.isRegularFile(path) || Files.size(path) == 0) {
                    return false;
                }
            } catch (IOException | RuntimeException ignored) {
                return false;
            }
        }
        return true;
    }

    private static PipelineRequest withRunId(PipelineRequest request, String runId) {
        if (runId == null || runId.isBlank()) {
            throw new IllegalArgumentException("resolved run ID must not be blank");
        }
        return new PipelineRequest(
                runId,
                request.repository(),
                new ExtractionOptions(runId),
                request.classification(),
                request.context(),
                request.testGeneration(),
                request.report());
    }

    private void executeStages(PipelineRequest request, RepositoryWorkspace workspace, ExecutionState state) {
        ExtractionResult extraction;
        try {
            extraction = extractor.extract(workspace, request.extraction());
        } catch (Exception ignored) {
            state.addError(stageFailure("extraction", "EXTRACTION_STAGE_FAILED"));
            return;
        }
        if (extraction == null) {
            state.addError(stageFailure("extraction", "EXTRACTION_RESULT_MISSING"));
            return;
        }
        state.addStageErrors("extraction", extraction.errors());
        List<SatdCandidate> candidates = validateCandidates(extraction.candidates(), state);

        ClassificationResult classification;
        try {
            classification = classifier.classify(candidates, request.classification());
        } catch (Exception ignored) {
            state.addError(stageFailure("classification", "CLASSIFICATION_STAGE_FAILED"));
            return;
        }
        if (classification == null) {
            state.addError(stageFailure("classification", "CLASSIFICATION_RESULT_MISSING"));
            return;
        }
        state.addStageErrors("classification", classification.errors());
        validateClassifications(classification.classifications(), state);

        List<ClassifiedDebt> satd = state.classificationsById.values().stream()
                .filter(ClassifiedDebt::satd)
                .toList();

        ContextEnrichmentResult enrichment;
        try {
            enrichment = contextEnricher.enrich(satd, request.context());
        } catch (Exception ignored) {
            PipelineError failure = stageFailure("context", "CONTEXT_STAGE_FAILED");
            state.addError(failure);
            state.enrichmentsById = failedContextFor(satd, failure);
            return;
        }
        if (enrichment == null) {
            PipelineError failure = stageFailure("context", "CONTEXT_RESULT_MISSING");
            state.addError(failure);
            state.enrichmentsById = failedContextFor(satd, failure);
            return;
        }
        state.addStageErrors("context", enrichment.errors());
        state.enrichmentsById = validateEnrichments(enrichment.enrichments(), state);
        addMissingContextErrors(state);

        if (!satd.isEmpty() && state.enrichmentsById.isEmpty()) {
            return;
        }

        state.generationExpectedIds = Collections.unmodifiableSet(
                new LinkedHashSet<>(state.enrichmentsById.keySet()));
        TestGenerationResult generation;
        try {
            generation = testGenerator.generate(
                    List.copyOf(state.enrichmentsById.values()), request.testGeneration());
        } catch (Exception ignored) {
            state.addError(stageFailure("generation", "GENERATION_STAGE_FAILED"));
            addMissingGenerationErrors(state);
            return;
        }
        if (generation == null) {
            state.addError(stageFailure("generation", "GENERATION_RESULT_MISSING"));
            addMissingGenerationErrors(state);
            return;
        }
        state.addStageErrors("generation", generation.errors());
        state.generatedById = validateGeneratedTests(generation.generatedTests(), state);
        addMissingGenerationErrors(state);
    }

    private PipelineResult assemble(String runId, RepositoryWorkspace workspace, ExecutionState state) {
        List<PipelineItemResult> items = new ArrayList<>(state.candidatesById.size());
        for (Map.Entry<String, SatdCandidate> entry : state.candidatesById.entrySet()) {
            String candidateId = entry.getKey();
            SatdCandidate candidate = entry.getValue();
            ClassifiedDebt classification = state.classificationsById.get(candidateId);
            List<PipelineError> itemErrors = errorsForCandidate(state.errors, candidateId);

            if (classification == null) {
                PipelineError missingClassification = correlationError(
                        "classification",
                        "CLASSIFICATION_RESULT_MISSING",
                        candidateId,
                        "classification did not return a value for the candidate");
                state.addError(missingClassification);
                addError(itemErrors, missingClassification);
                classification = missingClassification(candidate, missingClassification);
            }

            EnrichedSatdDebt enrichment = null;
            GeneratedTest generatedTest = null;
            if (classification.satd()) {
                enrichment = state.enrichmentsById.get(candidateId);
                if (enrichment != null) {
                    addErrors(itemErrors, enrichment.errors());
                    generatedTest = state.generatedById.get(candidateId);
                    if (generatedTest != null) {
                        addErrors(itemErrors, generatedTest.errors());
                    }
                }
            }

            addErrors(itemErrors, classification.errors());
            items.add(new PipelineItemResult(candidate, classification, enrichment, generatedTest, itemErrors));
        }

        return new PipelineResult(runId, statusFor(state.errors), workspace, items, state.errors, null);
    }

    private List<SatdCandidate> validateCandidates(
            List<SatdCandidate> candidates, ExecutionState state) {
        if (candidates == null) {
            state.addError(stageFailure("extraction", "EXTRACTION_CANDIDATES_MISSING"));
            return List.of();
        }

        for (SatdCandidate candidate : candidates) {
            if (candidate == null) {
                state.addError(correlationError(
                        "extraction",
                        "EXTRACTION_NULL_RESULT_ENTRY",
                        null,
                        "extraction returned a null candidate"));
                continue;
            }
            if (state.candidatesById.putIfAbsent(candidate.candidateId(), candidate) != null) {
                state.addError(correlationError(
                        "extraction",
                        "DUPLICATE_CANDIDATE_ID",
                        candidate.candidateId(),
                        "duplicate candidate ID was returned"));
            }
        }
        return List.copyOf(state.candidatesById.values());
    }

    private void validateClassifications(
            List<ClassifiedDebt> classifications, ExecutionState state) {
        if (classifications == null) {
            state.addError(stageFailure("classification", "CLASSIFICATION_VALUES_MISSING"));
            return;
        }

        Set<String> seenIds = new LinkedHashSet<>();
        for (ClassifiedDebt classification : classifications) {
            if (classification == null) {
                state.addError(correlationError(
                        "classification",
                        "CLASSIFICATION_NULL_RESULT_ENTRY",
                        null,
                        "classification returned a null result"));
                continue;
            }
            String candidateId = classification.candidateId();
            addErrors(state.errors, classification.errors());
            if (!seenIds.add(candidateId)) {
                state.addError(correlationError(
                        "classification",
                        "DUPLICATE_CLASSIFICATION_ID",
                        candidateId,
                        "duplicate classification ID was returned"));
                continue;
            }
            if (!state.candidatesById.containsKey(candidateId)) {
                state.addError(correlationError(
                        "classification",
                        "CLASSIFICATION_UNKNOWN_CANDIDATE",
                        candidateId,
                        "classification returned a result for an unknown candidate"));
                continue;
            }
            state.classificationsById.put(candidateId, classification);
        }
    }

    private Map<String, EnrichedSatdDebt> validateEnrichments(
            List<EnrichedSatdDebt> enrichments, ExecutionState state) {
        Map<String, EnrichedSatdDebt> valid = new LinkedHashMap<>();
        if (enrichments == null) {
            state.addError(stageFailure("context", "CONTEXT_ENRICHMENTS_MISSING"));
            return valid;
        }

        Set<String> seenIds = new LinkedHashSet<>();
        for (EnrichedSatdDebt enrichment : enrichments) {
            if (enrichment == null) {
                state.addError(correlationError(
                        "context",
                        "CONTEXT_NULL_RESULT_ENTRY",
                        null,
                        "context returned a null result"));
                continue;
            }
            String candidateId = enrichment.candidateId();
            addErrors(state.errors, enrichment.errors());
            if (!seenIds.add(candidateId)) {
                state.addError(correlationError(
                        "context",
                        "DUPLICATE_CONTEXT_RESULT_ID",
                        candidateId,
                        "duplicate context result ID was returned"));
                continue;
            }
            if (!state.classificationsById.containsKey(candidateId)) {
                state.addError(correlationError(
                        "context",
                        "CONTEXT_UNKNOWN_CANDIDATE",
                        candidateId,
                        "context returned a result for an unknown candidate"));
                continue;
            }
            if (!state.classificationsById.get(candidateId).satd()) {
                state.addError(correlationError(
                        "context",
                        "CONTEXT_NON_SATD_RESULT",
                        candidateId,
                        "context returned a result for a non-SATD candidate"));
                continue;
            }
            valid.put(candidateId, enrichment);
        }
        return valid;
    }

    private Map<String, GeneratedTest> validateGeneratedTests(
            List<GeneratedTest> generatedTests, ExecutionState state) {
        Map<String, GeneratedTest> valid = new LinkedHashMap<>();
        if (generatedTests == null) {
            state.addError(stageFailure("generation", "GENERATION_TESTS_MISSING"));
            return valid;
        }

        Set<String> seenIds = new LinkedHashSet<>();
        for (GeneratedTest generatedTest : generatedTests) {
            if (generatedTest == null) {
                state.addError(correlationError(
                        "generation",
                        "GENERATION_NULL_RESULT_ENTRY",
                        null,
                        "generation returned a null result"));
                continue;
            }
            String candidateId = generatedTest.candidateId();
            addErrors(state.errors, generatedTest.errors());
            if (!seenIds.add(candidateId)) {
                state.addError(correlationError(
                        "generation",
                        "DUPLICATE_GENERATION_RESULT_ID",
                        candidateId,
                        "duplicate generation result ID was returned"));
                continue;
            }
            ClassifiedDebt classification = state.classificationsById.get(candidateId);
            if (classification == null) {
                state.addError(correlationError(
                        "generation",
                        "GENERATION_UNKNOWN_CANDIDATE",
                        candidateId,
                        "generation returned a result for an unknown candidate"));
                continue;
            }
            if (!classification.satd()) {
                state.addError(correlationError(
                        "generation",
                        "GENERATION_NON_SATD_RESULT",
                        candidateId,
                        "generation returned a result for a non-SATD candidate"));
                continue;
            }
            if (!state.generationExpectedIds.contains(candidateId)) {
                state.addError(correlationError(
                        "generation",
                        "GENERATION_WITHOUT_CONTEXT",
                        candidateId,
                        "generation returned a result without context"));
                continue;
            }
            valid.put(candidateId, generatedTest);
        }
        return valid;
    }

    private void addMissingContextErrors(ExecutionState state) {
        for (String candidateId : state.classificationsById.keySet()) {
            if (state.classificationsById.get(candidateId).satd()
                    && !state.enrichmentsById.containsKey(candidateId)) {
                state.addError(correlationError(
                        "context",
                        "CONTEXT_RESULT_MISSING",
                        candidateId,
                        "context did not return a value for the SATD candidate"));
            }
        }
    }

    private void addMissingGenerationErrors(ExecutionState state) {
        for (String candidateId : state.generationExpectedIds) {
            if (!state.generatedById.containsKey(candidateId)) {
                state.addError(correlationError(
                        "generation",
                        "GENERATION_RESULT_MISSING",
                        candidateId,
                        "generation did not return a value for the SATD candidate"));
            }
        }
    }

    private static Map<String, EnrichedSatdDebt> failedContextFor(
            List<ClassifiedDebt> satd, PipelineError failure) {
        Map<String, EnrichedSatdDebt> failed = new LinkedHashMap<>();
        for (ClassifiedDebt classification : satd) {
            failed.put(classification.candidateId(), new EnrichedSatdDebt(
                    classification,
                    null,
                    List.of(),
                    ContextStatus.FAILED,
                    List.of(failure)));
        }
        return failed;
    }

    private static ClassifiedDebt missingClassification(SatdCandidate candidate, PipelineError error) {
        return new ClassifiedDebt(
                candidate,
                false,
                "UNCLASSIFIED",
                null,
                new Provenance("classifier", "unavailable", "unavailable"),
                ItemStatus.SKIPPED,
                List.of(error));
    }

    private static List<PipelineError> errorsForCandidate(List<PipelineError> errors, String candidateId) {
        return errors.stream()
                .filter(error -> candidateId.equals(error.candidateId()))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private static PipelineError stageFailure(String stage, String code) {
        return new PipelineError(stage, code, stage + " stage failed", null, false);
    }

    private static PipelineError correlationError(
            String stage, String code, String candidateId, String message) {
        return new PipelineError(stage, code, message, candidateId, true);
    }

    private static void addErrors(List<PipelineError> target, List<PipelineError> additions) {
        if (additions == null) {
            return;
        }
        for (PipelineError error : additions) {
            if (!target.contains(error)) {
                target.add(error);
            }
        }
    }

    private static void addError(List<PipelineError> target, PipelineError error) {
        if (!target.contains(error)) {
            target.add(error);
        }
    }

    private static PipelineError releaseFailure(RepositoryWorkspaceProvider provider, RepositoryWorkspace workspace) {
        try {
            provider.release(workspace);
            return null;
        } catch (Exception ignored) {
            return new PipelineError(
                    "repository",
                    "WORKSPACE_RELEASE_FAILED",
                    "repository workspace could not be released",
                    null,
                    false);
        }
    }

    private PipelineError releaseFailure(RepositoryWorkspace workspace) {
        return releaseFailure(workspaceProvider, workspace);
    }

    private static RunStatus statusFor(List<PipelineError> errors) {
        if (errors.stream().anyMatch(error -> !error.recoverable())) {
            return RunStatus.FAILED;
        }
        return errors.stream().anyMatch(PipelineError::recoverable)
                ? RunStatus.COMPLETED_WITH_ERRORS
                : RunStatus.COMPLETED;
    }

    private static PipelineResult failedPreparation(String runId) {
        return new PipelineResult(
                runId,
                RunStatus.FAILED,
                null,
                List.of(),
                List.of(new PipelineError(
                        "repository",
                        "WORKSPACE_PREPARATION_FAILED",
                        "repository workspace could not be prepared",
                        null,
                        false)),
                null);
    }

    @FunctionalInterface
    public interface RunIdResolver {
        String resolve(PipelineRequest request, RepositoryWorkspace workspace);
    }

    private static final class ExecutionState {
        private final Map<String, SatdCandidate> candidatesById = new LinkedHashMap<>();
        private final Map<String, ClassifiedDebt> classificationsById = new LinkedHashMap<>();
        private Map<String, EnrichedSatdDebt> enrichmentsById = Map.of();
        private Map<String, GeneratedTest> generatedById = Map.of();
        private Set<String> generationExpectedIds = Set.of();
        private final List<PipelineError> errors = new ArrayList<>();

        private void addStageErrors(String stage, List<PipelineError> errors) {
            if (errors == null) {
                addError(stageFailure(
                        stage,
                        stage.toUpperCase(Locale.ROOT) + "_ERRORS_MISSING"));
                return;
            }
            for (PipelineError error : errors) {
                addError(error == null
                        ? correlationError(stage, stage.toUpperCase(Locale.ROOT) + "_NULL_ERROR_ENTRY", null,
                                stage + " returned a null error")
                        : error);
            }
        }

        private void addError(PipelineError error) {
            if (!errors.contains(error)) {
                errors.add(error);
            }
        }
    }
}
