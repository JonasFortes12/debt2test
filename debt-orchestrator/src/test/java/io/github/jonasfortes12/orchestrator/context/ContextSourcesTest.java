package io.github.jonasfortes12.orchestrator.context;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import io.github.jonasfortes12.core.context.ContextAccessCheck;
import io.github.jonasfortes12.core.context.ContextSource;
import io.github.jonasfortes12.core.model.ClassifiedDebt;
import io.github.jonasfortes12.core.model.ContextRequest;
import io.github.jonasfortes12.core.model.ContextStatus;
import io.github.jonasfortes12.core.model.ItemStatus;
import io.github.jonasfortes12.core.model.Provenance;
import io.github.jonasfortes12.core.model.SatdCandidate;
import io.github.jonasfortes12.core.model.SourceProvenance;
import io.github.jonasfortes12.core.port.ContextEnricher;
import io.github.jonasfortes12.core.port.ContextSourceConnector;
import io.github.jonasfortes12.core.result.ContextEnrichmentResult;

class ContextSourcesTest {

    private static final ContextSource JIRA_SOURCE =
            new ContextSource("jira", "https://acme.atlassian.net", Optional.empty());

    @Test
    void listsSupportedProvidersSorted() {
        ContextSources sources = new ContextSources(List.of(new FakeConnector("jira"), new FakeConnector("gitlab")));

        assertEquals(List.of("gitlab", "jira"), List.copyOf(sources.supportedProviders()));
    }

    @Test
    void supportsIsCaseInsensitive() {
        ContextSources sources = new ContextSources(List.of(new FakeConnector("jira")));

        assertTrue(sources.supports(" JIRA "));
        assertFalse(sources.supports("github"));
        assertFalse(sources.supports(null));
    }

    @Test
    void verifyDelegatesToMatchingConnector() {
        FakeConnector jira = new FakeConnector("jira");
        ContextSources sources = new ContextSources(List.of(jira));

        assertTrue(sources.verify(JIRA_SOURCE).passed());
        assertEquals(List.of(JIRA_SOURCE), jira.verified);
    }

    @Test
    void verifyRejectsUnknownProvider() {
        ContextSources sources = new ContextSources(List.of(new FakeConnector("jira")));
        ContextSource github = new ContextSource("github", "https://github.com", Optional.empty());

        IllegalArgumentException failure = assertThrows(IllegalArgumentException.class, () -> sources.verify(github));
        assertEquals("unsupported context provider 'github'", failure.getMessage());
        assertThrows(IllegalArgumentException.class, () -> sources.enricherFor(Optional.of(github)));
    }

    @Test
    void sourceYieldsTheConnectorsEnricher() {
        FakeConnector jira = new FakeConnector("jira");
        ContextSources sources = new ContextSources(List.of(jira));

        assertSame(jira.enricher, sources.enricherFor(Optional.of(JIRA_SOURCE)));
    }

    @Test
    void emptySourceYieldsEnricherThatSkipsWhenDisabled() {
        ContextSources sources = new ContextSources(List.of(new FakeConnector("jira")));

        ContextEnrichmentResult result = sources.enricherFor(Optional.empty())
                .enrich(List.of(classifiedDebt()), new ContextRequest(false));

        assertEquals(ContextStatus.SKIPPED, result.enrichments().get(0).contextStatus());
    }

    @Test
    void rejectsDuplicateProviderIds() {
        assertThrows(IllegalArgumentException.class,
                () -> new ContextSources(List.of(new FakeConnector("jira"), new FakeConnector("JIRA"))));
    }

    private static ClassifiedDebt classifiedDebt() {
        SatdCandidate candidate = new SatdCandidate("candidate-1", "src/Example.java", "example", 10, "TODO OPS-1",
                "void example() {}", new SourceProvenance("https://example.test/repository", "main", "src/Example.java"));
        return new ClassifiedDebt(candidate, true, "DESIGN", 0.9, new Provenance("test", "fixture", "1"),
                ItemStatus.CLASSIFIED, List.of());
    }

    private static final class FakeConnector implements ContextSourceConnector {
        private final String providerId;
        private final List<ContextSource> verified = new ArrayList<>();
        private final ContextEnricher enricher = (debts, request) -> new ContextEnrichmentResult(List.of(), List.of());

        private FakeConnector(String providerId) {
            this.providerId = providerId;
        }

        @Override
        public String providerId() {
            return providerId;
        }

        @Override
        public ContextAccessCheck verifyAccess(ContextSource source) {
            verified.add(source);
            return ContextAccessCheck.ok();
        }

        @Override
        public ContextEnricher enricherFor(ContextSource source) {
            return enricher;
        }
    }
}
