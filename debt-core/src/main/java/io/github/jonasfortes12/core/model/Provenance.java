package io.github.jonasfortes12.core.model;

public record Provenance(String provider, String strategy, String version) {

    public Provenance {
        requireText(provider, "provider");
        requireText(strategy, "strategy");
        requireText(version, "version");
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
    }
}
