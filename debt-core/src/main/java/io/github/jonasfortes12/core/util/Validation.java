package io.github.jonasfortes12.core.util;

public final class Validation {

    private Validation() {
    }

    public static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
    }
}
