package io.github.jonasfortes12.core.model;

public record ExternalReference(String value, String source) {

    public ExternalReference {
        requireText(value, "value");
        requireText(source, "source");
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
    }
}
