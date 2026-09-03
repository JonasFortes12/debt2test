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
import io.github.jonasfortes12.core.model.PipelineError;
import io.github.jonasfortes12.core.port.ContextEnricher;
import io.github.jonasfortes12.core.result.ContextEnrichmentResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

public final class ContextHandler implements ContextEnricher {

    private static final Pattern SAFE_PROVIDER_ID = Pattern.compile("[A-Za-z][A-Za-z0-9_-]{0,31}");
    private static final Set<String> SAFE_ERROR_REASONS = Set.of(
            "NOT_FOUND", "RATE_LIMITED", "UNAUTHORIZED");

    private final IssueReferenceExtractor referenceExtractor;
    private final List<ContextProvider> providers;

    public ContextHandler(IssueReferenceExtractor referenceExtractor, List<ContextProvider> providers) {
        this.referenceExtractor = Objects.requireNonNull(referenceExtractor, "referenceExtractor must not be null");
        this.providers = List.copyOf(Objects.requireNonNull(providers, "providers must not be null"));
    }

    @Override
    public ContextEnrichmentResult enrich(List<ClassifiedDebt> debts, ContextRequest request) {
        Objects.requireNonNull(debts, "debts must not be null");
        Objects.requireNonNull(request, "request must not be null");

        List<EnrichedSatdDebt> enrichments = new ArrayList<>(debts.size());
        List<PipelineError> stageErrors = new ArrayList<>();
        for (ClassifiedDebt debt : debts) {
            enrichments.add(enrichDebt(
                    Objects.requireNonNull(debt, "debts must not contain null"), request.enabled(), stageErrors));
        }
        return new ContextEnrichmentResult(enrichments, stageErrors);
    }

    private EnrichedSatdDebt enrichDebt(
            ClassifiedDebt debt, boolean enabled, List<PipelineError> stageErrors) {
        List<ExternalReference> references = referenceExtractor.extract(debt.candidate().comment());
        if (!enabled) {
            return new EnrichedSatdDebt(debt, null, references, ContextStatus.SKIPPED, List.of());
        }
        if (references.isEmpty()) {
            return new EnrichedSatdDebt(debt, null, references, ContextStatus.NOT_FOUND, List.of());
        }

        List<PipelineError> providerErrors = new ArrayList<>();
        for (ExternalReference reference : references) {
            for (ContextProvider provider : providers) {
                boolean supported;
                try {
                    supported = provider.supports(reference);
                } catch (RuntimeException ignored) {
                    ProviderIdentity identity = providerIdentity(provider);
                    if (!identity.available()) {
                        PipelineError error = providerFailure(
                                debt, identity.value(), "IDENTITY", "ID_FAILED", false);
                        addError(providerErrors, stageErrors, error);
                        return failed(debt, references, providerErrors);
                    }
                    PipelineError error = providerFailure(
                            debt, identity.value(), "SUPPORTS", "SUPPORTS_FAILED");
                    addError(providerErrors, stageErrors, error);
                    if (!error.recoverable()) {
                        return failed(debt, references, providerErrors);
                    }
                    continue;
                }
                if (!supported) {
                    continue;
                }

                ProviderIdentity identity = providerIdentity(provider);
                if (!identity.available()) {
                    PipelineError error = providerFailure(
                            debt, identity.value(), "IDENTITY", "ID_FAILED", false);
                    addError(providerErrors, stageErrors, error);
                    return failed(debt, references, providerErrors);
                }
                String providerId = identity.value();
                ProviderResolution resolution;
                try {
                    resolution = provider.fetch(reference);
                } catch (RuntimeException ignored) {
                    PipelineError error = providerFailure(
                            debt, providerId, "FETCH", "FETCH_FAILED");
                    addError(providerErrors, stageErrors, error);
                    if (!error.recoverable()) {
                        return failed(debt, references, providerErrors);
                    }
                    continue;
                }
                if (resolution == null) {
                    PipelineError error = providerFailure(
                            debt, providerId, "FETCH", "NULL_RESOLUTION");
                    addError(providerErrors, stageErrors, error);
                    if (!error.recoverable()) {
                        return failed(debt, references, providerErrors);
                    }
                    continue;
                }
                if (resolution.error() != null) {
                    PipelineError error = sanitizeProviderError(debt, providerId, resolution.error());
                    addError(providerErrors, stageErrors, error);
                    if (!error.recoverable()) {
                        return failed(debt, references, providerErrors);
                    }
                    continue;
                }
                if (resolution.isNotFound()) {
                    continue;
                }

                ExternalTaskSpec task = resolution.task();
                if (providerId == null || !task.provider().equals(providerId)) {
                    PipelineError error = providerFailure(
                            debt, providerId, "FETCH", "TASK_MISMATCH", false);
                    addError(providerErrors, stageErrors, error);
                    return failed(debt, references, providerErrors);
                }
                return new EnrichedSatdDebt(debt, task, references, ContextStatus.MATCHED, providerErrors);
            }
        }

        return providerErrors.isEmpty()
                ? new EnrichedSatdDebt(debt, null, references, ContextStatus.NOT_FOUND, providerErrors)
                : failed(debt, references, providerErrors);
    }

    private EnrichedSatdDebt failed(
            ClassifiedDebt debt, List<ExternalReference> references, List<PipelineError> errors) {
        return new EnrichedSatdDebt(debt, null, references, ContextStatus.FAILED, errors);
    }

    private void addError(
            List<PipelineError> itemErrors, List<PipelineError> stageErrors, PipelineError error) {
        itemErrors.add(error);
        stageErrors.add(error);
    }

    private PipelineError providerFailure(
            ClassifiedDebt debt, String providerId, String operation, String reason) {
        return providerFailure(debt, providerId, operation, reason, false);
    }

    private PipelineError providerFailure(
            ClassifiedDebt debt, String providerId, String operation, String reason, boolean recoverable) {
        String providerLabel = safeProviderLabel(providerId);
        return new PipelineError(
                "context",
                "CONTEXT_PROVIDER_" + providerLabel + "_" + reason,
                "Context provider " + providerLabel + " failed during " + operation.toLowerCase(Locale.ROOT),
                debt.candidateId(),
                recoverable);
    }

    private PipelineError sanitizeProviderError(
            ClassifiedDebt debt, String providerId, PipelineError providerError) {
        String providerLabel = safeProviderLabel(providerId);
        String reason = safeErrorReason(providerError);
        return new PipelineError(
                "context",
                "CONTEXT_PROVIDER_" + providerLabel + "_" + reason,
                "Context provider " + providerLabel + " returned " + reason,
                debt.candidateId(),
                providerError.recoverable());
    }

    private String safeProviderLabel(String providerId) {
        if (providerId == null || !SAFE_PROVIDER_ID.matcher(providerId).matches()) {
            return "UNKNOWN";
        }
        return providerId.toUpperCase(Locale.ROOT);
    }

    private String safeErrorReason(PipelineError providerError) {
        String reason = providerError.code().toUpperCase(Locale.ROOT);
        return SAFE_ERROR_REASONS.contains(reason) ? reason : "UNKNOWN";
    }

    private ProviderIdentity providerIdentity(ContextProvider provider) {
        try {
            String providerId = provider.providerId();
            return providerId == null || providerId.isBlank()
                    ? new ProviderIdentity(null, false)
                    : new ProviderIdentity(providerId, true);
        } catch (RuntimeException ignored) {
            return new ProviderIdentity(null, false);
        }
    }

    private record ProviderIdentity(String value, boolean available) {
    }
}
