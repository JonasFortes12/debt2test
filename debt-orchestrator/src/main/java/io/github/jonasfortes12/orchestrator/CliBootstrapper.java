package io.github.jonasfortes12.orchestrator;

import java.util.Optional;
import java.util.function.UnaryOperator;

import io.github.cdimascio.dotenv.Dotenv;
import io.github.jonasfortes12.orchestrator.application.PipelineApplicationService;
import io.github.jonasfortes12.orchestrator.persistence.PersistenceContext;
import io.github.jonasfortes12.tester.LlmConfig;

public final class CliBootstrapper {

    private CliBootstrapper() {
    }

    public static BootstrappedPipeline bootstrap(String[] args) {
        AppOrchestrator.CliOptions options = AppOrchestrator.parseArguments(args);
        LlmConfig llmConfig = new LlmConfig();
        Optional<PersistenceContext> persistence = PersistenceContext.openIfEnabled(environment());
        PipelineApplicationService application =
                AppOrchestrator.createApplication(options, llmConfig, persistence);
        return new BootstrappedPipeline(options, application, persistence);
    }

    /**
     * Reads a process environment variable first, falling back to a local {@code .env} file
     * (ignored if absent). Matches the precedence {@link LlmConfig} and the Jira context provider
     * already use, so persistence config can also live in {@code .env}.
     */
    private static UnaryOperator<String> environment() {
        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
        return name -> {
            String value = System.getenv(name);
            if (value == null || value.isBlank()) {
                value = dotenv.get(name);
            }
            return value;
        };
    }
}
