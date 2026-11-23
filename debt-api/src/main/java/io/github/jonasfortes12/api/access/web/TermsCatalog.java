package io.github.jonasfortes12.api.access.web;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

/**
 * The current terms of use, read once from {@code classpath:terms/terms-<version>.md}. Terms are
 * versioned files in git, so what each participant accepted can always be shown exactly.
 */
public record TermsCatalog(String version, String text) {

    public static TermsCatalog load(String version) {
        String resource = "terms/terms-" + version + ".md";
        try (InputStream in = TermsCatalog.class.getClassLoader().getResourceAsStream(resource)) {
            if (in == null) {
                throw new IllegalStateException(
                        "terms file " + resource + " is missing for DEBT_ACCESS_TERMS_VERSION=" + version);
            }
            return new TermsCatalog(version, new String(in.readAllBytes(), StandardCharsets.UTF_8));
        } catch (IOException failure) {
            throw new UncheckedIOException(failure);
        }
    }
}
