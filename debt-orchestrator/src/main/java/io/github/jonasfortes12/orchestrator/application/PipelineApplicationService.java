package io.github.jonasfortes12.orchestrator.application;

import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

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
import io.github.jonasfortes12.core.util.UrlSanitizer;
import io.github.jonasfortes12.orchestrator.util.OrchestrationUtils;

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

        OrchestrationUtils.logPipeline(
                "preparing repository workspace for " + UrlSanitizer.sanitize(request.repository().repositoryUrl()));
        RepositoryWorkspace workspace;
        try {
            workspace = workspaceProvider.prepare(request.repository());
            if (workspace == null) {
                return failedPreparation(request.runId());
            }
        } catch (Exception ignored) {
            return failedPreparation(request.runId());
        }
        OrchestrationUtils.logPipeline("workspace ready at " + workspace.rootDirectory());

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

        OrchestrationUtils.logPipeline("writing pipeline report...");
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
        OrchestrationUtils.logPipeline("extracting SATD candidates...");
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
        List<SatdCandidate> candidates = indexCandidates(extraction.candidates(), state);
        OrchestrationUtils.logPipeline("extracted " + candidates.size() + " SATD candidate(s)");

        OrchestrationUtils.logPipeline("classifying " + candidates.size() + " candidate(s)...");
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
        indexClassifications(classification.classifications(), state);

        List<ClassifiedDebt> satd = state.classificationsById.values().stream()
                .filter(ClassifiedDebt::satd)
                .toList();
        OrchestrationUtils.logPipeline("classification complete: " + satd.size() + " candidate(s) flagged as SATD");

        OrchestrationUtils.logPipeline("enriching context for " + satd.size() + " SATD candidate(s)...");
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
        state.enrichmentsById = indexEnrichments(enrichment.enrichments(), state);
        addMissingContextErrors(state);
        OrchestrationUtils.logPipeline("context enrichment complete for " + state.enrichmentsById.size() + " candidate(s)");

        if (!satd.isEmpty() && state.enrichmentsById.isEmpty()) {
            return;
        }

        state.generationExpectedIds = Collections.unmodifiableSet(
                new LinkedHashSet<>(state.enrichmentsById.keySet()));
        OrchestrationUtils.logPipeline("generating tests for " + state.generationExpectedIds.size() + " candidate(s)...");
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
        state.generatedById = indexGeneratedTests(generation.generatedTests(), state);
        addMissingGenerationErrors(state);
        OrchestrationUtils.logPipeline("test generation complete for " + state.generatedById.size() + " candidate(s)");
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

    private List<SatdCandidate> indexCandidates(List<SatdCandidate> candidates, ExecutionState state) {
        if (candidates == null) {
            state.addError(stageFailure("extraction", "EXTRACTION_CANDIDATES_MISSING"));
            return List.of();
        }
        for (SatdCandidate candidate : candidates) {
            state.candidatesById.put(candidate.candidateId(), candidate);
        }
        return List.copyOf(state.candidatesById.values());
    }

    private void indexClassifications(List<ClassifiedDebt> classifications, ExecutionState state) {
        if (classifications == null) {
            state.addError(stageFailure("classification", "CLASSIFICATION_VALUES_MISSING"));
            return;
        }
        for (ClassifiedDebt classification : classifications) {
            addErrors(state.errors, classification.errors());
            state.classificationsById.put(classification.candidateId(), classification);
        }
    }

    private Map<String, EnrichedSatdDebt> indexEnrichments(List<EnrichedSatdDebt> enrichments, ExecutionState state) {
        if (enrichments == null) {
            state.addError(stageFailure("context", "CONTEXT_ENRICHMENTS_MISSING"));
            return new LinkedHashMap<>();
        }
        Map<String, EnrichedSatdDebt> indexed = new LinkedHashMap<>();
        for (EnrichedSatdDebt enrichment : enrichments) {
            addErrors(state.errors, enrichment.errors());
            indexed.put(enrichment.candidateId(), enrichment);
        }
        return indexed;
    }

    private Map<String, GeneratedTest> indexGeneratedTests(List<GeneratedTest> generatedTests, ExecutionState state) {
        if (generatedTests == null) {
            state.addError(stageFailure("generation", "GENERATION_TESTS_MISSING"));
            return new LinkedHashMap<>();
        }
        Map<String, GeneratedTest> indexed = new LinkedHashMap<>();
        for (GeneratedTest generatedTest : generatedTests) {
            addErrors(state.errors, generatedTest.errors());
            indexed.put(generatedTest.candidateId(), generatedTest);
        }
        return indexed;
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
