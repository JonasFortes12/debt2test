package io.github.jonasfortes12.persistence.adapter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import io.github.jonasfortes12.core.model.ClassificationOptions;
import io.github.jonasfortes12.core.model.ContextRequest;
import io.github.jonasfortes12.core.model.ExtractionOptions;
import io.github.jonasfortes12.core.model.GeneratedTest;
import io.github.jonasfortes12.core.model.ItemStatus;
import io.github.jonasfortes12.core.model.PipelineRequest;
import io.github.jonasfortes12.core.model.PipelineResult;
import io.github.jonasfortes12.core.model.ReportOptions;
import io.github.jonasfortes12.core.model.RepositoryRequest;
import io.github.jonasfortes12.core.model.RunStatus;
import io.github.jonasfortes12.core.model.SatdCandidate;
import io.github.jonasfortes12.core.model.SourceProvenance;
import io.github.jonasfortes12.core.model.TestGenerationOptions;
import io.github.jonasfortes12.core.model.ValidationStatus;
import io.github.jonasfortes12.persistence.entity.PipelineRunEntity;
import io.github.jonasfortes12.persistence.repository.DebtContextRepository;
import io.github.jonasfortes12.persistence.repository.GeneratedTestRepository;
import io.github.jonasfortes12.persistence.repository.PipelineErrorRepository;
import io.github.jonasfortes12.persistence.repository.PipelineRunRepository;
import io.github.jonasfortes12.persistence.repository.TechnicalDebtRepository;

class DatabasePipelineRunStoreLifecycleTest {

    private static final Instant CANCELLED_AT = Instant.parse("2026-11-03T10:00:00Z");

    private final PipelineRunRepository runs = mock(PipelineRunRepository.class);
    private final TechnicalDebtRepository debts = mock(TechnicalDebtRepository.class);
    private final DebtContextRepository contexts = mock(DebtContextRepository.class);
    private final GeneratedTestRepository tests = mock(GeneratedTestRepository.class);
    private final PipelineErrorRepository errors = mock(PipelineErrorRepository.class);
    private final DatabasePipelineRunStore store = new DatabasePipelineRunStore(runs, debts, contexts, tests, errors);

    private PipelineRunEntity storedRun(UUID id, RunStatus status) {
        PipelineRunEntity run = new PipelineRunEntity();
        run.setId(id);
        run.setStatus(status);
        run.setStartedAt(Instant.parse("2026-11-03T09:00:00Z"));
        if (status.isTerminal()) {
            run.setFinishedAt(CANCELLED_AT);
        }
        when(runs.findById(id)).thenReturn(Optional.of(run));
        when(runs.findByIdForUpdate(id)).thenReturn(Optional.of(run));
        when(debts.findByRun_Id(any(), any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));
        return run;
    }

    private static PipelineResult completedResult() {
        return new PipelineResult("run-1", RunStatus.COMPLETED, null, List.of(), List.of(), null);
    }

    private static SatdCandidate candidate() {
        return new SatdCandidate("candidate-1", "src/A.java", "save", 10, "// TODO fix", "void save() { }",
                new SourceProvenance("https://example.test/repo", "abc123", "src/A.java"));
    }

    private static GeneratedTest generatedTest() {
        return new GeneratedTest("candidate-1", "class ATest { }", "JUnit 5", "openai", "gpt-test", "v1",
                ItemStatus.GENERATED, ValidationStatus.NOT_RUN, List.of());
    }

    @Test
    void runFinishedDoesNotOverwriteACancelledRun() {
        UUID id = UUID.randomUUID();
        PipelineRunEntity run = storedRun(id, RunStatus.CANCELLED);

        store.runFinished(id.toString(), completedResult());

        assertEquals(RunStatus.CANCELLED, run.getStatus());
        assertEquals(CANCELLED_AT, run.getFinishedAt());
        verify(errors, never()).deleteByRun_Id(any());
        verify(errors, never()).saveAll(any());
    }

