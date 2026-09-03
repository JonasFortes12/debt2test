package io.github.jonasfortes12.core.result;

import io.github.jonasfortes12.core.model.ClassifiedDebt;
import io.github.jonasfortes12.core.model.PipelineError;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public record ClassificationResult(List<ClassifiedDebt> classifications, List<PipelineError> errors) {

    public ClassificationResult {
        classifications = Collections.unmodifiableList(
                new ArrayList<>(Objects.requireNonNull(classifications, "classifications must not be null")));
        errors = List.copyOf(Objects.requireNonNull(errors, "errors must not be null"));
    }
}
