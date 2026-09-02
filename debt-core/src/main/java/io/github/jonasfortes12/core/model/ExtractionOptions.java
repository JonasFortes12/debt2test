package io.github.jonasfortes12.core.model;

public record ExtractionOptions(String runId) {

    public ExtractionOptions {
        requireText(runId, "runId");
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
    }
}
