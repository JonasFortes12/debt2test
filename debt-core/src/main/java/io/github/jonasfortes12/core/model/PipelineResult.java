package io.github.jonasfortes12.core.model;

import static io.github.jonasfortes12.core.util.Validation.requireText;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public record PipelineResult(
        String runId,
        RunStatus status,
        RepositoryWorkspace workspace,
        List<PipelineItemResult> items,
        List<PipelineError> errors,
        ReportArtifact reportArtifact) {

    public PipelineResult {
        requireText(runId, "runId");
        if (status == null) {
            throw new IllegalArgumentException("status is required");
        }
        items = List.copyOf(Objects.requireNonNull(items, "items must not be null"));
        errors = List.copyOf(Objects.requireNonNull(errors, "errors must not be null"));
    }

    public PipelineResult withReportArtifact(ReportArtifact artifact) {
        return new PipelineResult(runId, status, workspace, items, errors,
                Objects.requireNonNull(artifact, "artifact is required"));
    }

    public PipelineResult withStatus(RunStatus newStatus) {
        return new PipelineResult(runId, newStatus, workspace, items, errors, reportArtifact);
    }

    public PipelineResult withAdditionalError(PipelineError error) {
        if (error == null) {
            throw new IllegalArgumentException("error is required");
        }
        List<PipelineError> updated = new ArrayList<>(errors);
        updated.add(error);
        return new PipelineResult(runId, status, workspace, items, updated, reportArtifact);
    }

}
