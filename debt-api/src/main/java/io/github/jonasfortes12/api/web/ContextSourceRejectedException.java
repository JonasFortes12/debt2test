package io.github.jonasfortes12.api.web;

import io.github.jonasfortes12.core.context.ContextAccessCheck;

/**
 * The context source failed its access check before the run was recorded. Answered with
 * {@code 502} when the tracker could not be reached, otherwise {@code 400}.
 */
class ContextSourceRejectedException extends RuntimeException {

    private final ContextAccessCheck check;

    ContextSourceRejectedException(ContextAccessCheck check) {
        super(check.message());
        this.check = check;
    }

    boolean trackerUnreachable() {
        return check.outcome() == ContextAccessCheck.Outcome.UNREACHABLE;
    }
}
