package io.github.jonasfortes12.api.access;

/** No usable access code: missing, unknown, or expired. Maps to 401 with {@code WWW-Authenticate: Bearer}. */
public class AccessCodeRequiredException extends RuntimeException {

    public AccessCodeRequiredException() {
        super("missing, unknown or expired access code");
    }
}
