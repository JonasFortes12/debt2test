package io.github.jonasfortes12.core.model;

import java.nio.file.Path;

public record ReportOptions(Path outputDirectory) {

    public ReportOptions {
        if (outputDirectory == null) {
            throw new IllegalArgumentException("outputDirectory is required");
        }
    }
}
