package io.github.jonasfortes12.orchestrator.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.github.jonasfortes12.core.error.PipelineException;
import io.github.jonasfortes12.core.model.PipelineRequest;
import io.github.jonasfortes12.core.model.PipelineResult;
import io.github.jonasfortes12.core.model.RunSnapshot;
import io.github.jonasfortes12.core.model.RunStatus;
import io.github.jonasfortes12.core.port.PipelineRunStore;

class PipelineApplicationServiceAllocateExecuteTest {

    @Test
    void executeAfterAllocateProducesTheSameOutcomeAsRun(@TempDir Path outputDirectory) {
        PipelineResult viaRun = RunStoreScenario.happyPath(outputDirectory).runWith(new NoOpTrackingStore());

        RunStoreScenario scenario = RunStoreScenario.happyPath(outputDirectory);
        PipelineApplicationService service = scenario.service(new NoOpTrackingStore());
        String executionId = service.allocate(scenario.request());
        PipelineResult viaAllocateExecute = service.execute(executionId, scenario.request());

        assertEquals(viaRun.status(), viaAllocateExecute.status());
        assertEquals(viaRun.items().size(), viaAllocateExecute.items().size());
        assertEquals(RunStatus.COMPLETED, viaAllocateExecute.status());
    }

    @Test
    void allocateReturnsAnIdThatFindRunCanSeeBeforeExecuteRuns(@TempDir Path outputDirectory) {
        FakeRunStore store = new FakeRunStore();
        RunStoreScenario scenario = RunStoreScenario.happyPath(outputDirectory);
        PipelineApplicationService service = scenario.service(store);

        String executionId = service.allocate(scenario.request());

        Optional<RunSnapshot> snapshot = store.findRun(executionId);
        assertTrue(snapshot.isPresent(),
                "the row must exist as soon as allocate() returns, before execute() is ever called");
        assertEquals(RunStatus.QUEUED, snapshot.orElseThrow().status());
        assertTrue(store.started.isEmpty(), "allocate() must queue the run, not start it");
    }

    @Test
    void executeDequeuesTheRunBeforeTheFirstStage(@TempDir Path outputDirectory) {
        FakeRunStore store = new FakeRunStore();
        RunStoreScenario scenario = RunStoreScenario.happyPath(outputDirectory);
        PipelineApplicationService service = scenario.service(store);

        String executionId = service.allocate(scenario.request());
        service.execute(executionId, scenario.request());

        assertEquals(List.of(executionId), store.dequeued);
        assertEquals(RunStatus.RUNNING, store.findRun(executionId).orElseThrow().status());
    }

    @Test
    void runStillRecordsRunStartedDirectly(@TempDir Path outputDirectory) {
        FakeRunStore store = new FakeRunStore();
        RunStoreScenario.happyPath(outputDirectory).runWith(store);

        assertEquals(1, store.started.size());
        assertTrue(store.queued.isEmpty(), "the CLI path never queues");
    }

    @Test
    void allocateThrowsWhenTheStoreCannotRecordTheStart(@TempDir Path outputDirectory) {
        PipelineRunStore explodingStore = new PipelineRunStore() {
            @Override
            public void runQueued(String executionId, PipelineRequest request) {
                throw new IllegalStateException("database is down");
            }
        };
        RunStoreScenario scenario = RunStoreScenario.happyPath(outputDirectory);
        PipelineApplicationService service = scenario.service(explodingStore);

        assertThrows(PipelineException.class, () -> service.allocate(scenario.request()));
    }

    /**
     * Everything defaults to a no-op; only used where the store's behavior is
     * irrelevant.
     */
    private static final class NoOpTrackingStore implements PipelineRunStore {
    }

    /** Tracks lifecycle calls so findRun can answer before execute() ever runs. */
    private static final class FakeRunStore implements PipelineRunStore {
        private final List<String> started = new ArrayList<>();
        private final List<String> queued = new ArrayList<>();
        private final List<String> dequeued = new ArrayList<>();

        @Override
        public void runStarted(String executionId, PipelineRequest request) {
            started.add(executionId);
        }

        @Override
        public void runQueued(String executionId, PipelineRequest request) {
            queued.add(executionId);
        }

        @Override
        public void runDequeued(String executionId) {
            dequeued.add(executionId);
        }

        @Override
        public Optional<RunSnapshot> findRun(String executionId) {
            RunStatus status;
            if (started.contains(executionId) || dequeued.contains(executionId)) {
                status = RunStatus.RUNNING;
            } else if (queued.contains(executionId)) {
                status = RunStatus.QUEUED;
            } else {
                return Optional.empty();
            }
            return Optional.of(new RunSnapshot(
                    executionId, null, status, Instant.now(), null, 0, 0, 0, List.of()));
        }
    }
}
