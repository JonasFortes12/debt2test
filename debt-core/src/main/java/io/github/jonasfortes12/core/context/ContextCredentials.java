package io.github.jonasfortes12.core.context;

import java.util.Optional;

/**
 * Credentials for a context source (for Jira Cloud: account email and API token).
 *
 * <p>Runtime configuration, not a domain record: never persisted, never reported, and
 * {@link #toString()} redacts both values so a stray log line cannot leak them.
 */
public record ContextCredentials(String principal, String secret) {

    public ContextCredentials {
        if (isBlank(principal) || isBlank(secret)) {
            throw new IllegalArgumentException("principal and secret must not be blank");
        }
    }

    /**
     * Builds credentials from two optional inputs. Both blank means "no credentials" (anonymous
     * access); exactly one blank is a caller mistake.
     */
    public static Optional<ContextCredentials> optionalOf(String principal, String secret) {
        boolean principalMissing = isBlank(principal);
        boolean secretMissing = isBlank(secret);
        if (principalMissing && secretMissing) {
            return Optional.empty();
        }
        if (principalMissing || secretMissing) {
            throw new IllegalArgumentException("principal and secret must be provided together");
        }
        return Optional.of(new ContextCredentials(principal.trim(), secret.trim()));
    }

    @Override
    public String toString() {
        return "ContextCredentials[principal=***, secret=***]";
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
