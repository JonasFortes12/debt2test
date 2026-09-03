package io.github.jonasfortes12.core.model;

import java.util.List;
import java.util.Objects;

public record PipelineItemResult(
        SatdCandidate candidate,
        ClassifiedDebt classification,
        EnrichedSatdDebt enrichment,
        GeneratedTest generatedTest,
        List<PipelineError> errors) {

    public PipelineItemResult {
        if (candidate == null || classification == null) {
            throw new IllegalArgumentException("pipeline item candidate and classification are required");
        }
        String candidateId = candidate.candidateId();
        if (!candidateId.equals(classification.candidateId())) {
            throw new IllegalArgumentException("pipeline item classification candidate ID must match candidate ID");
        }
        if (enrichment != null && !candidateId.equals(enrichment.candidateId())) {
            throw new IllegalArgumentException("pipeline item enrichment candidate ID must match candidate ID");
        }
        if (generatedTest != null && !candidateId.equals(generatedTest.candidateId())) {
            throw new IllegalArgumentException("pipeline item generated-test candidate ID must match candidate ID");
        }
        errors = List.copyOf(Objects.requireNonNull(errors, "errors must not be null"));
    }
}
