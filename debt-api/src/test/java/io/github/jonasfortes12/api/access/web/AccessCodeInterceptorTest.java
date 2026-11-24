package io.github.jonasfortes12.api.access.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import io.github.jonasfortes12.api.PipelineRunLauncher;
import io.github.jonasfortes12.api.access.AccessCodeState;
import io.github.jonasfortes12.api.access.AccessService;
import io.github.jonasfortes12.api.access.AuthenticatedCode;
import io.github.jonasfortes12.api.config.CorsConfig;
import io.github.jonasfortes12.api.web.PipelineRunController;
import io.github.jonasfortes12.core.model.RunSnapshot;
import io.github.jonasfortes12.core.model.RunStatus;
import io.github.jonasfortes12.core.port.PipelineRunStore;
import io.github.jonasfortes12.core.port.ReportSink;
import io.github.jonasfortes12.orchestrator.AppOrchestrator;
import io.github.jonasfortes12.orchestrator.context.ContextSources;

@WebMvcTest(PipelineRunController.class)
@Import({ AccessCodeInterceptorTest.TestConfig.class, CorsConfig.class })
@TestPropertySource(properties = "spring.main.allow-bean-definition-overriding=true")
class AccessCodeInterceptorTest {

    private static final String CODE = "D2T-AAAA-BBBB-CCCC-DDDD-EEEE-FFFF-GGGG";
    private static final UUID OWN_RUN = UUID.fromString("3f2b8c1e-9a4d-4e7b-8f6a-2c1d5e9b7a40");
    private static final String OTHER_RUN = "11111111-2222-3333-4444-555555555555";
    private static final Instant LATER = Instant.parse("2099-01-01T00:00:00Z");

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

    /** Every run exists in the store, so a 404 can only come from the access check. */
    private void everyRunExists() {
        when(runStore.findRun(any())).thenAnswer(call -> Optional.of(new RunSnapshot(call.getArgument(0), "run-1",
                RunStatus.COMPLETED, Instant.parse("2026-11-06T12:00:00Z"), Instant.parse("2026-11-06T12:05:00Z"),
                0, 0, 0, List.of())));
    }

    private void codeIs(AccessCodeState state, UUID executionId) {
        when(accessService.authenticate(any()))
                .thenReturn(Optional.of(new AuthenticatedCode("hash", state, LATER, executionId)));
    }

    @Test
    void noHeaderAnswers401WithWwwAuthenticate() throws Exception {
        when(accessService.authenticate(any())).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/pipeline/runs/" + OWN_RUN))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", "Bearer"))
                .andExpect(jsonPath("$.message").value("missing, unknown or expired access code"));
    }

    @Test
    void expiredCodeAnswers401() throws Exception {
        codeIs(AccessCodeState.EXPIRED, OWN_RUN);

        mockMvc.perform(get("/api/pipeline/runs/" + OWN_RUN).header("Authorization", "Bearer " + CODE))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void usedCodeOnCreateAnswers403() throws Exception {
        codeIs(AccessCodeState.USED, OWN_RUN);

        mockMvc.perform(post("/api/pipeline/runs").header("Authorization", "Bearer " + CODE)
                .contentType(MediaType.APPLICATION_JSON).content("{\"repositoryUrl\":\"https://example.test/repo\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("access code has already been used for a run"));
    }

    @Test
    void codeForOtherRunAnswers404() throws Exception {
        codeIs(AccessCodeState.USED, OWN_RUN);
        everyRunExists();

        mockMvc.perform(get("/api/pipeline/runs/" + OTHER_RUN).header("Authorization", "Bearer " + CODE))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("no run for execution " + OTHER_RUN));
    }

    @Test
    void unusedCodeOnStatusAnswers404() throws Exception {
        codeIs(AccessCodeState.UNUSED, null);
        everyRunExists();

        mockMvc.perform(get("/api/pipeline/runs/" + OWN_RUN).header("Authorization", "Bearer " + CODE))
                .andExpect(status().isNotFound());
    }

    @Test
    void ownerReadsStatus200() throws Exception {
        codeIs(AccessCodeState.USED, OWN_RUN);
        when(runStore.findRun(OWN_RUN.toString())).thenReturn(Optional.of(new RunSnapshot(
                OWN_RUN.toString(), "run-1", RunStatus.RUNNING, Instant.parse("2026-11-06T12:00:00Z"), null,
                0, 0, 0, List.of())));

        mockMvc.perform(get("/api/pipeline/runs/" + OWN_RUN).header("Authorization", "bearer " + CODE))
                .andExpect(status().isOk());
    }

    @Test
    void preflightOptionsPassesWithoutHeader() throws Exception {
        mockMvc.perform(options("/api/pipeline/runs")
                .header("Origin", "http://localhost:5173")
                .header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "Authorization, Content-Type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
    }

    @Test
    void studyKeyHeaderIsIgnoredWhenNoKeyIsConfigured() throws Exception {
        when(accessService.authenticate(any())).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/pipeline/runs/" + OTHER_RUN).header("X-Study-Key", "anything"))
                .andExpect(status().isUnauthorized());
    }
}
