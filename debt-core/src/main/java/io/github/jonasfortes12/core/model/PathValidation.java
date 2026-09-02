package io.github.jonasfortes12.core.model;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;

final class PathValidation {

    private PathValidation() {
    }

    static String requireRelative(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }

        String slashSeparated = value.replace('\\', '/');
        if (slashSeparated.startsWith("/") || hasWindowsDrivePrefix(slashSeparated)) {
            throw new IllegalArgumentException(field + " must be repository-relative");
        }

        try {
            Path normalized = Path.of(slashSeparated).normalize();
            Path parent = Path.of("..");
            if (normalized.toString().isEmpty() || normalized.equals(Path.of("."))) {
                throw new IllegalArgumentException(field + " must identify a repository-relative file");
            }
            if (normalized.equals(parent) || normalized.startsWith(parent)) {
                throw new IllegalArgumentException(field + " must not escape the repository");
            }
            return normalized.toString().replace('\\', '/');
        } catch (InvalidPathException exception) {
            throw new IllegalArgumentException(field + " must be a valid path", exception);
        }
    }

    private static boolean hasWindowsDrivePrefix(String value) {
        return value.length() >= 2
                && Character.isLetter(value.charAt(0))
                && value.charAt(1) == ':';
    }
}
