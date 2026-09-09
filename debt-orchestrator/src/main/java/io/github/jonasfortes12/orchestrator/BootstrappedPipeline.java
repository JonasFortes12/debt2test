package io.github.jonasfortes12.orchestrator;

import java.util.Optional;

import io.github.jonasfortes12.orchestrator.application.PipelineApplicationService;
import io.github.jonasfortes12.orchestrator.persistence.PersistenceContext;

/**
 * A ready-to-run pipeline plus the persistence context backing it, if any.
 * The caller must close the context when the run ends.
 */
public record BootstrappedPipeline(
        AppOrchestrator.CliOptions options,
        PipelineApplicationService application,
        Optional<PersistenceContext> persistence) {

    public BootstrappedPipeline(
            AppOrchestrator.CliOptions options, PipelineApplicationService application) {
        this(options, application, Optional.empty());
    }
}
