package io.github.jonasfortes12.core.model;

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

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
    }
}