    @Test
    void stageHooksIgnoreACancelledRun() {
        UUID id = UUID.randomUUID();
        storedRun(id, RunStatus.CANCELLED);

        store.candidatesExtracted(id.toString(), List.of(candidate()));
        store.testsGenerated(id.toString(), List.of(generatedTest()));

        verify(debts, never()).saveAll(any());
        verify(tests, never()).save(any());
    }

    @Test
    void runFinishedStillRecordsARunningRun() {
        UUID id = UUID.randomUUID();
        PipelineRunEntity run = storedRun(id, RunStatus.RUNNING);

        store.runFinished(id.toString(), completedResult());

        assertEquals(RunStatus.COMPLETED, run.getStatus());
        assertNotNull(run.getFinishedAt());
    }

    @Test
    void runQueuedSavesAQueuedRow() {
        UUID id = UUID.randomUUID();
        PipelineRequest request = new PipelineRequest("auto",
                new RepositoryRequest("https://example.test/repo", null),
                new ExtractionOptions("auto"),
                new ClassificationOptions(false),
                new ContextRequest(false),
                new TestGenerationOptions(TestGenerationOptions.DEFAULT_FRAMEWORK),
                new ReportOptions(Path.of("output")));

        store.runQueued(id.toString(), request);

        ArgumentCaptor<PipelineRunEntity> saved = ArgumentCaptor.forClass(PipelineRunEntity.class);
        verify(runs).save(saved.capture());
        assertEquals(RunStatus.QUEUED, saved.getValue().getStatus());
        assertNotNull(saved.getValue().getStartedAt());
    }

    @Test
    void runDequeuedMovesQueuedToRunningAndResetsStartedAt() {
        UUID id = UUID.randomUUID();
        PipelineRunEntity run = storedRun(id, RunStatus.QUEUED);
        Instant queuedAt = run.getStartedAt();

        store.runDequeued(id.toString());

        assertEquals(RunStatus.RUNNING, run.getStatus());
        assertTrue(run.getStartedAt().isAfter(queuedAt));
    }

    @Test
    void runDequeuedLeavesACancelledRunCancelled() {
        UUID id = UUID.randomUUID();
        PipelineRunEntity run = storedRun(id, RunStatus.CANCELLED);

        store.runDequeued(id.toString());

        assertEquals(RunStatus.CANCELLED, run.getStatus());
    }

    /*
     * The writers below must read the row under a write lock: otherwise a cancel
     * committed between
     * their read and their write is overwritten (Hibernate rewrites every column,
     * status included).
     * Only the locking finder is stubbed, so a plain findById read leaves the run
     * untouched.
     */

    @Test
    void runFinishedReadsTheRunUnderAWriteLock() {
        UUID id = UUID.randomUUID();
        PipelineRunEntity run = lockedOnlyRun(id, RunStatus.RUNNING);

        store.runFinished(id.toString(), completedResult());

        assertEquals(RunStatus.COMPLETED, run.getStatus());
    }

    @Test
    void runCancelledReadsTheRunUnderAWriteLock() {
        UUID id = UUID.randomUUID();
        PipelineRunEntity run = lockedOnlyRun(id, RunStatus.RUNNING);

        store.runCancelled(id.toString());

        assertEquals(RunStatus.CANCELLED, run.getStatus());
    }

    @Test
    void runDequeuedReadsTheRunUnderAWriteLock() {
        UUID id = UUID.randomUUID();
        PipelineRunEntity run = lockedOnlyRun(id, RunStatus.QUEUED);

        store.runDequeued(id.toString());

        assertEquals(RunStatus.RUNNING, run.getStatus());
    }

    private PipelineRunEntity lockedOnlyRun(UUID id, RunStatus status) {
        PipelineRunEntity run = new PipelineRunEntity();
        run.setId(id);
        run.setStatus(status);
        run.setStartedAt(Instant.parse("2026-11-03T09:00:00Z"));
        when(runs.findById(id)).thenReturn(Optional.empty());
        when(runs.findByIdForUpdate(id)).thenReturn(Optional.of(run));
        when(debts.findByRun_Id(any(), any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));
        return run;
    }
}
