package io.github.jonasfortes12.core.model;

import static io.github.jonasfortes12.core.util.Validation.requireText;

public record ExtractionOptions(String runId) {

    public ExtractionOptions {
        requireText(runId, "runId");
    }

}
