package io.github.jonasfortes12.api.access;

/** The code was already used to start a run (or expired before it could be used). Maps to 403. */
public class AccessCodeUsedException extends RuntimeException {

    public AccessCodeUsedException() {
        super("access code has already been used for a run");
    }
}
