package io.github.jonasfortes12.core.model;

import static io.github.jonasfortes12.core.util.Validation.requireText;

import java.nio.file.Path;

public record RepositoryWorkspace(Path rootDirectory, String repositoryUrl, String revision) {

    public RepositoryWorkspace {
        if (rootDirectory == null) {
            throw new IllegalArgumentException("rootDirectory is required");
        }
        requireText(repositoryUrl, "repositoryUrl");
    }

}
