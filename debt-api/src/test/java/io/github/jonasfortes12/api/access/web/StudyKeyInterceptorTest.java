package io.github.jonasfortes12.api.access.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import io.github.jonasfortes12.api.PipelineRunLauncher;
import io.github.jonasfortes12.api.access.AccessService;
import io.github.jonasfortes12.api.config.CorsConfig;
import io.github.jonasfortes12.api.web.PipelineRunController;
import io.github.jonasfortes12.core.model.RunSnapshot;
import io.github.jonasfortes12.core.model.RunStatus;
import io.github.jonasfortes12.core.port.PipelineRunStore;
import io.github.jonasfortes12.core.port.ReportSink;
import io.github.jonasfortes12.orchestrator.AppOrchestrator;
import io.github.jonasfortes12.orchestrator.context.ContextSources;

/** With a study key configured: the right key replaces the access code; anything else falls back to it. */
@WebMvcTest(PipelineRunController.class)
@Import({ StudyKeyInterceptorTest.TestConfig.class, CorsConfig.class })
@TestPropertySource(properties = { "spring.main.allow-bean-definition-overriding=true", "debt.study.api-key=s3cret" })
class StudyKeyInterceptorTest {

    private static final String RUN = "3f2b8c1e-9a4d-4e7b-8f6a-2c1d5e9b7a40";

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private AccessService accessService;
    @MockitoBean
    private PipelineRunLauncher launcher;
    @MockitoBean
    private PipelineRunStore runStore;
    @MockitoBean
    private ReportSink databaseReportSink;

    @TestConfiguration
    static class TestConfig {
        @Bean
        AppOrchestrator.CliOptions cliOptions() {
            return new AppOrchestrator.CliOptions("https://example.test/repo", "b.model", "m.model", Path.of("output"));
        }

        @Bean
        ContextSources contextSources() {
            return new ContextSources(List.of());
        }
    }

    private void runExists() {
        when(runStore.findRun(any())).thenReturn(Optional.of(new RunSnapshot(RUN, "run-1", RunStatus.COMPLETED,
                Instant.parse("2026-11-06T12:00:00Z"), Instant.parse("2026-11-06T12:05:00Z"), 0, 0, 0, List.of())));
    }

    @Test
    void correctKeyReadsAnyRunWithoutAnAccessCode() throws Exception {
        runExists();

        mockMvc.perform(get("/api/pipeline/runs/" + RUN).header("X-Study-Key", "s3cret"))
                .andExpect(status().isOk());
    }

    @Test
    void wrongKeyFallsBackToTheAccessCodeCheck() throws Exception {
        when(accessService.authenticate(any())).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/pipeline/runs/" + RUN).header("X-Study-Key", "wrong"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void noKeyStillRequiresAnAccessCode() throws Exception {
        when(accessService.authenticate(any())).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/pipeline/runs/" + RUN)).andExpect(status().isUnauthorized());
    }
}
