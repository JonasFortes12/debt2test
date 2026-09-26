package io.github.jonasfortes12.core.model;

public enum RunStatus {
    QUEUED,
    RUNNING,
    COMPLETED,
    COMPLETED_WITH_ERRORS,
    FAILED,
    CANCELLED;

    /** True for the four statuses a run never leaves once reached. */
    public boolean isTerminal() {
        return this == COMPLETED || this == COMPLETED_WITH_ERRORS || this == FAILED || this == CANCELLED;
    }
}
