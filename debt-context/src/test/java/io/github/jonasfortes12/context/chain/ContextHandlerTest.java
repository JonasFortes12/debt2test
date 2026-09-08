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

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContextHandlerTest {

    private final IssueReferenceExtractor extractor = new IssueReferenceExtractor();

    @Test
    void noProviderConfiguredReturnsNotFound() {
        ContextEnrichmentResult result = handler(Optional.empty()).enrich(
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
    void noProviderConfiguredWithReferencesStillReturnsNotFoundWithoutError() {
        ContextEnrichmentResult result = handler(Optional.empty()).enrich(
                List.of(classifiedDebt("candidate-1", "TODO OPS-12")),
                new ContextRequest(true));

        EnrichedSatdDebt enrichment = result.enrichments().get(0);
        assertEquals(ContextStatus.NOT_FOUND, enrichment.contextStatus());
        assertNull(enrichment.externalTask());
        assertEquals(List.of(new ExternalReference("OPS-12", "comment")), enrichment.references());
        assertTrue(enrichment.errors().isEmpty());
        assertTrue(result.errors().isEmpty());
    }

    @Test
    void providerNotFoundIsANormalMiss() {
        ContextProvider provider = providerFor("OPS-12", new AtomicInteger(),
                ProviderResolution.notFound());

        ContextEnrichmentResult result = handler(provider).enrich(
                List.of(classifiedDebt("candidate-1", "TODO OPS-12")),
                new ContextRequest(true));

        EnrichedSatdDebt enrichment = result.enrichments().get(0);
        assertEquals(ContextStatus.NOT_FOUND, enrichment.contextStatus());
        assertNull(enrichment.externalTask());
        assertTrue(enrichment.errors().isEmpty());
        assertTrue(result.errors().isEmpty());
    }

    @Test
    void disabledContextReturnsSkippedWithoutCallingProvider() {
        AtomicInteger fetches = new AtomicInteger();
        ContextProvider provider = providerFor("OPS-12", fetches,
                ProviderResolution.matched(task("OPS-12")));

        ContextEnrichmentResult result = handler(provider).enrich(
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

        ContextEnrichmentResult result = handler(provider).enrich(
                List.of(classifiedDebt("candidate-1", "TODO OPS-12")),
                new ContextRequest(true));

        EnrichedSatdDebt enrichment = result.enrichments().get(0);
        assertEquals(ContextStatus.MATCHED, enrichment.contextStatus());
        assertEquals(task, enrichment.externalTask());
        assertEquals(List.of(new ExternalReference("OPS-12", "comment")), enrichment.references());
        assertTrue(enrichment.errors().isEmpty());
    }

    @Test
    void unsupportedReferenceIsSkippedWithoutFetchAndReturnsNotFound() {
        AtomicInteger fetches = new AtomicInteger();
        ContextProvider provider = providerFor("BUILD-7", fetches,
                ProviderResolution.matched(task("BUILD-7")));

        ContextEnrichmentResult result = handler(provider).enrich(
                List.of(classifiedDebt("candidate-1", "TODO OPS-12")),
                new ContextRequest(true));

        EnrichedSatdDebt enrichment = result.enrichments().get(0);
        assertEquals(ContextStatus.NOT_FOUND, enrichment.contextStatus());
        assertNull(enrichment.externalTask());
        assertEquals(0, fetches.get());
        assertTrue(enrichment.errors().isEmpty());
    }

    @Test
    void recoverableReferenceFailureContinuesToLaterReference() {
        PipelineError failure = new PipelineError(
                "context", "PROVIDER_FAILED", "first reference failed", "candidate-1", true);
        ExternalTaskSpec task = task("BUILD-7");
        ContextProvider provider = new ContextProvider() {
            @Override
            public String providerId() {
                return "fake";
            }

            @Override
            public boolean supports(ExternalReference reference) {
                return true;
            }

            @Override
            public ProviderResolution fetch(ExternalReference reference) {
                return reference.value().equals("OPS-12")
                        ? ProviderResolution.failed(failure)
                        : ProviderResolution.matched(task);
            }
        };
        PipelineError sanitizedFailure = new PipelineError(
                "context", "CONTEXT_PROVIDER_FAKE_UNKNOWN", "Context provider FAKE returned UNKNOWN", "candidate-1", true);

        ContextEnrichmentResult result = handler(provider).enrich(
                List.of(classifiedDebt("candidate-1", "TODO OPS-12 BUILD-7")),
                new ContextRequest(true));

        EnrichedSatdDebt enrichment = result.enrichments().get(0);
        assertEquals(ContextStatus.MATCHED, enrichment.contextStatus());
        assertEquals(task, enrichment.externalTask());
        assertEquals(List.of(sanitizedFailure), enrichment.errors());
        assertEquals(List.of(sanitizedFailure), result.errors());
    }

    @Test
    void recoverableThenNonRecoverableReferenceFailuresReturnFailedWithoutTask() {
        PipelineError firstFailure = new PipelineError(
                "context", "FIRST_FAILED", "first reference failed", "candidate-1", true);
        PipelineError secondFailure = new PipelineError(
                "context", "SECOND_FAILED", "second reference failed", "candidate-1", false);
        ContextProvider provider = new ContextProvider() {
            @Override
            public String providerId() {
                return "fake";
            }

            @Override
            public boolean supports(ExternalReference reference) {
                return true;
            }

            @Override
            public ProviderResolution fetch(ExternalReference reference) {
                return reference.value().equals("OPS-12")
                        ? ProviderResolution.failed(firstFailure)
                        : ProviderResolution.failed(secondFailure);
            }
        };
        PipelineError sanitizedFirstFailure = new PipelineError(
                "context", "CONTEXT_PROVIDER_FAKE_UNKNOWN", "Context provider FAKE returned UNKNOWN", "candidate-1", true);
        PipelineError sanitizedSecondFailure = new PipelineError(
                "context", "CONTEXT_PROVIDER_FAKE_UNKNOWN", "Context provider FAKE returned UNKNOWN", "candidate-1", false);

        ContextEnrichmentResult result = handler(provider).enrich(
                List.of(classifiedDebt("candidate-1", "TODO OPS-12 BUILD-7")),
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
    void contextUsesOnlyTheInjectedProviderAndPerformsNoNetworkAccess() {
        AtomicInteger supportsCalls = new AtomicInteger();
        ContextProvider provider = new ContextProvider() {
            @Override
            public boolean supports(ExternalReference reference) {
                supportsCalls.incrementAndGet();
                return false;
            }

            @Override
            public String providerId() {
                return "offline";
            }

            @Override
            public ProviderResolution fetch(ExternalReference reference) {
                throw new AssertionError("unsupported reference must not be fetched");
            }
        };

        ContextEnrichmentResult result = handler(provider).enrich(
                List.of(classifiedDebt("candidate-1", "TODO OPS-12")),
                new ContextRequest(true));

        assertEquals(ContextStatus.NOT_FOUND, result.enrichments().get(0).contextStatus());
        assertEquals(1, supportsCalls.get());
        assertTrue(result.enrichments().get(0).errors().isEmpty());
    }

    @Test
    void supportsExceptionIsSanitizedAndStopsFurtherReferences() {
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

        ContextEnrichmentResult result = handler(throwingProvider).enrich(
                List.of(classifiedDebt("candidate-1", "TODO OPS-12 BUILD-7")),
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
        assertEquals(1, result.enrichments().get(0).errors().size());
    }

    @Test
    void fetchExceptionIsSanitizedAndStopsFurtherReferences() {
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

        ContextEnrichmentResult result = handler(throwingProvider).enrich(
                List.of(classifiedDebt("candidate-1", "TODO OPS-12 BUILD-7")),
                new ContextRequest(true));

        PipelineError error = result.enrichments().get(0).errors().get(0);
        assertEquals(ContextStatus.FAILED, result.enrichments().get(0).contextStatus());
        assertNull(result.enrichments().get(0).externalTask());
        assertEquals("CONTEXT_PROVIDER_FETCH-PROVIDER_FETCH_FAILED", error.code());
        assertFalse(error.recoverable());
        assertFalse(error.message().contains("secret fetch details"));
        assertEquals(List.of(error), result.errors());
        assertEquals(1, result.enrichments().get(0).errors().size());
    }

    @Test
    void nullProviderResolutionIsSanitizedAndStopsFurtherReferences() {
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

        ContextEnrichmentResult result = handler(nullProvider).enrich(
                List.of(classifiedDebt("candidate-1", "TODO OPS-12 BUILD-7")),
                new ContextRequest(true));

        PipelineError error = result.enrichments().get(0).errors().get(0);
        assertEquals(ContextStatus.FAILED, result.enrichments().get(0).contextStatus());
        assertNull(result.enrichments().get(0).externalTask());
        assertEquals("CONTEXT_PROVIDER_NULL-PROVIDER_NULL_RESOLUTION", error.code());
        assertFalse(error.recoverable());
        assertEquals(List.of(error), result.errors());
        assertEquals(1, result.enrichments().get(0).errors().size());
    }

    @Test
    void providerReturnedErrorIsSanitizedAndCorrelatedToCurrentDebt() {
        PipelineError maliciousError = new PipelineError(
                "untrusted-stage", "SECRET_TOKEN", "Authorization=top-secret", "other-candidate", true);
        ContextProvider provider = providerFor(
                "OPS-12", "untrusted/provider", new AtomicInteger(), ProviderResolution.failed(maliciousError));

        ContextEnrichmentResult result = handler(provider).enrich(
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
    void providerFailureOnOneDebtDoesNotAbortOtherDebts() {
        ExternalTaskSpec task = new ExternalTaskSpec(
                "shared-provider", "BUILD-7", "Summary", "Description", List.of(), List.of(), null);
        ContextProvider provider = new ContextProvider() {
            @Override
            public String providerId() {
                return "shared-provider";
            }

            @Override
            public boolean supports(ExternalReference reference) {
                return true;
            }

            @Override
            public ProviderResolution fetch(ExternalReference reference) {
                if (reference.value().equals("OPS-12")) {
                    throw new IllegalStateException("must not escape stage");
                }
                return ProviderResolution.matched(task);
            }
        };

        ContextEnrichmentResult result = handler(provider).enrich(
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

        ContextEnrichmentResult result = handler(provider).enrich(
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
    void providerIdExceptionFailsOnlyDebtsThatNeedTheProvider() {
        ContextProvider brokenProvider = new ContextProvider() {
            @Override
            public String providerId() {
                throw new IllegalStateException("secret provider identity");
            }

            @Override
            public boolean supports(ExternalReference reference) {
                throw new AssertionError("identity failure must stop before supports");
            }

            @Override
            public ProviderResolution fetch(ExternalReference reference) {
                throw new AssertionError("identity failure must stop before fetch");
            }
        };

        ContextEnrichmentResult result = handler(brokenProvider).enrich(
                List.of(
                        classifiedDebt("candidate-1", "TODO OPS-12"),
                        classifiedDebt("candidate-2", "no reference here")),
                new ContextRequest(true));

        EnrichedSatdDebt failedEnrichment = result.enrichments().get(0);
        assertEquals(ContextStatus.FAILED, failedEnrichment.contextStatus());
        assertNull(failedEnrichment.externalTask());
        assertEquals("CONTEXT_PROVIDER_UNKNOWN_ID_FAILED", failedEnrichment.errors().get(0).code());
        assertEquals("candidate-1", failedEnrichment.errors().get(0).candidateId());
        assertFalse(failedEnrichment.errors().get(0).recoverable());
        assertFalse(failedEnrichment.errors().get(0).message().contains("secret provider identity"));

        EnrichedSatdDebt unaffectedEnrichment = result.enrichments().get(1);
        assertEquals(ContextStatus.NOT_FOUND, unaffectedEnrichment.contextStatus());
        assertTrue(unaffectedEnrichment.errors().isEmpty());

        assertEquals(failedEnrichment.errors(), result.errors());
    }

    @Test
    void blankProviderIdFailsEnrichmentWithoutCallingSupportsOrFetch() {
        ContextProvider blankIdProvider = new ContextProvider() {
            @Override
            public String providerId() {
                return "  ";
            }

            @Override
            public boolean supports(ExternalReference reference) {
                throw new AssertionError("identity failure must stop before supports");
            }

            @Override
            public ProviderResolution fetch(ExternalReference reference) {
                throw new AssertionError("identity failure must stop before fetch");
            }
        };

        ContextEnrichmentResult result = handler(blankIdProvider).enrich(
                List.of(classifiedDebt("candidate-1", "TODO OPS-12")),
                new ContextRequest(true));

        EnrichedSatdDebt enrichment = result.enrichments().get(0);
        assertEquals(ContextStatus.FAILED, enrichment.contextStatus());
        assertEquals("CONTEXT_PROVIDER_UNKNOWN_ID_FAILED", enrichment.errors().get(0).code());
        assertFalse(enrichment.errors().get(0).recoverable());
    }

    private ContextHandler handler(ContextProvider provider) {
        return handler(Optional.of(provider));
    }

    private ContextHandler handler(Optional<ContextProvider> provider) {
        return new ContextHandler(extractor, provider);
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
