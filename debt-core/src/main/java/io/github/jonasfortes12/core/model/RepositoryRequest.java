package io.github.jonasfortes12.core.model;

import static io.github.jonasfortes12.core.util.Validation.requireText;

public record RepositoryRequest(String repositoryUrl, String revision) {

    public RepositoryRequest {
        requireText(repositoryUrl, "repositoryUrl");
    }

}
