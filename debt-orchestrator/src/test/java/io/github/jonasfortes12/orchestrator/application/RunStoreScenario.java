package io.github.jonasfortes12.orchestrator.application;

import java.nio.file.Path;
import java.util.List;

import io.github.jonasfortes12.core.model.ClassificationOptions;
import io.github.jonasfortes12.core.model.ClassifiedDebt;
import io.github.jonasfortes12.core.model.ContextRequest;
import io.github.jonasfortes12.core.model.ContextStatus;
import io.github.jonasfortes12.core.model.EnrichedSatdDebt;
import io.github.jonasfortes12.core.model.ExtractionOptions;
import io.github.jonasfortes12.core.model.GeneratedTest;
import io.github.jonasfortes12.core.model.ItemStatus;
import io.github.jonasfortes12.core.model.PipelineRequest;
import io.github.jonasfortes12.core.model.PipelineResult;
import io.github.jonasfortes12.core.model.Provenance;
import io.github.jonasfortes12.core.model.ReportOptions;
import io.github.jonasfortes12.core.model.RepositoryRequest;
import io.github.jonasfortes12.core.model.RepositoryWorkspace;
import io.github.jonasfortes12.core.model.SatdCandidate;
import io.github.jonasfortes12.core.model.SourceProvenance;
import io.github.jonasfortes12.core.model.TestGenerationOptions;
import io.github.jonasfortes12.core.model.ValidationStatus;
import io.github.jonasfortes12.core.port.ContextEnricher;
import io.github.jonasfortes12.core.port.DebtClassifier;
import io.github.jonasfortes12.core.port.PipelineRunStore;
import io.github.jonasfortes12.core.port.RepositoryWorkspaceProvider;
import io.github.jonasfortes12.core.port.SatdExtractor;
import io.github.jonasfortes12.core.port.TestGenerator;
import io.github.jonasfortes12.core.result.ClassificationResult;
import io.github.jonasfortes12.core.result.ContextEnrichmentResult;
import io.github.jonasfortes12.core.result.ExtractionResult;
import io.github.jonasfortes12.core.result.TestGenerationResult;
import io.github.jonasfortes12.orchestrator.reporting.FileReportSink;

/** Builds a one-candidate pipeline whose stages all succeed, for run-store tests. */
final class RunStoreScenario {

    private static final String REPO = "https://example.com/repo.git";
    private static final String CANDIDATE_ID = "candidate-1";

    private final Path outputDirectory;
    private final boolean workspaceFails;

    private RunStoreScenario(Path outputDirectory, boolean workspaceFails) {
        this.outputDirectory = outputDirectory;
        this.workspaceFails = workspaceFails;
    }

    static RunStoreScenario happyPath(Path outputDirectory) {
        return new RunStoreScenario(outputDirectory, false);
    }

    static RunStoreScenario failingWorkspace(Path outputDirectory) {
        return new RunStoreScenario(outputDirectory, true);
    }

    PipelineResult runWith(PipelineRunStore store) {
        PipelineApplicationService service = new PipelineApplicationService(
                workspaceProvider(),
                extractor(),
                classifier(),
                enricher(),
                generator(),
                new FileReportSink(),
                (request, workspace) -> request.runId(),
                store);
        return service.run(request());
    }

    private PipelineRequest request() {
        return new PipelineRequest(
                "run-1",
                new RepositoryRequest(REPO, "main"),
                new ExtractionOptions("run-1"),
                new ClassificationOptions(true),
                new ContextRequest(true),
                new TestGenerationOptions("junit5", "v1"),
                new ReportOptions(outputDirectory));
    }

    private RepositoryWorkspaceProvider workspaceProvider() {
        return new RepositoryWorkspaceProvider() {
            @Override
            public RepositoryWorkspace prepare(RepositoryRequest repositoryRequest) {
                if (workspaceFails) {
                    throw new IllegalStateException("clone failed");
                }
                return new RepositoryWorkspace(outputDirectory, REPO, "abc123");
            }

            @Override
            public void release(RepositoryWorkspace workspace) {
            }
        };
    }

    private static SatdCandidate candidate() {
        return new SatdCandidate(
                CANDIDATE_ID,
                "src/main/java/Example.java",
                "doWork",
                12,
                "// TODO fix this",
                "void doWork() { }",
                new SourceProvenance(REPO, "abc123", "src/main/java/Example.java"));
    }

    private static ClassifiedDebt classified() {
        return new ClassifiedDebt(
                candidate(),
                true,
                "DESIGN",
                0.91,
                new Provenance("weka", "debthunter", "1.0"),
                ItemStatus.CLASSIFIED,
                List.of());
    }

    private static EnrichedSatdDebt enriched() {
        return new EnrichedSatdDebt(
                classified(), null, List.of(), ContextStatus.NOT_FOUND, List.of());
    }

    private static GeneratedTest generatedTest() {
        return new GeneratedTest(
                CANDIDATE_ID,
                "class ExampleTest { }",
                "junit5",
                "openai",
                "gpt-test",
                "v1",
                ItemStatus.GENERATED,
                ValidationStatus.NOT_RUN,
                List.of());
    }

    private static SatdExtractor extractor() {
        return (workspace, options) -> new ExtractionResult(List.of(candidate()), List.of());
    }

    private static DebtClassifier classifier() {
        return (candidates, options) -> new ClassificationResult(List.of(classified()), List.of());
    }

    private static ContextEnricher enricher() {
        return (debts, contextRequest) -> new ContextEnrichmentResult(List.of(enriched()), List.of());
    }

    private static TestGenerator generator() {
        return (debts, options) -> new TestGenerationResult(List.of(generatedTest()), List.of());
    }
}
