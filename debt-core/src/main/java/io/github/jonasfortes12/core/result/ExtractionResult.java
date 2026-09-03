package io.github.jonasfortes12.core.result;

import io.github.jonasfortes12.core.model.PipelineError;
import io.github.jonasfortes12.core.model.SatdCandidate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public record ExtractionResult(List<SatdCandidate> candidates, List<PipelineError> errors) {

    public ExtractionResult {
        candidates = Collections.unmodifiableList(
                new ArrayList<>(Objects.requireNonNull(candidates, "candidates must not be null")));
        errors = List.copyOf(Objects.requireNonNull(errors, "errors must not be null"));
    }
}
