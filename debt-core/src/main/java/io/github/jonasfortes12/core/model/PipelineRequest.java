package io.github.jonasfortes12.core.model;

import static io.github.jonasfortes12.core.util.Validation.requireText;

public record PipelineRequest(
        String runId,
        RepositoryRequest repository,
        ExtractionOptions extraction,
        ClassificationOptions classification,
        ContextRequest context,
        TestGenerationOptions testGeneration,
        ReportOptions report) {

    public PipelineRequest {
        requireText(runId, "runId");
        if (repository == null || extraction == null || classification == null
                || context == null || testGeneration == null || report == null) {
            throw new IllegalArgumentException("pipeline request fields are required");
        }
        if (!runId.equals(extraction.runId())) {
            throw new IllegalArgumentException("request and extraction run IDs must match");
        }
    }

}
