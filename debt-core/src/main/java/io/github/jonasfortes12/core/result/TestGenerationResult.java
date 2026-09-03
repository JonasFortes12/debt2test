package io.github.jonasfortes12.core.result;

import io.github.jonasfortes12.core.model.GeneratedTest;
import io.github.jonasfortes12.core.model.PipelineError;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public record TestGenerationResult(List<GeneratedTest> generatedTests, List<PipelineError> errors) {

    public TestGenerationResult {
        generatedTests = Collections.unmodifiableList(
                new ArrayList<>(Objects.requireNonNull(generatedTests, "generatedTests must not be null")));
        errors = List.copyOf(Objects.requireNonNull(errors, "errors must not be null"));
    }
}
