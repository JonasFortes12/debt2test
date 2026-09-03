package io.github.jonasfortes12.core.model;

import static io.github.jonasfortes12.core.util.Validation.requireText;

import java.util.List;
import java.util.Objects;

public record GeneratedTest(
        String candidateId,
        String sourceCode,
        String framework,
        String provider,
        String model,
        String promptVersion,
        ItemStatus status,
        ValidationStatus validationStatus,
        List<PipelineError> errors) {

    public GeneratedTest {
        requireText(candidateId, "candidateId");
        requireText(framework, "framework");
        requireText(provider, "provider");
        requireText(model, "model");
        requireText(promptVersion, "promptVersion");
        if (status == null || validationStatus == null) {
            throw new IllegalArgumentException("generated-test statuses are required");
        }
        errors = List.copyOf(Objects.requireNonNull(errors, "errors must not be null"));
        if (status != ItemStatus.GENERATED && status != ItemStatus.GENERATION_FAILED) {
            throw new IllegalArgumentException("generated-test status must be GENERATED or GENERATION_FAILED");
        }
        if (status == ItemStatus.GENERATION_FAILED && validationStatus != ValidationStatus.NOT_RUN) {
            throw new IllegalArgumentException("generation failures must use NOT_RUN validation status");
        }
        if (status == ItemStatus.GENERATION_FAILED && errors.isEmpty()) {
            throw new IllegalArgumentException("generation failures must include an error");
        }
        if (status == ItemStatus.GENERATED
                && errors.stream().anyMatch(error -> "generation".equals(error.stage()))) {
            throw new IllegalArgumentException("generated tests must not include generation-failure errors");
        }
        if (sourceCode == null && status != ItemStatus.GENERATION_FAILED) {
            throw new IllegalArgumentException("sourceCode is required unless generation failed");
        }
        if (sourceCode != null) {
            requireText(sourceCode, "sourceCode");
        }
    }

}
