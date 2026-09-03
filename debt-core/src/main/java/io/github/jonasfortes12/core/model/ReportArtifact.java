package io.github.jonasfortes12.core.model;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

public record ReportArtifact(List<Path> paths) {

    public ReportArtifact {
        paths = List.copyOf(Objects.requireNonNull(paths, "paths must not be null"));
        if (paths.isEmpty()) {
            throw new IllegalArgumentException("paths must not be empty");
        }
    }
}
