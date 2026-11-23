package io.github.jonasfortes12.core.context;

import java.util.Objects;

/**
 * Result of checking, before a run starts, whether a {@link ContextSource} can be read.
 *
 * <p>{@link #message()} is written by the provider adapter, names the host only, and is safe to
 * return to an API caller or print on the CLI.
 */
public record ContextAccessCheck(Outcome outcome, String message) {

    public enum Outcome {
        /** The source can be read with the given (or no) credentials. */
        OK,
        /** No credentials were given and the source does not allow anonymous access. */
        AUTH_REQUIRED,
        /** Credentials were given and the source rejected them. */
        CREDENTIALS_REJECTED,
        /** Credentials were accepted but cannot browse anything. */
        NO_PROJECT_ACCESS,
        /** The base URL answered, but not like this provider. */
        NOT_A_SOURCE,
        /** The source could not be reached or answered with a server-side failure. */
        UNREACHABLE
    }

    public ContextAccessCheck {
        Objects.requireNonNull(outcome, "outcome must not be null");
        Objects.requireNonNull(message, "message must not be null");
    }

    public static ContextAccessCheck ok() {
        return new ContextAccessCheck(Outcome.OK, "ok");
    }

    public boolean passed() {
        return outcome == Outcome.OK;
    }
}
