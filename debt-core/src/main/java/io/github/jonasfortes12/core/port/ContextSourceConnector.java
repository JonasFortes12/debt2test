package io.github.jonasfortes12.core.port;

import io.github.jonasfortes12.core.context.ContextAccessCheck;
import io.github.jonasfortes12.core.context.ContextSource;

/**
 * One issue-tracker provider (e.g. Jira) that can check a {@link ContextSource} and build the
 * {@link ContextEnricher} for a run that reads from it.
 */
public interface ContextSourceConnector {

    /** Lower-case id matched against {@link ContextSource#providerId()}. */
    String providerId();

    /**
     * Checks that the source can be read. Never throws for remote failures (unreachable host,
     * rejected credentials, ...): those are returned as an outcome.
     */
    ContextAccessCheck verifyAccess(ContextSource source);

    /** Builds the enricher for one run reading from {@code source}. */
    ContextEnricher enricherFor(ContextSource source);
}
