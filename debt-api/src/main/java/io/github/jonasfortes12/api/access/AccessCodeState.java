package io.github.jonasfortes12.api.access;

/** Lifecycle of an access code as seen by a client. A replaced (revoked) code reports EXPIRED. */
public enum AccessCodeState {
    /** Valid, and not yet used to start a run. */
    UNUSED,
    /** Used to start its run; still grants access to that run. */
    USED,
    /** No longer grants anything: unused past its expiry, replaced, or past its results window. */
    EXPIRED
}
