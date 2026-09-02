package io.github.jonasfortes12.core.model;

import java.util.List;
import java.util.Objects;

public record ClassifiedDebt(
        SatdCandidate candidate,
        boolean satd,
        String debtType,
        Double confidence,
        Provenance provenance,
        ItemStatus status,
        List<PipelineError> errors) {

    public ClassifiedDebt {
        if (candidate == null || provenance == null || status == null) {
            throw new IllegalArgumentException("classification object fields are required");
        }
        requireText(debtType, "debtType");
        errors = List.copyOf(Objects.requireNonNull(errors, "errors must not be null"));
        if (satd && status != ItemStatus.CLASSIFIED) {
            throw new IllegalArgumentException("satd classifications must use CLASSIFIED status");
        }
        if (!satd && status != ItemStatus.NOT_SATD
                && !(status == ItemStatus.SKIPPED
                && "UNCLASSIFIED".equals(debtType)
                && "unavailable".equals(provenance.strategy())
                && !errors.isEmpty())) {
            throw new IllegalArgumentException(
                    "non-SATD classifications must use NOT_SATD or unavailable UNCLASSIFIED SKIPPED status");
        }
        if (confidence != null && (!Double.isFinite(confidence) || confidence < 0 || confidence > 1)) {
            throw new IllegalArgumentException("confidence must be finite and between 0 and 1 inclusive");
        }
    }

    public String candidateId() {
        return candidate.candidateId();
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
    }
}
