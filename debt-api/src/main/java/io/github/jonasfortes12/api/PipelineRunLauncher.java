package io.github.jonasfortes12.api;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import io.github.jonasfortes12.core.model.PipelineRequest;
import io.github.jonasfortes12.core.port.PipelineRunStore;
import io.github.jonasfortes12.orchestrator.application.PipelineApplicationService;
import io.github.jonasfortes12.orchestrator.util.OrchestrationUtils;

/**
 * Runs pipeline executions on a single background thread so the HTTP layer never blocks on one.
 *
 * <p>One worker is the minimum that unblocks the caller: nothing about this deployment has more
 * than one operator submitting runs, so a pool, a queue-depth config, or backpressure headers
 * would all be unused generality.
 */
public final class PipelineRunLauncher {

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Map<String, Future<?>> inFlight = new ConcurrentHashMap<>();
    private final PipelineApplicationService application;
    private final PipelineRunStore runStore;

    public PipelineRunLauncher(PipelineApplicationService application, PipelineRunStore runStore) {
        this.application = application;
        this.runStore = runStore;
    }

    /** Allocates and records the run synchronously, then runs its stages on the background thread. */
    public String submit(PipelineRequest request) {
        String executionId = application.allocate(request);
        Future<?> future = executor.submit(() -> runExecute(executionId, request));
        inFlight.put(executionId, future);
        return executionId;
    }

    /**
     * {@code execute()} already catches and records the stage failures it knows about; this only
     * guards against something truly unexpected escaping it (e.g. a bug deep inside assemble() or
     * the report sink), so the run's row doesn't stay RUNNING forever with zero trace of why.
     */
    private void runExecute(String executionId, PipelineRequest request) {
        try {
            application.execute(executionId, request);
        } catch (RuntimeException unexpected) {
            OrchestrationUtils.logPipeline(
                    "execution " + executionId + " failed unexpectedly: " + unexpected.getMessage());
        }
    }

    /**
     * Best-effort cancellation: interrupts the worker thread if it is still running this
     * execution, and unconditionally marks the row CANCELLED regardless of whether the interrupt
     * lands or this process even knows about the execution (e.g. after a restart). Nothing in the
     * extractor/classifier/context/tester chain checks for interruption today, so the underlying
     * work may keep running to completion afterward.
     */
    public CancelOutcome cancel(String executionId) {
        Future<?> future = inFlight.get(executionId);
        if (future != null) {
            future.cancel(true);
        }
        runStore.runCancelled(executionId);
        return future == null ? CancelOutcome.UNKNOWN : CancelOutcome.ACCEPTED;
    }

    void shutdown() {
        executor.shutdownNow();
    }

    public enum CancelOutcome {
        ACCEPTED,
        UNKNOWN
    }
}
