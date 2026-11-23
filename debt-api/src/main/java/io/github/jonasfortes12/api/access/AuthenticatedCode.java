package io.github.jonasfortes12.api.access;

import java.time.Instant;
import java.util.UUID;

/**
 * A known access code and what it currently grants.
 *
 * @param expiresAt   when the current state ends: the code's expiry while UNUSED, the end of the
 *                    results window while USED, and the later of the two for EXPIRED
 * @param executionId the run started with this code, or null
 */
public record AuthenticatedCode(String codeHash, AccessCodeState state, Instant expiresAt, UUID executionId) {

    public boolean grantsAccessTo(String requestedExecutionId) {
        return state == AccessCodeState.USED && executionId != null
                && executionId.toString().equalsIgnoreCase(requestedExecutionId);
    }
}
