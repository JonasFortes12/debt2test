package io.github.jonasfortes12.orchestrator.persistence;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.UnaryOperator;

import org.springframework.boot.Banner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import io.github.jonasfortes12.core.model.PipelineRequest;
import io.github.jonasfortes12.core.model.PipelineResult;
import io.github.jonasfortes12.core.model.ReportArtifact;
import io.github.jonasfortes12.core.model.ReportOptions;
import io.github.jonasfortes12.core.model.SatdCandidate;
import io.github.jonasfortes12.core.port.PipelineRunStore;
import io.github.jonasfortes12.core.port.ReportSink;
import io.github.jonasfortes12.orchestrator.util.OrchestrationUtils;
import io.github.jonasfortes12.persistence.PersistenceConfiguration;

/**
 * Holds the headless Spring context that backs database persistence.
 *
 * <p>The CLI is not a Spring application. When persistence is enabled this boots a web-less
 * context purely to obtain the JPA-backed adapters, and closes it when the run ends. When
 * persistence is disabled nothing Spring-related is loaded at all, so the CLI keeps working
 * with no database present.
 *
 * <p>If persistence is enabled but the database is unreachable, the context is reported as
 * unavailable rather than failing the process. Its store then throws on use, which the
 * application service converts into a recoverable error: the run still executes, still writes
 * its file report, and ends COMPLETED_WITH_ERRORS with the outage recorded. An outage costs a
 * run its stored history, never its analysis.
 */
public final class PersistenceContext implements AutoCloseable {

    public static final String ENABLED_VARIABLE = "DEBT_PERSISTENCE_ENABLED";

    /** Null when persistence was enabled but could not be started. */
    private final ConfigurableApplicationContext context;

    private PersistenceContext(ConfigurableApplicationContext context) {
        this.context = context;
    }

    /**
     * @param environment reads an environment variable by name; {@code System::getenv} in production
     * @return an open context, an unavailable one if the database could not be reached, or empty
     *         when persistence is disabled
     */
    public static Optional<PersistenceContext> openIfEnabled(UnaryOperator<String> environment) {
        Objects.requireNonNull(environment, "environment must not be null");
        if (!Boolean.parseBoolean(environment.apply(ENABLED_VARIABLE))) {
            return Optional.empty();
        }
        try {
            ConfigurableApplicationContext started =
                    new SpringApplicationBuilder(PersistenceConfiguration.class)
                            .web(WebApplicationType.NONE)
                            .profiles("persistence")
                            .bannerMode(Banner.Mode.OFF)
                            .run();
            return Optional.of(new PersistenceContext(started));
        } catch (Exception failure) {
            OrchestrationUtils.logPipeline(
                    "persistence is enabled but unavailable; continuing without it");
            return Optional.of(new PersistenceContext(null));
        }
    }

    public boolean isAvailable() {
        return context != null;
    }

    public PipelineRunStore runStore() {
        return context == null ? UnavailableRunStore.INSTANCE : context.getBean(PipelineRunStore.class);
    }

    public ReportSink reportSink() {
        return context == null ? UnavailableReportSink.INSTANCE : context.getBean(ReportSink.class);
    }

    @Override
    public void close() {
        if (context != null) {
            context.close();
        }
    }

    /**
     * Fails on the bookend callbacks so an outage is recorded as a recoverable pipeline error
     * rather than passing silently. The report de-duplicates them into a single entry.
     */
    private static final class UnavailableRunStore implements PipelineRunStore {
        private static final PipelineRunStore INSTANCE = new UnavailableRunStore();

        @Override
        public void runStarted(String executionId, PipelineRequest request) {
            throw new IllegalStateException("persistence is unavailable");
        }

        @Override
        public void candidatesExtracted(String executionId, List<SatdCandidate> candidates) {
            throw new IllegalStateException("persistence is unavailable");
        }

        @Override
        public void runFinished(String executionId, PipelineResult result) {
            throw new IllegalStateException("persistence is unavailable");
        }
    }

    /** Throws so {@code CompositeReportSink} logs and skips it, keeping the file report. */
    private static final class UnavailableReportSink implements ReportSink {
        private static final ReportSink INSTANCE = new UnavailableReportSink();

        @Override
        public ReportArtifact write(PipelineResult result, ReportOptions options) {
            throw new IllegalStateException("persistence is unavailable");
        }
    }
}
