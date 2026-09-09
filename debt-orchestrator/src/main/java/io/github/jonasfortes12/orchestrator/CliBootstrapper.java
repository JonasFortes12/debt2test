package io.github.jonasfortes12.orchestrator;

import java.util.Optional;

import io.github.jonasfortes12.orchestrator.application.PipelineApplicationService;
import io.github.jonasfortes12.orchestrator.persistence.PersistenceContext;
import io.github.jonasfortes12.tester.LlmConfig;

public final class CliBootstrapper {

    private CliBootstrapper() {
    }

    public static BootstrappedPipeline bootstrap(String[] args) {
        AppOrchestrator.CliOptions options = AppOrchestrator.parseArguments(args);
        LlmConfig llmConfig = new LlmConfig();
        Optional<PersistenceContext> persistence = PersistenceContext.openIfEnabled(System::getenv);
        PipelineApplicationService application =
                AppOrchestrator.createApplication(options, llmConfig, persistence);
        return new BootstrappedPipeline(options, application, persistence);
    }
}
