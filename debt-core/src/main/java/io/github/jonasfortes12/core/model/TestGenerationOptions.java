package io.github.jonasfortes12.core.model;

public record TestGenerationOptions(String framework, String promptVersion) {

    public TestGenerationOptions {
        requireText(framework, "framework");
        requireText(promptVersion, "promptVersion");
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
    }
}
