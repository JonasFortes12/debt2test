package io.github.jonasfortes12.orchestrator;

import io.github.jonasfortes12.orchestrator.application.PipelineApplicationService;

public record BootstrappedPipeline(
        AppOrchestrator.CliOptions options,
        PipelineApplicationService application) {
}
