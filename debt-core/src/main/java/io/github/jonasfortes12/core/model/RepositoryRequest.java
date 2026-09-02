package io.github.jonasfortes12.core.model;

public record RepositoryRequest(String repositoryUrl, String revision) {

    public RepositoryRequest {
        requireText(repositoryUrl, "repositoryUrl");
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
    }
}
