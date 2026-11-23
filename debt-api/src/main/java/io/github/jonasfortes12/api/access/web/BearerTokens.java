package io.github.jonasfortes12.api.access.web;

import java.util.Locale;

/** Reads the token out of an {@code Authorization: Bearer <token>} header. */
public final class BearerTokens {

    private static final String SCHEME = "bearer ";

    private BearerTokens() {
    }

    /** @return the token, or null when the header is missing or not a Bearer header */
    public static String from(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.toLowerCase(Locale.ROOT).startsWith(SCHEME)) {
            return null;
        }
        return authorizationHeader.substring(SCHEME.length()).trim();
    }
}
