package io.github.jonasfortes12.orchestrator;

import java.util.Optional;

import io.github.jonasfortes12.core.context.ContextSource;
import io.github.jonasfortes12.orchestrator.application.PipelineApplicationService;
import io.github.jonasfortes12.orchestrator.context.ContextSources;
import io.github.jonasfortes12.orchestrator.persistence.PersistenceContext;

/**
 * A ready-to-run pipeline plus the persistence context backing it, if any, and the CLI's
 * context source (from {@code DEBT_CONTEXT_JIRA_*}) with the registry that checks it.
 * The caller must close the persistence context when the run ends.
 */
public record BootstrappedPipeline(
        AppOrchestrator.CliOptions options,
        PipelineApplicationService application,
        Optional<PersistenceContext> persistence,
        Optional<ContextSource> contextSource,
        ContextSources contextSources) {

    public BootstrappedPipeline(
            AppOrchestrator.CliOptions options, PipelineApplicationService application) {
        this(options, application, Optional.empty(), Optional.empty(), AppOrchestrator.contextSources());
    }
}
