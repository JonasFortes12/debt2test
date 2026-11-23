package io.github.jonasfortes12.api.access;

/** The client IP exceeded its hourly code-request limit. Maps to 429. */
public class TooManyRequestsException extends RuntimeException {

    public TooManyRequestsException() {
        super("too many access-code requests from this address; try again later");
    }
}
