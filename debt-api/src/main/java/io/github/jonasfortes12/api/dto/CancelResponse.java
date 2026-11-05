package io.github.jonasfortes12.api.dto;

import io.github.jonasfortes12.api.PipelineRunLauncher;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Result of a cancel request. In both outcomes the run is now CANCELLED.")
public record CancelResponse(
                @Schema(description = "The run that was cancelled.", example = "3f2b8c1e-9a4d-4e7b-8f6a-2c1d5e9b7a40") String executionId,

                @Schema(description = "ACCEPTED: this server had the run queued or running and interrupted it. "
                                + "UNKNOWN: this server has no job for the run (e.g. it was restarted); the run is "
                                + "still marked CANCELLED.") PipelineRunLauncher.CancelOutcome outcome) {
}
