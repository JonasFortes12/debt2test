package io.github.jonasfortes12.api;

import java.util.Optional;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration;
import org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration;
import org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

import io.github.jonasfortes12.core.port.PipelineRunStore;
import io.github.jonasfortes12.orchestrator.AppOrchestrator;
import io.github.jonasfortes12.orchestrator.application.PipelineApplicationService;
import io.github.jonasfortes12.orchestrator.persistence.PersistenceContext;

/**
 * Excludes the JPA/Flyway/DataSource auto-configuration that {@code debt-persistence} pulls onto
 * this module's classpath (transitively, via {@code debt-orchestrator}): this outer Spring
 * context never talks to the database directly, {@link PersistenceContext} boots its own separate
 * child context for that. Without the exclusion, Spring Boot's classpath-driven auto-configuration
 * tries to build a {@code DataSource}/{@code EntityManagerFactory}/{@code Flyway} bean for THIS
 * context too, which has no {@code spring.datasource.url} to work with and fails context startup.
 */
@SpringBootApplication(exclude = {
        DataSourceAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class,
        FlywayAutoConfiguration.class,
        DataJpaRepositoriesAutoConfiguration.class
})
public class DebtApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(DebtApiApplication.class, args);
    }

    /**
     * Fails fast at context startup if persistence is not enabled (D-3): {@code GET /runs/{id}}
     * has nowhere to read state back from otherwise. Reads through Spring's {@link Environment},
     * which by the time any {@code @Bean} method runs already includes {@code .env} values via
     * {@link DotenvEnvironmentPostProcessor} (a real OS environment variable, a JVM system
     * property, or a test's {@code @TestPropertySource} still wins over it).
     */
    @Bean(destroyMethod = "close")
    public PersistenceContext persistenceContext(Environment environment) {
        return PersistenceContext.openIfEnabled(environment::getProperty)
                .orElseThrow(() -> new IllegalStateException(
                        PersistenceContext.ENABLED_VARIABLE + " must be true to start debt-api"));
    }

    /** Same model paths, output directory, and CLI defaults as the CLI itself (`AppOrchestrator`). */
    @Bean
    public AppOrchestrator.CliOptions cliOptions() {
        return AppOrchestrator.parseArguments(new String[0]);
    }

    @Bean
    public PipelineApplicationService pipelineApplicationService(
            AppOrchestrator.CliOptions cliOptions, PersistenceContext persistenceContext) {
        return AppOrchestrator.createApplication(cliOptions, Optional.of(persistenceContext));
    }

    @Bean
    public PipelineRunStore pipelineRunStore(PersistenceContext persistenceContext) {
        return persistenceContext.runStore();
    }

    @Bean(destroyMethod = "shutdown")
    public PipelineRunLauncher pipelineRunLauncher(
            PipelineApplicationService pipelineApplicationService, PipelineRunStore pipelineRunStore) {
        return new PipelineRunLauncher(pipelineApplicationService, pipelineRunStore);
    }
}
