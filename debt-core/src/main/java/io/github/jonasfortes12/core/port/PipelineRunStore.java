package io.github.jonasfortes12.core.port;

import java.util.List;
import java.util.Optional;

import io.github.jonasfortes12.core.model.ClassifiedDebt;
import io.github.jonasfortes12.core.model.EnrichedSatdDebt;
import io.github.jonasfortes12.core.model.GeneratedTest;
import io.github.jonasfortes12.core.model.PipelineRequest;
import io.github.jonasfortes12.core.model.PipelineResult;
import io.github.jonasfortes12.core.model.RepositoryWorkspace;
import io.github.jonasfortes12.core.model.RunReport;
import io.github.jonasfortes12.core.model.RunSnapshot;
import io.github.jonasfortes12.core.model.SatdCandidate;

/**
 * Records the progress and outcome of a pipeline run.
 *
 * <p>
 * Runs are identified here by an execution ID rather than the domain run ID,
 * because the
 * domain run ID may still be unresolved when a run starts.
 *
 * <p>
 * Every method is a no-op by default, so adding a stage never breaks an
 * existing
 * implementation and a run without persistence needs no special casing.
 *
 * <p>
 * Once a run reaches a terminal status (see {@code RunStatus#isTerminal}) its
 * record is
 * frozen: implementations ignore every later write for it, so a run cancelled
 * while its stages
 * are still running stays CANCELLED and its report stops changing.
 *
 * <p>
 * Implementations should not throw. The orchestrator guards every call, but a
 * store that
 * fails loudly still degrades an otherwise successful run into one with errors.
 */
public interface PipelineRunStore {

    /** A store that records nothing. */
    PipelineRunStore NO_OP = new PipelineRunStore() {
    };

    /**
     * Called before the workspace is prepared, so failed preparations are still
     * recorded.
     */
    default void runStarted(String executionId, PipelineRequest request) {
    }

    /**
     * Records a run accepted for later execution (status QUEUED, startedAt =
     * submission time).
     * Used instead of {@link #runStarted} when the caller runs the stages
     * asynchronously.
     */
    default void runQueued(String executionId, PipelineRequest request) {
    }

    /**
     * Moves a QUEUED run to RUNNING and sets startedAt to now; a no-op for any
     * other status.
     */
    default void runDequeued(String executionId) {
    }

    /**
     * Called once the domain run ID is known, which may be after workspace
     * preparation.
     */
    default void runIdResolved(String executionId, String runId) {
    }

    default void workspaceReady(String executionId, RepositoryWorkspace workspace) {
    }

    default void candidatesExtracted(String executionId, List<SatdCandidate> candidates) {
    }

    default void candidatesClassified(String executionId, List<ClassifiedDebt> classifications) {
    }

    default void contextEnriched(String executionId, List<EnrichedSatdDebt> enrichments) {
    }

    default void testsGenerated(String executionId, List<GeneratedTest> generatedTests) {
    }

    /**
     * Called exactly once per run, including runs that failed before any stage ran.
     * Ignored for a
     * run that is already terminal (e.g. CANCELLED).
     */
    default void runFinished(String executionId, PipelineResult result) {
    }

    /**
     * Returns the run's current snapshot, or empty if {@code executionId} is
     * unknown.
     */
    default Optional<RunSnapshot> findRun(String executionId) {
        return Optional.empty();
    }

    /**
     * Returns the run's full report, or empty if {@code executionId} is unknown.
     */
    default Optional<RunReport> findReport(String executionId) {
        return Optional.empty();
    }

    /**
     * Marks a non-terminal run CANCELLED. A no-op by default, and expected to be a
     * no-op for a
     * run that has already reached a terminal status (see the implementation in
     * {@code DatabasePipelineRunStore}).
     */
    default void runCancelled(String executionId) {
    }
}
