package io.github.jonasfortes12.core.model;

import java.nio.file.Path;

public record RepositoryWorkspace(Path rootDirectory, String repositoryUrl, String revision) {

    public RepositoryWorkspace {
        if (rootDirectory == null) {
            throw new IllegalArgumentException("rootDirectory is required");
        }
        requireText(repositoryUrl, "repositoryUrl");
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
    }
}
