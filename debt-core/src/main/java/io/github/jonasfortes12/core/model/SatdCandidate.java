package io.github.jonasfortes12.core.model;

public record SatdCandidate(
        String candidateId,
        String filePath,
        String methodName,
        int lineNumber,
        String comment,
        String methodSourceCode,
        SourceProvenance sourceProvenance) {

    public SatdCandidate {
        requireText(candidateId, "candidateId");
        filePath = PathValidation.requireRelative(filePath, "filePath");
        requireText(methodName, "methodName");
        requireText(comment, "comment");
        requireText(methodSourceCode, "methodSourceCode");
        if (lineNumber < 1) {
            throw new IllegalArgumentException("lineNumber must be positive");
        }
        if (sourceProvenance == null) {
            throw new IllegalArgumentException("sourceProvenance is required");
        }
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
    }
}
