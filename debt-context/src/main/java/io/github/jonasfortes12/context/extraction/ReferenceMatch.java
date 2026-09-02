package io.github.jonasfortes12.context.extraction;

public record ReferenceMatch(String value, int offset) {

    public ReferenceMatch {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("reference value must not be blank");
        }
        if (offset < 0) {
            throw new IllegalArgumentException("reference offset must not be negative");
        }
    }
}
