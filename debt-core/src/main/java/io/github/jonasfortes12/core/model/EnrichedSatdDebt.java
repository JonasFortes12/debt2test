package io.github.jonasfortes12.core.model;

import java.util.List;
import java.util.Objects;

public record EnrichedSatdDebt(
        ClassifiedDebt classifiedDebt,
        ExternalTaskSpec externalTask,
        List<ExternalReference> references,
        ContextStatus contextStatus,
        List<PipelineError> errors) {

    public EnrichedSatdDebt {
        if (classifiedDebt == null || contextStatus == null) {
            throw new IllegalArgumentException("enrichment fields are required");
        }
        references = List.copyOf(Objects.requireNonNull(references, "references must not be null"));
        errors = List.copyOf(Objects.requireNonNull(errors, "errors must not be null"));

        if (contextStatus == ContextStatus.MATCHED) {
            if (externalTask == null) {
                throw new IllegalArgumentException("MATCHED context requires an external task");
            }
            if (references.isEmpty()) {
                throw new IllegalArgumentException("MATCHED context requires a reference");
            }
        }
        if ((contextStatus == ContextStatus.NOT_FOUND
                || contextStatus == ContextStatus.FAILED
                || contextStatus == ContextStatus.SKIPPED)
                && externalTask != null) {
            throw new IllegalArgumentException(contextStatus + " context must not have an external task");
        }
        if (contextStatus == ContextStatus.FAILED && errors.isEmpty()) {
            throw new IllegalArgumentException("FAILED context requires an error");
        }
    }

    public String candidateId() {
        return classifiedDebt.candidateId();
    }
}
