package io.github.jonasfortes12.orchestrator.application;

import java.util.Objects;
import java.util.Optional;

import io.github.jonasfortes12.core.context.ContextSource;
import io.github.jonasfortes12.core.port.ContextEnricher;

/**
 * Supplies the {@link ContextEnricher} for one run, from that run's (optional) context source.
 * Context sources may carry per-request credentials, so the enricher cannot be built once at startup.
 */
@FunctionalInterface
public interface ContextEnricherFactory {

    ContextEnricher forSource(Optional<ContextSource> source);

    /** A factory that ignores the source and always uses {@code enricher} (tests, fixed wiring). */
    static ContextEnricherFactory fixed(ContextEnricher enricher) {
        Objects.requireNonNull(enricher, "enricher must not be null");
        return source -> enricher;
    }
}
