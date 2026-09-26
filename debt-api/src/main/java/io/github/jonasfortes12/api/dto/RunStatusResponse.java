package io.github.jonasfortes12.api.dto;

import java.time.Instant;
import java.util.List;

import io.github.jonasfortes12.core.model.RunSnapshot;
import io.github.jonasfortes12.core.model.RunStatus;

public record RunStatusResponse(
        String executionId,
        String runId,
        RunStatus status,
        Instant startedAt,
        Instant finishedAt,
        int candidateCount,
        int satdCount,
        int generatedTestCount,
        List<String> reportPaths) {

    public static RunStatusResponse from(RunSnapshot snapshot) {
        return new RunStatusResponse(
                snapshot.executionId(), snapshot.runId(), snapshot.status(), snapshot.startedAt(),
                snapshot.finishedAt(), snapshot.candidateCount(), snapshot.satdCount(),
                snapshot.generatedTestCount(), snapshot.reportPaths());
    }
}
