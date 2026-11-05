package io.github.jonasfortes12.core.model;

import java.util.List;
import java.util.Objects;

/**
 * A run's summary, one flat row per extracted candidate, and the run-level
 * errors that belong to
 * no candidate (e.g. a workspace that could not be prepared). See
 * {@link RunSnapshot}.
 */
public record RunReport(RunSnapshot summary, List<RunReportItem> items, List<PipelineError> errors) {

    public RunReport {
        Objects.requireNonNull(summary, "summary must not be null");
        items = List.copyOf(Objects.requireNonNull(items, "items must not be null"));
        errors = List.copyOf(Objects.requireNonNull(errors, "errors must not be null"));
    }
}
