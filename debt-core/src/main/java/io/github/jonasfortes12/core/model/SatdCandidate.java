package io.github.jonasfortes12.core.model;

import static io.github.jonasfortes12.core.util.Validation.requireText;

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

}
