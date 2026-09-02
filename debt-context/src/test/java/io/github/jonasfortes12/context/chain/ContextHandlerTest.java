package io.github.jonasfortes12.context.chain;

import io.github.jonasfortes12.context.extraction.IssueReferenceExtractor;
import io.github.jonasfortes12.context.provider.ContextProvider;
import io.github.jonasfortes12.context.provider.ProviderResolution;
import io.github.jonasfortes12.core.model.ClassifiedDebt;
import io.github.jonasfortes12.core.model.ContextRequest;
import io.github.jonasfortes12.core.model.ContextStatus;
import io.github.jonasfortes12.core.model.EnrichedSatdDebt;
import io.github.jonasfortes12.core.model.ExternalReference;
import io.github.jonasfortes12.core.model.ExternalTaskSpec;
import io.github.jonasfortes12.core.model.ItemStatus;
import io.github.jonasfortes12.core.model.PipelineError;
import io.github.jonasfortes12.core.model.Provenance;
import io.github.jonasfortes12.core.model.SatdCandidate;
import io.github.jonasfortes12.core.model.SourceProvenance;
import io.github.jonasfortes12.core.result.ContextEnrichmentResult;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContextHandlerTest {

    private final IssueReferenceExtractor extractor = new IssueReferenceExtractor();

    @Test
    void emptyProviderChainReturnsNotFound() {
        ContextEnrichmentResult result = handler(List.of()).enrich(
                List.of(classifiedDebt("candidate-1", "TODO without a reference")),
                new ContextRequest(true));

        EnrichedSatdDebt enrichment = result.enrichments().get(0);
        assertEquals(ContextStatus.NOT_FOUND, enrichment.contextStatus());
        assertNull(enrichment.externalTask());
        assertTrue(enrichment.references().isEmpty());
        assertTrue(enrichment.errors().isEmpty());
        assertTrue(result.errors().isEmpty());
    }

    @Test
    void providerNotFoundIsANormalMiss() {
        ContextProvider provider = providerFor("OPS-12", new AtomicInteger(),
                ProviderResolution.notFound());

        ContextEnrichmentResult result = handler(List.of(provider)).enrich(
                List.of(classifiedDebt("candidate-1", "TODO OPS-12")),
                new ContextRequest(true));

        EnrichedSatdDebt enrichment = result.enrichments().get(0);
        assertEquals(ContextStatus.NOT_FOUND, enrichment.contextStatus());
        assertNull(enrichment.externalTask());
        assertTrue(enrichment.errors().isEmpty());
        assertTrue(result.errors().isEmpty());
    }

    @Test
    void disabledContextReturnsSkippedWithoutCallingProviders() {
        AtomicInteger fetches = new AtomicInteger();
        ContextProvider provider = providerFor("OPS-12", fetches,
                ProviderResolution.matched(task("OPS-12")));

        ContextEnrichmentResult result = handler(List.of(provider)).enrich(
                List.of(classifiedDebt("candidate-1", "TODO OPS-12")),
                new ContextRequest(false));

        EnrichedSatdDebt enrichment = result.enrichments().get(0);
        assertEquals(ContextStatus.SKIPPED, enrichment.contextStatus());
        assertNull(enrichment.externalTask());
        assertEquals(List.of(new ExternalReference("OPS-12", "comment")), enrichment.references());
        assertEquals(0, fetches.get());
    }

    @Test
    void matchedProviderReturnsTaskAndMatchedReference() {
        ExternalTaskSpec task = task("OPS-12");
        ContextProvider provider = providerFor("OPS-12", new AtomicInteger(),
                ProviderResolution.matched(task));

        ContextEnrichmentResult result = handler(List.of(provider)).enrich(
                List.of(classifiedDebt("candidate-1", "TODO OPS-12")),
                new ContextRequest(true));

        EnrichedSatdDebt enrichment = result.enrichments().get(0);
        assertEquals(ContextStatus.MATCHED, enrichment.contextStatus());
        assertEquals(task, enrichment.externalTask());
        assertEquals(List.of(new ExternalReference("OPS-12", "comment")), enrichment.references());
        assertTrue(enrichment.errors().isEmpty());
    }

    @Test
    void firstMatchingProviderWinsInConfiguredOrder() {
        AtomicInteger firstFetches = new AtomicInteger();
        AtomicInteger secondFetches = new AtomicInteger();
        ExternalTaskSpec firstTask = task("OPS-12");
        ExternalTaskSpec secondTask = task("OPS-12-alternative");
        ContextProvider firstProvider = providerFor(
                "OPS-12", firstFetches, ProviderResolution.matched(firstTask));
        ContextProvider secondProvider = providerFor(
                "OPS-12", secondFetches, ProviderResolution.matched(secondTask));

        ContextEnrichmentResult result = handler(List.of(firstProvider, secondProvider)).enrich(
                List.of(classifiedDebt("candidate-1", "TODO OPS-12")),
                new ContextRequest(true));

        assertEquals(firstTask, result.enrichments().get(0).externalTask());
        assertEquals(1, firstFetches.get());
        assertEquals(0, secondFetches.get());
    }

    @Test
    void providerListIsSnapshottedAtConstruction() {
        AtomicInteger fetches = new AtomicInteger();
        ExternalTaskSpec originalTask = task("OPS-12");
        List<ContextProvider> providers = new ArrayList<>();
        providers.add(providerFor("OPS-12", fetches, ProviderResolution.matched(originalTask)));
        ContextHandler contextHandler = handler(providers);
        providers.clear();

        ContextEnrichmentResult result = contextHandler.enrich(
                List.of(classifiedDebt("candidate-1", "TODO OPS-12")),
                new ContextRequest(true));

        assertEquals(ContextStatus.MATCHED, result.enrichments().get(0).contextStatus());
        assertEquals(originalTask, result.enrichments().get(0).externalTask());
        assertEquals(1, fetches.get());
    }

    @Test
    void providerFailureFallsBackToLaterProviderAndPreservesError() {
        PipelineError failure = new PipelineError(
                "context", "PROVIDER_FAILED", "first provider failed", "candidate-1", true);
        ContextProvider failingProvider = providerFor("OPS-12", new AtomicInteger(),
                ProviderResolution.failed(failure));
        ExternalTaskSpec task = task("OPS-12");
        ContextProvider fallbackProvider = providerFor("OPS-12", new AtomicInteger(),
                ProviderResolution.matched(task));
        PipelineError sanitizedFailure = new PipelineError(
                "context", "CONTEXT_PROVIDER_FAKE_UNKNOWN", "Context provider FAKE returned UNKNOWN", "candidate-1", true);

        ContextEnrichmentResult result = handler(List.of(failingProvider, fallbackProvider)).enrich(
                List.of(classifiedDebt("candidate-1", "TODO OPS-12")),
                new ContextRequest(true));

        EnrichedSatdDebt enrichment = result.enrichments().get(0);
        assertEquals(ContextStatus.MATCHED, enrichment.contextStatus());
        assertEquals(ItemStatus.CLASSIFIED, enrichment.classifiedDebt().status());
        assertEquals(task, enrichment.externalTask());
        assertEquals(List.of(sanitizedFailure), enrichment.errors());
        assertEquals(List.of(sanitizedFailure), result.errors());
        assertThrows(UnsupportedOperationException.class, () -> result.errors().add(sanitizedFailure));
    }

    @Test
    void recoverableFailureContinuesToLaterReference() {
        PipelineError failure = new PipelineError(
                "context", "PROVIDER_FAILED", "first reference failed", "candidate-1", true);
        ContextProvider failingProvider = providerFor("OPS-12", new AtomicInteger(),
                ProviderResolution.failed(failure));
        ExternalTaskSpec task = task("BUILD-7");
        ContextProvider fallbackProvider = providerFor("BUILD-7", new AtomicInteger(),
                ProviderResolution.matched(task));
        PipelineError sanitizedFailure = new PipelineError(
                "context", "CONTEXT_PROVIDER_FAKE_UNKNOWN", "Context provider FAKE returned UNKNOWN", "candidate-1", true);

        ContextEnrichmentResult result = handler(List.of(failingProvider, fallbackProvider)).enrich(
                List.of(classifiedDebt("candidate-1", "TODO OPS-12 BUILD-7")),
                new ContextRequest(true));

        EnrichedSatdDebt enrichment = result.enrichments().get(0);
        assertEquals(ContextStatus.MATCHED, enrichment.contextStatus());
        assertEquals(task, enrichment.externalTask());
        assertEquals(List.of(sanitizedFailure), enrichment.errors());
        assertEquals(List.of(sanitizedFailure), result.errors());
    }

    @Test
    void allProviderFailuresReturnFailedWithoutTask() {
        PipelineError firstFailure = new PipelineError(
                "context", "FIRST_FAILED", "first provider failed", "candidate-1", true);
        PipelineError secondFailure = new PipelineError(
                "context", "SECOND_FAILED", "second provider failed", "candidate-1", false);

        ContextProvider firstProvider = providerFor("OPS-12", new AtomicInteger(),
                ProviderResolution.failed(firstFailure));
        ContextProvider secondProvider = providerFor("OPS-12", new AtomicInteger(),
                ProviderResolution.failed(secondFailure));
        PipelineError sanitizedFirstFailure = new PipelineError(
                "context", "CONTEXT_PROVIDER_FAKE_UNKNOWN", "Context provider FAKE returned UNKNOWN", "candidate-1", true);
        PipelineError sanitizedSecondFailure = new PipelineError(
                "context", "CONTEXT_PROVIDER_FAKE_UNKNOWN", "Context provider FAKE returned UNKNOWN", "candidate-1", false);

        ContextEnrichmentResult result = handler(List.of(firstProvider, secondProvider)).enrich(
                List.of(classifiedDebt("candidate-1", "TODO OPS-12")),
                new ContextRequest(true));

        EnrichedSatdDebt enrichment = result.enrichments().get(0);
        assertEquals(ContextStatus.FAILED, enrichment.contextStatus());
        assertNull(enrichment.externalTask());
        assertEquals(List.of(sanitizedFirstFailure, sanitizedSecondFailure), enrichment.errors());
        assertEquals(List.of(sanitizedFirstFailure, sanitizedSecondFailure), result.errors());
    }

    @Test
    void providerResolutionContainsExactlyOneOutcome() {
        ExternalTaskSpec task = task("OPS-12");
        PipelineError failure = new PipelineError(
                "context", "PROVIDER_FAILED", "provider failed", "candidate-1", true);

        ProviderResolution matched = ProviderResolution.matched(task);
        ProviderResolution notFound = ProviderResolution.notFound();
        ProviderResolution failed = ProviderResolution.failed(failure);

        assertEquals(task, matched.task());
        assertNull(matched.error());
        assertFalse(matched.isNotFound());
        assertNull(notFound.task());
        assertNull(notFound.error());
        assertTrue(notFound.isNotFound());
        assertNull(failed.task());
        assertEquals(failure, failed.error());
        assertFalse(failed.isNotFound());
        assertThrows(IllegalArgumentException.class, () -> new ProviderResolution(task, failure, false));
        assertThrows(IllegalArgumentException.class, () -> new ProviderResolution(null, null, false));
        assertThrows(IllegalArgumentException.class, () -> new ProviderResolution(task, null, true));
        assertThrows(IllegalArgumentException.class, () -> new ProviderResolution(null, failure, true));
    }

    @Test
    void contextUsesOnlyInjectedProvidersAndPerformsNoNetworkAccess() {
        AtomicInteger firstSupports = new AtomicInteger();
        AtomicInteger secondSupports = new AtomicInteger();
        ContextProvider firstProvider = new ContextProvider() {
            @Override
            public boolean supports(ExternalReference reference) {
                firstSupports.incrementAndGet();
                return false;
            }

            @Override
            public String providerId() {
                return "first-offline";
            }

            @Override
            public ProviderResolution fetch(ExternalReference reference) {
                throw new AssertionError("unsupported provider must not be fetched");
            }
        };
        ContextProvider secondProvider = new ContextProvider() {
            @Override
            public boolean supports(ExternalReference reference) {
                secondSupports.incrementAndGet();
                return false;
            }

            @Override
            public String providerId() {
                return "second-offline";
            }

            @Override
            public ProviderResolution fetch(ExternalReference reference) {
                throw new AssertionError("unsupported provider must not be fetched");
            }
        };

        ContextEnrichmentResult result = handler(List.of(firstProvider, secondProvider)).enrich(
                List.of(classifiedDebt("candidate-1", "TODO OPS-12")),
                new ContextRequest(true));

        assertEquals(ContextStatus.NOT_FOUND, result.enrichments().get(0).contextStatus());
        assertEquals(1, firstSupports.get());
        assertEquals(1, secondSupports.get());
        assertTrue(result.enrichments().get(0).errors().isEmpty());
    }

    @Test
    void supportsExceptionIsSanitizedAndStopsFallback() {
        ContextProvider throwingProvider = new ContextProvider() {
            @Override
            public String providerId() {
                return "supports-provider";
            }

            @Override
            public boolean supports(ExternalReference reference) {
                throw new IllegalStateException("secret network details");
            }

            @Override
            public ProviderResolution fetch(ExternalReference reference) {
                throw new AssertionError("supports failure must skip fetch");
            }
        };
        AtomicInteger fallbackFetches = new AtomicInteger();
        ContextProvider fallbackProvider = providerFor("OPS-12", fallbackFetches,
                ProviderResolution.matched(task("OPS-12")));

        ContextEnrichmentResult result = handler(List.of(throwingProvider, fallbackProvider)).enrich(
                List.of(classifiedDebt("candidate-1", "TODO OPS-12")),
                new ContextRequest(true));

        PipelineError error = result.enrichments().get(0).errors().get(0);
        assertEquals(ContextStatus.FAILED, result.enrichments().get(0).contextStatus());
        assertNull(result.enrichments().get(0).externalTask());
        assertEquals("CONTEXT_PROVIDER_SUPPORTS-PROVIDER_SUPPORTS_FAILED", error.code());
        assertEquals("candidate-1", error.candidateId());
        assertFalse(error.recoverable());
        assertFalse(error.message().contains("supports-provider"));
        assertFalse(error.message().contains("secret network details"));
        assertEquals(List.of(error), result.errors());
        assertEquals(0, fallbackFetches.get());
    }

    @Test
    void fetchExceptionIsSanitizedAndStopsFallback() {
        ContextProvider throwingProvider = new ContextProvider() {
            @Override
            public String providerId() {
                return "fetch-provider";
            }

            @Override
            public boolean supports(ExternalReference reference) {
                return true;
            }

            @Override
            public ProviderResolution fetch(ExternalReference reference) {
                throw new IllegalStateException("secret fetch details");
            }
        };
        AtomicInteger fallbackFetches = new AtomicInteger();
        ContextProvider fallbackProvider = providerFor("OPS-12", fallbackFetches,
                ProviderResolution.matched(task("OPS-12")));

        ContextEnrichmentResult result = handler(List.of(throwingProvider, fallbackProvider)).enrich(
                List.of(classifiedDebt("candidate-1", "TODO OPS-12")),
                new ContextRequest(true));

        PipelineError error = result.enrichments().get(0).errors().get(0);
        assertEquals(ContextStatus.FAILED, result.enrichments().get(0).contextStatus());
        assertNull(result.enrichments().get(0).externalTask());
        assertEquals("CONTEXT_PROVIDER_FETCH-PROVIDER_FETCH_FAILED", error.code());
        assertFalse(error.recoverable());
        assertFalse(error.message().contains("secret fetch details"));
        assertEquals(List.of(error), result.errors());
        assertEquals(0, fallbackFetches.get());
    }

    @Test
    void nullProviderResolutionIsSanitizedAndStopsFallback() {
        ContextProvider nullProvider = new ContextProvider() {
            @Override
            public String providerId() {
                return "null-provider";
            }

            @Override
            public boolean supports(ExternalReference reference) {
                return true;
            }

            @Override
            public ProviderResolution fetch(ExternalReference reference) {
                return null;
            }
        };
        AtomicInteger fallbackFetches = new AtomicInteger();
        ContextProvider fallbackProvider = providerFor("OPS-12", fallbackFetches,
                ProviderResolution.matched(task("OPS-12")));

        ContextEnrichmentResult result = handler(List.of(nullProvider, fallbackProvider)).enrich(
                List.of(classifiedDebt("candidate-1", "TODO OPS-12")),
                new ContextRequest(true));

        PipelineError error = result.enrichments().get(0).errors().get(0);
        assertEquals(ContextStatus.FAILED, result.enrichments().get(0).contextStatus());
        assertNull(result.enrichments().get(0).externalTask());
        assertEquals("CONTEXT_PROVIDER_NULL-PROVIDER_NULL_RESOLUTION", error.code());
        assertFalse(error.recoverable());
        assertEquals(List.of(error), result.errors());
        assertEquals(0, fallbackFetches.get());
    }

    @Test
    void thrownAndNullProviderFailuresRetainSafeProviderAttribution() {
        ContextProvider throwingProvider = new ContextProvider() {
            @Override
            public String providerId() {
                return "jira";
            }

            @Override
            public boolean supports(ExternalReference reference) {
                return reference.value().equals("OPS-12");
            }

            @Override
            public ProviderResolution fetch(ExternalReference reference) {
                throw new IllegalStateException("jira secret");
            }
        };
        ContextProvider nullProvider = providerFor(
                "BUILD-7", "trello", new AtomicInteger(), null);

        ContextEnrichmentResult result = handler(List.of(throwingProvider, nullProvider)).enrich(
                List.of(
                        classifiedDebt("candidate-1", "TODO OPS-12"),
                        classifiedDebt("candidate-2", "TODO BUILD-7")),
                new ContextRequest(true));

        assertEquals(List.of(
                new PipelineError(
                        "context", "CONTEXT_PROVIDER_JIRA_FETCH_FAILED",
                        "Context provider JIRA failed during fetch", "candidate-1", false),
                new PipelineError(
                        "context", "CONTEXT_PROVIDER_TRELLO_NULL_RESOLUTION",
                        "Context provider TRELLO failed during fetch", "candidate-2", false)),
                result.errors());
        assertEquals(ContextStatus.FAILED, result.enrichments().get(0).contextStatus());
        assertEquals(ContextStatus.FAILED, result.enrichments().get(1).contextStatus());
        assertNull(result.enrichments().get(0).externalTask());
        assertNull(result.enrichments().get(1).externalTask());
        assertFalse(result.errors().get(0).message().contains("jira secret"));
    }

    @Test
    void nonrecoverableProviderFailureStopsFallbackForThatDebt() {
        PipelineError failure = new PipelineError(
                "context", "UNAUTHORIZED", "provider is unauthorized", "candidate-1", false);
        AtomicInteger fallbackFetches = new AtomicInteger();
        ContextProvider blockingProvider = providerFor("OPS-12", new AtomicInteger(),
                ProviderResolution.failed(failure));
        ContextProvider fallbackProvider = providerFor("OPS-12", fallbackFetches,
                ProviderResolution.matched(task("OPS-12")));
        PipelineError sanitizedFailure = new PipelineError(
                "context", "CONTEXT_PROVIDER_FAKE_UNAUTHORIZED", "Context provider FAKE returned UNAUTHORIZED", "candidate-1", false);

        ContextEnrichmentResult result = handler(List.of(blockingProvider, fallbackProvider)).enrich(
                List.of(classifiedDebt("candidate-1", "TODO OPS-12")),
                new ContextRequest(true));

        EnrichedSatdDebt enrichment = result.enrichments().get(0);
        assertEquals(ContextStatus.FAILED, enrichment.contextStatus());
        assertNull(enrichment.externalTask());
        assertEquals(List.of(sanitizedFailure), enrichment.errors());
        assertEquals(List.of(sanitizedFailure), result.errors());
        assertEquals(0, fallbackFetches.get());
    }

    @Test
    void providerReturnedErrorIsSanitizedAndCorrelatedToCurrentDebt() {
        PipelineError maliciousError = new PipelineError(
                "untrusted-stage", "SECRET_TOKEN", "Authorization=top-secret", "other-candidate", true);
        ContextProvider provider = providerFor(
                "OPS-12", "untrusted/provider", new AtomicInteger(), ProviderResolution.failed(maliciousError));

        ContextEnrichmentResult result = handler(List.of(provider)).enrich(
                List.of(classifiedDebt("candidate-1", "TODO OPS-12")),
                new ContextRequest(true));

        PipelineError sanitizedError = result.enrichments().get(0).errors().get(0);
        assertEquals(ContextStatus.FAILED, result.enrichments().get(0).contextStatus());
        assertEquals(new PipelineError(
                "context",
                "CONTEXT_PROVIDER_UNKNOWN_UNKNOWN",
                "Context provider UNKNOWN returned UNKNOWN",
                "candidate-1",
                true), sanitizedError);
        assertEquals(List.of(sanitizedError), result.errors());
        assertFalse(sanitizedError.message().contains("Authorization=top-secret"));
        assertFalse(sanitizedError.message().contains("untrusted/provider"));
        assertFalse(sanitizedError.message().contains("other-candidate"));
    }

    @Test
    void providerErrorsRetainSafeProviderAndReasonAttribution() {
        PipelineError jiraError = new PipelineError(
                "attacker-stage", "UNAUTHORIZED", "token=secret", "wrong-candidate", true);
        PipelineError trelloError = new PipelineError(
                "another-stage", "RATE_LIMITED", "cookie=secret", "wrong-candidate", false);
        ContextProvider jiraProvider = providerFor(
                "OPS-12", "jira", new AtomicInteger(), ProviderResolution.failed(jiraError));
        ContextProvider trelloProvider = providerFor(
                "OPS-12", "trello", new AtomicInteger(), ProviderResolution.failed(trelloError));

        ContextEnrichmentResult result = handler(List.of(jiraProvider, trelloProvider)).enrich(
                List.of(classifiedDebt("candidate-1", "TODO OPS-12")),
                new ContextRequest(true));

        assertEquals(List.of(
                new PipelineError(
                        "context", "CONTEXT_PROVIDER_JIRA_UNAUTHORIZED",
                        "Context provider JIRA returned UNAUTHORIZED", "candidate-1", true),
                new PipelineError(
                        "context", "CONTEXT_PROVIDER_TRELLO_RATE_LIMITED",
                        "Context provider TRELLO returned RATE_LIMITED", "candidate-1", false)),
                result.errors());
        assertEquals(result.errors(), result.enrichments().get(0).errors());
        assertEquals(ContextStatus.FAILED, result.enrichments().get(0).contextStatus());
        assertFalse(result.errors().get(0).message().contains("token=secret"));
        assertFalse(result.errors().get(1).message().contains("cookie=secret"));
        assertFalse(result.errors().get(0).message().contains("wrong-candidate"));
    }

    @Test
    void providerFailureOnOneDebtDoesNotAbortOtherDebts() {
        ContextProvider throwingProvider = new ContextProvider() {
            @Override
            public String providerId() {
                return "throwing-provider";
            }

            @Override
            public boolean supports(ExternalReference reference) {
                return reference.value().equals("OPS-12");
            }

            @Override
            public ProviderResolution fetch(ExternalReference reference) {
                throw new IllegalStateException("must not escape stage");
            }
        };
        ExternalTaskSpec task = task("BUILD-7");
        ContextProvider matchingProvider = providerFor("BUILD-7", new AtomicInteger(),
                ProviderResolution.matched(task));

        ContextEnrichmentResult result = handler(List.of(throwingProvider, matchingProvider)).enrich(
                List.of(
                        classifiedDebt("candidate-1", "TODO OPS-12"),
                        classifiedDebt("candidate-2", "TODO BUILD-7")),
                new ContextRequest(true));

        assertEquals(2, result.enrichments().size());
        assertEquals(ContextStatus.FAILED, result.enrichments().get(0).contextStatus());
        assertEquals(ContextStatus.MATCHED, result.enrichments().get(1).contextStatus());
        assertEquals(task, result.enrichments().get(1).externalTask());
        assertEquals(1, result.errors().size());
        assertEquals("candidate-1", result.errors().get(0).candidateId());
    }

    @Test
    void mismatchedTaskProviderIsRejectedAsNonrecoverableFailure() {
        ContextProvider provider = new ContextProvider() {
            @Override
            public String providerId() {
                return "provider-a";
            }

            @Override
            public boolean supports(ExternalReference reference) {
                return true;
            }

            @Override
            public ProviderResolution fetch(ExternalReference reference) {
                return ProviderResolution.matched(new ExternalTaskSpec(
                        "provider-b", "OPS-12", "Summary", null, List.of(), List.of(), null));
            }
        };

        ContextEnrichmentResult result = handler(List.of(provider)).enrich(
                List.of(classifiedDebt("candidate-1", "TODO OPS-12")),
                new ContextRequest(true));

        EnrichedSatdDebt enrichment = result.enrichments().get(0);
        assertEquals(ContextStatus.FAILED, enrichment.contextStatus());
        assertNull(enrichment.externalTask());
        assertEquals("CONTEXT_PROVIDER_PROVIDER-A_TASK_MISMATCH", enrichment.errors().get(0).code());
        assertFalse(enrichment.errors().get(0).recoverable());
        assertEquals(enrichment.errors(), result.errors());
    }

    @Test
    void providerIdExceptionFailsOnlyAffectedDebtAndContinuesOtherDebts() {
        ContextProvider brokenProvider = new ContextProvider() {
            @Override
            public String providerId() {
                throw new IllegalStateException("secret provider identity");
            }

            @Override
            public boolean supports(ExternalReference reference) {
                return reference.value().equals("OPS-12");
            }

            @Override
            public ProviderResolution fetch(ExternalReference reference) {
                throw new AssertionError("identity failure must stop before fetch");
            }
        };
        ExternalTaskSpec task = task("BUILD-7");
        ContextProvider matchingProvider = providerFor("BUILD-7", new AtomicInteger(),
                ProviderResolution.matched(task));

        ContextEnrichmentResult result = handler(List.of(brokenProvider, matchingProvider)).enrich(
                List.of(
                        classifiedDebt("candidate-1", "TODO OPS-12"),
                        classifiedDebt("candidate-2", "TODO BUILD-7")),
                new ContextRequest(true));

        EnrichedSatdDebt failedEnrichment = result.enrichments().get(0);
        assertEquals(ContextStatus.FAILED, failedEnrichment.contextStatus());
        assertNull(failedEnrichment.externalTask());
        assertEquals("CONTEXT_PROVIDER_UNKNOWN_ID_FAILED", failedEnrichment.errors().get(0).code());
        assertEquals("candidate-1", failedEnrichment.errors().get(0).candidateId());
        assertFalse(failedEnrichment.errors().get(0).recoverable());
        assertFalse(failedEnrichment.errors().get(0).message().contains("secret provider identity"));
        assertEquals(ContextStatus.MATCHED, result.enrichments().get(1).contextStatus());
        assertEquals(task, result.enrichments().get(1).externalTask());
        assertEquals(failedEnrichment.errors(), result.errors());
    }

    private ContextHandler handler(List<ContextProvider> providers) {
        return new ContextHandler(extractor, providers);
    }

    private static ContextProvider providerFor(
            String supportedValue, AtomicInteger fetches, ProviderResolution resolution) {
        return providerFor(supportedValue, "fake", fetches, resolution);
    }

    private static ContextProvider providerFor(
            String supportedValue, String providerId, AtomicInteger fetches, ProviderResolution resolution) {
        return new ContextProvider() {
            @Override
            public boolean supports(ExternalReference reference) {
                return reference.value().equals(supportedValue);
            }

            @Override
            public String providerId() {
                return providerId;
            }

            @Override
            public ProviderResolution fetch(ExternalReference reference) {
                fetches.incrementAndGet();
                return resolution;
            }
        };
    }

    private static ClassifiedDebt classifiedDebt(String candidateId, String comment) {
        SatdCandidate candidate = new SatdCandidate(
                candidateId,
                "src/Example.java",
                "example",
                10,
                comment,
                "void example() {}",
                new SourceProvenance("https://example.test/repository", "main", "src/Example.java"));
        return new ClassifiedDebt(
                candidate,
                true,
                "DESIGN",
                0.9,
                new Provenance("test", "fixture", "1"),
                ItemStatus.CLASSIFIED,
                List.of());
    }

    private static ExternalTaskSpec task(String key) {
        return new ExternalTaskSpec("fake", key, "Summary", "Description", List.of(), List.of(), null);
    }
}
