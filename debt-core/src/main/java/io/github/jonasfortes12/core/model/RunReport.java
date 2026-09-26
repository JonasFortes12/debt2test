package io.github.jonasfortes12.core.model;

import java.util.List;
import java.util.Objects;

/** A run's summary plus one flat row per extracted candidate. See {@link RunSnapshot}. */
public record RunReport(RunSnapshot summary, List<RunReportItem> items) {

    public RunReport {
        Objects.requireNonNull(summary, "summary must not be null");
        items = List.copyOf(Objects.requireNonNull(items, "items must not be null"));
    }
}
