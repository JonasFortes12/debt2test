package io.github.jonasfortes12.core.model;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * A flat, read-only snapshot of one run's row in the run store.
 *
 * <p>Unvalidated by design: unlike {@link PipelineResult} and its nested records, this projects
 * a database row that may be partial (a run still in progress) or historical, so it carries none
 * of the compact-constructor invariants tuned for in-process assembly.
 */
public record RunSnapshot(
        String executionId,
        String runId,
        RunStatus status,
        Instant startedAt,
        Instant finishedAt,
        int candidateCount,
        int satdCount,
        int generatedTestCount,
        List<String> reportPaths) {

    public RunSnapshot {
        reportPaths = List.copyOf(Objects.requireNonNull(reportPaths, "reportPaths must not be null"));
    }
}
