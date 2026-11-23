package io.github.jonasfortes12.orchestrator.context;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import io.github.jonasfortes12.context.chain.ContextHandler;
import io.github.jonasfortes12.context.extraction.IssueReferenceExtractor;
import io.github.jonasfortes12.core.context.ContextAccessCheck;
import io.github.jonasfortes12.core.context.ContextSource;
import io.github.jonasfortes12.core.port.ContextEnricher;
import io.github.jonasfortes12.core.port.ContextSourceConnector;

/**
 * The registered context providers, looked up by {@link ContextSource#providerId()}.
 *
 * <p>Adding a provider means adding its {@link ContextSourceConnector} to the list built in
 * {@code AppOrchestrator}; nothing else in the pipeline or the API changes.
 */
public final class ContextSources {

    private final Map<String, ContextSourceConnector> connectorsById;

    public ContextSources(List<ContextSourceConnector> connectors) {
        Map<String, ContextSourceConnector> byId = new TreeMap<>();
        for (ContextSourceConnector connector : connectors) {
            String id = normalise(connector.providerId());
            if (byId.putIfAbsent(id, connector) != null) {
                throw new IllegalArgumentException("duplicate context provider '" + id + "'");
            }
        }
        this.connectorsById = Collections.unmodifiableMap(byId);
    }

    public SortedSet<String> supportedProviders() {
        return Collections.unmodifiableSortedSet(new TreeSet<>(connectorsById.keySet()));
    }

    public boolean supports(String providerId) {
        return providerId != null && connectorsById.containsKey(normalise(providerId));
    }

    public ContextAccessCheck verify(ContextSource source) {
        return connectorFor(source).verifyAccess(source);
    }

    /**
     * The enricher for one run. Without a source, context is not looked up: the returned enricher
     * has no provider, so items resolve to {@code SKIPPED} (the request has context disabled).
     */
    public ContextEnricher enricherFor(Optional<ContextSource> source) {
        return source
                .map(present -> connectorFor(present).enricherFor(present))
                .orElseGet(() -> new ContextHandler(new IssueReferenceExtractor(), Optional.empty()));
    }

    private ContextSourceConnector connectorFor(ContextSource source) {
        ContextSourceConnector connector = connectorsById.get(source.providerId());
        if (connector == null) {
            throw new IllegalArgumentException("unsupported context provider '" + source.providerId() + "'");
        }
        return connector;
    }

    private static String normalise(String providerId) {
        return providerId.trim().toLowerCase(Locale.ROOT);
    }
}
