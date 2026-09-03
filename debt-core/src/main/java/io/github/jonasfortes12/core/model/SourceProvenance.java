package io.github.jonasfortes12.core.model;

import static io.github.jonasfortes12.core.util.Validation.requireText;

public record SourceProvenance(String repositoryUrl, String revision, String relativeFilePath) {

    public SourceProvenance {
        requireText(repositoryUrl, "repositoryUrl");
        relativeFilePath = PathValidation.requireRelative(relativeFilePath, "relativeFilePath");
    }

}
