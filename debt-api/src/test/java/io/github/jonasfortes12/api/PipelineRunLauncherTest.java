package io.github.jonasfortes12.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.github.jonasfortes12.core.model.ClassificationOptions;
import io.github.jonasfortes12.core.model.ContextRequest;
import io.github.jonasfortes12.core.model.ExtractionOptions;
import io.github.jonasfortes12.core.model.PipelineRequest;
import io.github.jonasfortes12.core.model.ReportArtifact;
import io.github.jonasfortes12.core.model.ReportOptions;
import io.github.jonasfortes12.core.model.RepositoryRequest;
import io.github.jonasfortes12.core.model.RepositoryWorkspace;
import io.github.jonasfortes12.core.model.TestGenerationOptions;
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
import io.github.jonasfortes12.orchestrator.application.PipelineApplicationService;

class PipelineRunLauncherTest {

    @TempDir
    private Path tempDir;

    private static PipelineRequest request() {
        return new PipelineRequest(
                PipelineApplicationService.AUTOMATIC_RUN_ID,
                new RepositoryRequest("https://example.test/repo", "main"),
                new ExtractionOptions(PipelineApplicationService.AUTOMATIC_RUN_ID),
                new ClassificationOptions(true),
                new ContextRequest(true),
                new TestGenerationOptions("JUnit 5"),
                new ReportOptions(Path.of("output")));
    }

    private static final class RecordingRunStore implements PipelineRunStore {
        private final List<String> startedIds = new CopyOnWriteArrayList<>();
        private final List<String> cancelledIds = new CopyOnWriteArrayList<>();

        @Override
        public void runStarted(String executionId, PipelineRequest request) {
            startedIds.add(executionId);
        }

        @Override
        public void runCancelled(String executionId) {
            cancelledIds.add(executionId);
        }
    }

    /** A service whose workspace preparation blocks until the test releases it, to observe async submit(). */
    private PipelineApplicationService blockingService(
            PipelineRunStore store, CountDownLatch startedLatch, CountDownLatch releaseLatch) {
        RepositoryWorkspaceProvider workspaceProvider = new RepositoryWorkspaceProvider() {
            @Override
            public RepositoryWorkspace prepare(RepositoryRequest repositoryRequest) {
                startedLatch.countDown();
                try {
                    releaseLatch.await(5, TimeUnit.SECONDS);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                }
                return new RepositoryWorkspace(Path.of("/tmp/ws"), repositoryRequest.repositoryUrl(), "abc123");
            }

            @Override
            public void release(RepositoryWorkspace workspace) {
            }
        };
        SatdExtractor extractor = (workspace, options) -> new ExtractionResult(List.of(), List.of());
        DebtClassifier classifier = (candidates, options) -> new ClassificationResult(List.of(), List.of());
        ContextEnricher enricher = (debts, contextRequest) -> new ContextEnrichmentResult(List.of(), List.of());
        TestGenerator generator = (debts, options) -> new TestGenerationResult(List.of(), List.of());
        return new PipelineApplicationService(
                workspaceProvider, extractor, classifier, enricher, generator,
                (result, options) -> {
                    try {
                        Path report = Files.createTempFile(tempDir, "launcher-report-", ".json");
                        Files.writeString(report, "{}", StandardCharsets.UTF_8);
                        return new ReportArtifact(List.of(report));
                    } catch (IOException exception) {
                        throw new RuntimeException(exception);
                    }
                },
                (req, workspace) -> req.runId().equals(PipelineApplicationService.AUTOMATIC_RUN_ID)
                        ? "resolved-run" : req.runId(),
                store);
    }

    @Test
    void submitAllocatesSynchronouslyAndRunsTheRestInTheBackground() throws InterruptedException {
        RecordingRunStore store = new RecordingRunStore();
        CountDownLatch startedLatch = new CountDownLatch(1);
        CountDownLatch releaseLatch = new CountDownLatch(1);
        PipelineRunLauncher launcher = new PipelineRunLauncher(
                blockingService(store, startedLatch, releaseLatch), store);
        try {
            String executionId = launcher.submit(request());

            assertEquals(List.of(executionId), store.startedIds,
                    "allocate() must have recorded the run before submit() returns");
            assertTrue(startedLatch.await(2, TimeUnit.SECONDS),
                    "execute() must run on the background thread without blocking submit()");
            releaseLatch.countDown();
        } finally {
            launcher.shutdown();
        }
    }

    @Test
    void cancelMarksAKnownRunCancelledAndReturnsAccepted() throws InterruptedException {
        RecordingRunStore store = new RecordingRunStore();
        CountDownLatch startedLatch = new CountDownLatch(1);
        CountDownLatch releaseLatch = new CountDownLatch(1);
        PipelineRunLauncher launcher = new PipelineRunLauncher(
                blockingService(store, startedLatch, releaseLatch), store);
        try {
            String executionId = launcher.submit(request());
            assertTrue(startedLatch.await(2, TimeUnit.SECONDS));

            PipelineRunLauncher.CancelOutcome outcome = launcher.cancel(executionId);

            assertEquals(PipelineRunLauncher.CancelOutcome.ACCEPTED, outcome);
            assertEquals(List.of(executionId), store.cancelledIds);
            releaseLatch.countDown();
        } finally {
            launcher.shutdown();
        }
    }

    @Test
    void cancelReturnsUnknownForAnUnrecognizedExecutionIdButStillMarksItCancelled() {
        RecordingRunStore store = new RecordingRunStore();
        PipelineRunLauncher launcher = new PipelineRunLauncher(
                blockingService(store, new CountDownLatch(1), new CountDownLatch(1)), store);
        try {
            assertEquals(PipelineRunLauncher.CancelOutcome.UNKNOWN, launcher.cancel("unknown-id"));
            assertEquals(List.of("unknown-id"), store.cancelledIds,
                    "the store write must happen even when this process never submitted the run "
                            + "(e.g. after a restart), since the controller already confirmed the run exists");
        } finally {
            launcher.shutdown();
        }
    }
}
