package io.github.jonasfortes12.orchestrator.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.github.jonasfortes12.core.model.ClassifiedDebt;
import io.github.jonasfortes12.core.model.EnrichedSatdDebt;
import io.github.jonasfortes12.core.model.GeneratedTest;
import io.github.jonasfortes12.core.model.PipelineError;
import io.github.jonasfortes12.core.model.PipelineRequest;
import io.github.jonasfortes12.core.model.PipelineResult;
import io.github.jonasfortes12.core.model.RepositoryWorkspace;
import io.github.jonasfortes12.core.model.RunStatus;
import io.github.jonasfortes12.core.model.SatdCandidate;
import io.github.jonasfortes12.core.port.PipelineRunStore;

class PipelineRunStoreHooksTest {

    /** Records the order of callbacks. Hand-written fake: this codebase uses no mocking library. */
    private static final class RecordingRunStore implements PipelineRunStore {
        private final List<String> calls = new ArrayList<>();
        private final List<String> executionIds = new ArrayList<>();

        @Override
        public void runStarted(String executionId, PipelineRequest request) {
            calls.add("runStarted");
            executionIds.add(executionId);
        }

        @Override
        public void runIdResolved(String executionId, String runId) {
            calls.add("runIdResolved");
            executionIds.add(executionId);
        }

        @Override
        public void workspaceReady(String executionId, RepositoryWorkspace workspace) {
            calls.add("workspaceReady");
            executionIds.add(executionId);
        }

        @Override
        public void candidatesExtracted(String executionId, List<SatdCandidate> candidates) {
            calls.add("candidatesExtracted");
            executionIds.add(executionId);
        }

        @Override
        public void candidatesClassified(String executionId, List<ClassifiedDebt> classifications) {
            calls.add("candidatesClassified");
            executionIds.add(executionId);
        }

        @Override
        public void contextEnriched(String executionId, List<EnrichedSatdDebt> enrichments) {
            calls.add("contextEnriched");
            executionIds.add(executionId);
        }

        @Override
        public void testsGenerated(String executionId, List<GeneratedTest> generatedTests) {
            calls.add("testsGenerated");
            executionIds.add(executionId);
        }

        @Override
        public void runFinished(String executionId, PipelineResult result) {
            calls.add("runFinished");
            executionIds.add(executionId);
        }
    }

    /** Fails on every callback, to prove persistence failures never fail a run. */
    private static final class ExplodingRunStore implements PipelineRunStore {
        @Override
        public void runStarted(String executionId, PipelineRequest request) {
            throw new IllegalStateException("database is down");
        }

        @Override
        public void candidatesExtracted(String executionId, List<SatdCandidate> candidates) {
            throw new IllegalStateException("database is down");
        }

        @Override
        public void runFinished(String executionId, PipelineResult result) {
            throw new IllegalStateException("database is down");
        }
    }

    @Test
    void hooksFireInPipelineOrderWithOneStableExecutionId(@TempDir Path outputDirectory) {
        RecordingRunStore store = new RecordingRunStore();

        PipelineResult result = RunStoreScenario.happyPath(outputDirectory).runWith(store);

        assertEquals(RunStatus.COMPLETED, result.status());
        assertEquals(
                List.of("runStarted", "workspaceReady", "runIdResolved", "candidatesExtracted",
                        "candidatesClassified", "contextEnriched", "testsGenerated", "runFinished"),
                store.calls);
        assertEquals(1, store.executionIds.stream().distinct().count(),
                "every callback must carry the same execution ID");
    }

    @Test
    void storeFailureDegradesToCompletedWithErrorsAndKeepsTheReport(@TempDir Path outputDirectory) {
        PipelineResult result = RunStoreScenario.happyPath(outputDirectory)
                .runWith(new ExplodingRunStore());

        assertEquals(RunStatus.COMPLETED_WITH_ERRORS, result.status());
        assertTrue(result.errors().stream().anyMatch(PipelineRunStoreHooksTest::isPersistenceError),
                "a failing store must surface a recoverable persistence error");
        assertTrue(result.reportArtifact() != null && !result.reportArtifact().paths().isEmpty(),
                "the file report must still be written when the store fails");
        assertEquals(1, result.items().size(), "analysis results must survive a store failure");
    }

    @Test
    void failedWorkspacePreparationStillReportsRunStartedAndRunFinished(@TempDir Path outputDirectory) {
        RecordingRunStore store = new RecordingRunStore();

        PipelineResult result = RunStoreScenario.failingWorkspace(outputDirectory).runWith(store);

        assertEquals(RunStatus.FAILED, result.status());
        assertEquals(List.of("runStarted", "runFinished"), store.calls);
    }

    private static boolean isPersistenceError(PipelineError error) {
        return "persistence".equals(error.stage())
                && "PERSISTENCE_WRITE_FAILED".equals(error.code())
                && error.recoverable();
    }
}
