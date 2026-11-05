package io.github.jonasfortes12.api.dto;

import io.github.jonasfortes12.core.model.PipelineError;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "One failure recorded during a run. Branch on code; show message to people.")
public record PipelineErrorResponse(
                @Schema(description = "Pipeline stage that failed: repository, extraction, classification, "
                                + "context, generation, report, persistence or orchestration.", example = "generation") String stage,

                @Schema(description = "Stable identifier of the failure, e.g. WORKSPACE_PREPARATION_FAILED, "
                                + "CLASSIFIER_MODEL_UNAVAILABLE, LLM_HTTP_UNAUTHORIZED, LLM_HTTP_RATE_LIMITED, "
                                + "LLM_RESPONSE_INVALID.", example = "LLM_HTTP_RATE_LIMITED") String code,

                @Schema(description = "Human-readable description, safe to display.", example = "LLM test generation failed.") String message,

                @Schema(description = "Candidate the error belongs to; null for a failure of the run as a whole.", nullable = true) String candidateId,

                @Schema(description = "Whether retrying the run could succeed (e.g. a rate limit), as opposed "
                                + "to a failure that will repeat (e.g. an invalid API key).") boolean recoverable) {

        public static PipelineErrorResponse from(PipelineError error) {
                return new PipelineErrorResponse(
                                error.stage(), error.code(), error.message(), error.candidateId(), error.recoverable());
        }
}
