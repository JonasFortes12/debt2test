package io.github.jonasfortes12.core.result;

import io.github.jonasfortes12.core.model.EnrichedSatdDebt;
import io.github.jonasfortes12.core.model.PipelineError;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public record ContextEnrichmentResult(List<EnrichedSatdDebt> enrichments, List<PipelineError> errors) {

    public ContextEnrichmentResult {
        enrichments = Collections.unmodifiableList(
                new ArrayList<>(Objects.requireNonNull(enrichments, "enrichments must not be null")));
        errors = List.copyOf(Objects.requireNonNull(errors, "errors must not be null"));
    }
}
