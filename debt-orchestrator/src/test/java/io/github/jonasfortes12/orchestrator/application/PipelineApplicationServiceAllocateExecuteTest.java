package io.github.jonasfortes12.orchestrator.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

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
        assertEquals(RunStatus.RUNNING, snapshot.orElseThrow().status());
    }

    @Test
    void allocateThrowsWhenTheStoreCannotRecordTheStart(@TempDir Path outputDirectory) {
        PipelineRunStore explodingStore = new PipelineRunStore() {
            @Override
            public void runStarted(String executionId, PipelineRequest request) {
                throw new IllegalStateException("database is down");
            }
        };
        RunStoreScenario scenario = RunStoreScenario.happyPath(outputDirectory);
        PipelineApplicationService service = scenario.service(explodingStore);

        assertThrows(PipelineException.class, () -> service.allocate(scenario.request()));
    }

    /** Everything defaults to a no-op; only used where the store's behavior is irrelevant. */
    private static final class NoOpTrackingStore implements PipelineRunStore {
    }

    /** Tracks started execution IDs so findRun can answer before execute() ever runs. */
    private static final class FakeRunStore implements PipelineRunStore {
        private final Set<String> started = new HashSet<>();

        @Override
        public void runStarted(String executionId, PipelineRequest request) {
            started.add(executionId);
        }

        @Override
        public Optional<RunSnapshot> findRun(String executionId) {
            if (!started.contains(executionId)) {
                return Optional.empty();
            }
            return Optional.of(new RunSnapshot(
                    executionId, null, RunStatus.RUNNING, Instant.now(), null, 0, 0, 0, List.of()));
        }
    }
}
