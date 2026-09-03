package io.github.jonasfortes12.orchestrator;

import io.github.jonasfortes12.orchestrator.application.PipelineApplicationService;
import io.github.jonasfortes12.tester.LlmConfig;

public final class CliBootstrapper {

    private CliBootstrapper() {
    }

    public static BootstrappedPipeline bootstrap(String[] args) {
        AppOrchestrator.CliOptions options = AppOrchestrator.parseArguments(args);
        LlmConfig llmConfig = new LlmConfig();
        PipelineApplicationService application = AppOrchestrator.createApplication(options, llmConfig);
        return new BootstrappedPipeline(options, application);
    }
}
