package io.github.jonasfortes12.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import io.github.jonasfortes12.core.port.PipelineRunStore;
import io.github.jonasfortes12.core.port.ReportSink;
import io.github.jonasfortes12.orchestrator.AppOrchestrator;
import io.github.jonasfortes12.orchestrator.application.PipelineApplicationService;
import io.github.jonasfortes12.orchestrator.context.ContextSources;

/**
 * The API's Spring context also hosts {@code debt-persistence} (see
 * {@link io.github.jonasfortes12.api.config.PersistenceImportConfig}): the run store, the database
 * report sink and the access-code store are ordinary beans here. The API therefore does not start
 * without a reachable database, unlike the CLI, which keeps running with persistence unavailable.
 */
@SpringBootApplication
public class DebtApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(DebtApiApplication.class, args);
    }

    /** Same model paths, output directory, and CLI defaults as the CLI itself (`AppOrchestrator`). */
    @Bean
    public AppOrchestrator.CliOptions cliOptions() {
        return AppOrchestrator.parseArguments(new String[0]);
    }

    @Bean
    public PipelineApplicationService pipelineApplicationService(
            AppOrchestrator.CliOptions cliOptions, PipelineRunStore pipelineRunStore, ReportSink databaseReportSink) {
        return AppOrchestrator.createApplication(cliOptions, pipelineRunStore, databaseReportSink);
    }

    /** The registered context providers; the controller checks a request's context source against them. */
    @Bean
    public ContextSources contextSources() {
        return AppOrchestrator.contextSources();
    }

    @Bean(destroyMethod = "shutdown")
    public PipelineRunLauncher pipelineRunLauncher(
            PipelineApplicationService pipelineApplicationService, PipelineRunStore pipelineRunStore) {
        return new PipelineRunLauncher(pipelineApplicationService, pipelineRunStore);
    }
}
