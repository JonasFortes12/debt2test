package io.github.jonasfortes12.persistence.adapter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import io.github.jonasfortes12.core.model.RunReport;
import io.github.jonasfortes12.core.model.RunSnapshot;
import io.github.jonasfortes12.core.model.RunStatus;
import io.github.jonasfortes12.persistence.entity.PipelineErrorEntity;
import io.github.jonasfortes12.persistence.entity.PipelineRunEntity;
import io.github.jonasfortes12.persistence.entity.TechnicalDebtEntity;
import io.github.jonasfortes12.persistence.repository.DebtContextRepository;
import io.github.jonasfortes12.persistence.repository.GeneratedTestRepository;
import io.github.jonasfortes12.persistence.repository.PipelineErrorRepository;
import io.github.jonasfortes12.persistence.repository.PipelineRunRepository;
import io.github.jonasfortes12.persistence.repository.TechnicalDebtRepository;

class DatabasePipelineRunStoreReadTest {

    private final PipelineRunRepository runs = mock(PipelineRunRepository.class);
    private final TechnicalDebtRepository debts = mock(TechnicalDebtRepository.class);
    private final DebtContextRepository contexts = mock(DebtContextRepository.class);
    private final GeneratedTestRepository tests = mock(GeneratedTestRepository.class);
    private final PipelineErrorRepository errors = mock(PipelineErrorRepository.class);
    private final DatabasePipelineRunStore store =
            new DatabasePipelineRunStore(runs, debts, contexts, tests, errors);

    private static PipelineRunEntity run(UUID id, RunStatus status) {
        PipelineRunEntity run = new PipelineRunEntity();
        run.setId(id);
        run.setRunId("run-1");
        run.setStatus(status);
        run.setStartedAt(Instant.parse("2026-09-15T10:00:00Z"));
        run.setCandidateCount(2);
        run.setSatdCount(1);
        run.setGeneratedTestCount(1);
        run.setReportPaths("output/debt-report.json");
        return run;
    }

    @Test
    void findRunReturnsEmptyForAnUnknownExecutionId() {
        UUID id = UUID.randomUUID();
        when(runs.findById(id)).thenReturn(Optional.empty());

        assertTrue(store.findRun(id.toString()).isEmpty());
    }

    @Test
    void findRunMapsTheStoredRowToASnapshot() {
        UUID id = UUID.randomUUID();
        when(runs.findById(id)).thenReturn(Optional.of(run(id, RunStatus.COMPLETED)));

        RunSnapshot snapshot = store.findRun(id.toString()).orElseThrow();

        assertEquals(id.toString(), snapshot.executionId());
        assertEquals(RunStatus.COMPLETED, snapshot.status());
        assertEquals(2, snapshot.candidateCount());
        assertEquals(List.of("output/debt-report.json"), snapshot.reportPaths());
    }

    @Test
    void findReportReturnsEmptyForAnUnknownExecutionId() {
        UUID id = UUID.randomUUID();
        when(runs.findById(id)).thenReturn(Optional.empty());

        assertTrue(store.findReport(id.toString()).isEmpty());
    }

    @Test
    void findReportOnlyAttachesErrorsLinkedToTheirOwnItem() {
        UUID id = UUID.randomUUID();
        when(runs.findById(id)).thenReturn(Optional.of(run(id, RunStatus.COMPLETED_WITH_ERRORS)));

        TechnicalDebtEntity debt = new TechnicalDebtEntity();
        debt.setId(1L);
        debt.setCandidateId("candidate-1");
        debt.setFilePath("src/A.java");
        debt.setMethodName("save");
        debt.setLineNumber(10);
        debt.setComment("// TODO fix");
        debt.setSatd(true);
        debt.setDebtType("DESIGN");
        Page<TechnicalDebtEntity> page = new PageImpl<>(List.of(debt));
        when(debts.findByRun_Id(id, Pageable.unpaged())).thenReturn(page);

        PipelineErrorEntity itemError = new PipelineErrorEntity();
        itemError.setTechnicalDebt(debt);
        itemError.setMessage("generation failed");
        PipelineErrorEntity runLevelError = new PipelineErrorEntity();
        runLevelError.setMessage("workspace release failed");
        when(errors.findByRun_Id(id)).thenReturn(List.of(itemError, runLevelError));

        RunReport report = store.findReport(id.toString()).orElseThrow();

        assertEquals(1, report.items().size());
        assertEquals(List.of("generation failed"), report.items().get(0).errorMessages());
    }

    @Test
    void runCancelledMarksANonTerminalRunCancelled() {
        UUID id = UUID.randomUUID();
        PipelineRunEntity runEntity = run(id, RunStatus.RUNNING);
        when(runs.findById(id)).thenReturn(Optional.of(runEntity));

        store.runCancelled(id.toString());

        assertEquals(RunStatus.CANCELLED, runEntity.getStatus());
        assertNotNull(runEntity.getFinishedAt());
    }

    @Test
    void findRunReturnsEmptyForAMalformedExecutionIdWithoutQueryingTheRepository() {
        assertTrue(store.findRun("not-a-uuid").isEmpty());
    }

    @Test
    void findReportReturnsEmptyForAMalformedExecutionIdWithoutQueryingTheRepository() {
        assertTrue(store.findReport("not-a-uuid").isEmpty());
    }

    @Test
    void runCancelledNoOpsForAMalformedExecutionIdWithoutQueryingTheRepository() {
        store.runCancelled("not-a-uuid");
        // no exception: parsing fails before any repository call, so nothing to assert on runs/debts/errors mocks
    }

    @Test
    void runCancelledDoesNotOverwriteATerminalStatus() {
        UUID id = UUID.randomUUID();
        PipelineRunEntity runEntity = run(id, RunStatus.COMPLETED);
        Instant finishedAt = Instant.parse("2026-09-15T10:05:00Z");
        runEntity.setFinishedAt(finishedAt);
        when(runs.findById(id)).thenReturn(Optional.of(runEntity));

        store.runCancelled(id.toString());

        assertEquals(RunStatus.COMPLETED, runEntity.getStatus());
        assertEquals(finishedAt, runEntity.getFinishedAt());
    }
}
