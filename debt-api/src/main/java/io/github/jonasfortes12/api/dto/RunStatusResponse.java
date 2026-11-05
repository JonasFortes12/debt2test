package io.github.jonasfortes12.api.dto;

import java.time.Instant;
import java.util.List;

import io.github.jonasfortes12.core.model.RunSnapshot;
import io.github.jonasfortes12.core.model.RunStatus;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "A run's current state and, once it is terminal, its totals.")
public record RunStatusResponse(
                @Schema(description = "UUID of this run; the identifier every endpoint takes.", example = "3f2b8c1e-9a4d-4e7b-8f6a-2c1d5e9b7a40") String executionId,

                @Schema(description = "Fingerprint of the run's inputs (SHA-256 of repository, revision, models, "
                                + "LLM configuration and prompt version), used to name report artifacts. Two runs of the "
                                + "same inputs share it, so it is informational only and never used in URLs. Null until "
                                + "the repository has been cloned.", nullable = true) String runId,

                @Schema(description = "QUEUED: waiting for the single worker. RUNNING: stages in progress. "
                                + "COMPLETED: finished without errors. COMPLETED_WITH_ERRORS: finished, but some items "
                                + "carry errors. FAILED: the run itself failed (see the report's run-level errors). "
                                + "CANCELLED: cancelled by a client. The last four are terminal and never change.") RunStatus status,

                @Schema(description = "ISO-8601 UTC. While QUEUED, when the run was submitted; afterwards, when "
                                + "the worker started it.", example = "2026-11-03T14:00:05Z") Instant startedAt,

                @Schema(description = "ISO-8601 UTC time the run became terminal; null until then.", nullable = true) Instant finishedAt,

                @Schema(description = "Number of extracted candidates. Set when the run finishes; 0 before that "
                                + "and for CANCELLED runs.") int candidateCount,

                @Schema(description = "Number of candidates classified as SATD. Set when the run finishes; 0 "
                                + "before that and for CANCELLED runs.") int satdCount,

                @Schema(description = "Number of items whose testStatus is GENERATED. Set when the run finishes; "
                                + "0 before that and for CANCELLED runs.") int generatedTestCount,

                @Schema(description = "Paths of the JSON/Markdown report artifacts on "
                                + "the server's filesystem. Not downloadable through this API; use the report endpoint.") List<String> reportPaths) {

        public static RunStatusResponse from(RunSnapshot snapshot) {
                return new RunStatusResponse(
                                snapshot.executionId(), snapshot.runId(), snapshot.status(), snapshot.startedAt(),
                                snapshot.finishedAt(), snapshot.candidateCount(), snapshot.satdCount(),
                                snapshot.generatedTestCount(), snapshot.reportPaths());
        }
}
