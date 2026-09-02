package io.github.jonasfortes12.core.model;

public record SourceProvenance(String repositoryUrl, String revision, String relativeFilePath) {

    public SourceProvenance {
        requireText(repositoryUrl, "repositoryUrl");
        relativeFilePath = PathValidation.requireRelative(relativeFilePath, "relativeFilePath");
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
    }
}
