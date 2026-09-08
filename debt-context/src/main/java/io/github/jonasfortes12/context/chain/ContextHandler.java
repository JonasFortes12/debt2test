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
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

public final class ContextHandler implements ContextEnricher {

    private static final Pattern SAFE_PROVIDER_ID = Pattern.compile("[A-Za-z][A-Za-z0-9_-]{0,31}");
    private static final Set<String> SAFE_ERROR_REASONS = Set.of(
            "NOT_FOUND", "RATE_LIMITED", "UNAUTHORIZED");

    private final IssueReferenceExtractor referenceExtractor;
    private final Optional<ContextProvider> provider;

    public ContextHandler(IssueReferenceExtractor referenceExtractor, Optional<ContextProvider> provider) {
        this.referenceExtractor = Objects.requireNonNull(referenceExtractor, "referenceExtractor must not be null");
        this.provider = Objects.requireNonNull(provider, "provider must not be null");
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
        if (references.isEmpty() || provider.isEmpty()) {
            return new EnrichedSatdDebt(debt, null, references, ContextStatus.NOT_FOUND, List.of());
        }

        List<PipelineError> referenceErrors = new ArrayList<>();
        ProviderIdentity providerIdentity = validateProviderIdentity(debt, referenceErrors, stageErrors);
        if (!providerIdentity.available()) {
            return new EnrichedSatdDebt(debt, null, references, ContextStatus.FAILED, referenceErrors);
        }
        String providerId = providerIdentity.value();

        for (ExternalReference reference : references) {
            if (!isReferenceSupportedByProvider(debt, reference, providerId, referenceErrors, stageErrors)) {
                if (hasNonRecoverableError(referenceErrors)) {
                    return failed(debt, references, referenceErrors);
                }
                continue;
            }

            ExternalTaskSpec task = fetchExternalTask(debt, reference, providerId, referenceErrors, stageErrors);
            if (task != null) {
                return new EnrichedSatdDebt(debt, task, references, ContextStatus.MATCHED, referenceErrors);
            }
            if (hasNonRecoverableError(referenceErrors)) {
                return failed(debt, references, referenceErrors);
            }
        }

        return referenceErrors.isEmpty()
                ? new EnrichedSatdDebt(debt, null, references, ContextStatus.NOT_FOUND, referenceErrors)
                : failed(debt, references, referenceErrors);
    }

    private ProviderIdentity validateProviderIdentity(
            ClassifiedDebt debt, List<PipelineError> referenceErrors, List<PipelineError> stageErrors) {
        ProviderIdentity identity = providerIdentity(provider.get());
        if (!identity.available()) {
            PipelineError error = providerFailure(debt, identity.value(), "IDENTITY", "ID_FAILED", false);
            addError(referenceErrors, stageErrors, error);
        }
        return identity;
    }

    private boolean isReferenceSupportedByProvider(
            ClassifiedDebt debt, ExternalReference reference, String providerId,
            List<PipelineError> referenceErrors, List<PipelineError> stageErrors) {
        boolean supported;
        try {
            supported = provider.get().supports(reference);
        } catch (RuntimeException ignored) {
            PipelineError error = providerFailure(debt, providerId, "SUPPORTS", "SUPPORTS_FAILED");
            addError(referenceErrors, stageErrors, error);
            return false;
        }
        return supported;
    }

    private ExternalTaskSpec fetchExternalTask(
            ClassifiedDebt debt, ExternalReference reference, String providerId,
            List<PipelineError> referenceErrors, List<PipelineError> stageErrors) {
        ProviderResolution resolution;
        try {
            resolution = provider.get().fetch(reference);
        } catch (RuntimeException ignored) {
            PipelineError error = providerFailure(debt, providerId, "FETCH", "FETCH_FAILED");
            addError(referenceErrors, stageErrors, error);
            return null;
        }
        if (resolution == null) {
            PipelineError error = providerFailure(debt, providerId, "FETCH", "NULL_RESOLUTION");
            addError(referenceErrors, stageErrors, error);
            return null;
        }
        if (resolution.error() != null) {
            PipelineError error = sanitizeProviderError(debt, providerId, resolution.error());
            addError(referenceErrors, stageErrors, error);
            return null;
        }
        if (resolution.isNotFound()) {
            return null;
        }

        ExternalTaskSpec task = resolution.task();
        if (!task.provider().equals(providerId)) {
            PipelineError error = providerFailure(debt, providerId, "FETCH", "TASK_MISMATCH", false);
            addError(referenceErrors, stageErrors, error);
            return null;
        }
        return task;
    }

    private boolean hasNonRecoverableError(List<PipelineError> referenceErrors) {
        return !referenceErrors.isEmpty()
                && !referenceErrors.get(referenceErrors.size() - 1).recoverable();
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
