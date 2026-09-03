package io.github.jonasfortes12.core.model;

import static io.github.jonasfortes12.core.util.Validation.requireText;

public record PipelineError(
        String stage,
        String code,
        String message,
        String candidateId,
        boolean recoverable) {

    public PipelineError {
        requireText(stage, "stage");
        requireText(code, "code");
        requireText(message, "message");
        if (candidateId != null && candidateId.isBlank()) {
            throw new IllegalArgumentException("candidateId must not be blank when provided");
        }
    }

}
