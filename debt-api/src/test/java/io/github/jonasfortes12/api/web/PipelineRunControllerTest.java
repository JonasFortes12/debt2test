package io.github.jonasfortes12.api.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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
import io.github.jonasfortes12.api.dto.ContextSourceRequest;
import io.github.jonasfortes12.core.context.ContextAccessCheck;
import io.github.jonasfortes12.core.context.ContextSource;
import io.github.jonasfortes12.core.port.ContextEnricher;
import io.github.jonasfortes12.core.port.ContextSourceConnector;
import io.github.jonasfortes12.orchestrator.context.ContextSources;
import io.github.jonasfortes12.core.error.PipelineException;
import io.github.jonasfortes12.core.model.ContextStatus;
import io.github.jonasfortes12.core.model.ItemStatus;
import io.github.jonasfortes12.core.model.PipelineError;
import io.github.jonasfortes12.core.model.PipelineRequest;
import io.github.jonasfortes12.core.model.RunReport;
import io.github.jonasfortes12.core.model.RunReportItem;
import io.github.jonasfortes12.core.model.RunSnapshot;
import io.github.jonasfortes12.core.model.RunStatus;
import io.github.jonasfortes12.core.port.PipelineRunStore;
import io.github.jonasfortes12.orchestrator.AppOrchestrator;

@WebMvcTest(PipelineRunController.class)
@Import(PipelineRunControllerTest.TestConfig.class)
@TestPropertySource(properties = {
                "spring.main.allow-bean-definition-overriding=true",
                "DEBT_PERSISTENCE_ENABLED=true",
                "DEBT_DB_URL=jdbc:postgresql://localhost:1/unreachable-test-db",
                "DEBT_DB_USERNAME=test",
                "DEBT_DB_PASSWORD=test"
})
class PipelineRunControllerTest {

        @Autowired
        private MockMvc mockMvc;
        @MockitoBean
        private PipelineRunLauncher launcher;
        @MockitoBean
        private PipelineRunStore runStore;

        private static final FakeConnector JIRA = new FakeConnector();

        @TestConfiguration
        static class TestConfig {
                @Bean
                AppOrchestrator.CliOptions cliOptions() {
                        return new AppOrchestrator.CliOptions(
                                        "https://example.test/repo", "preTrainedModels/DHbinaryClassifier.model",
                                        "preTrainedModels/DHmultiClassifier.model", Path.of("output"));
                }

                @Bean
                ContextSources contextSources() {
                        return new ContextSources(List.of(JIRA));
                }
        }

        /** Stands in for the Jira connector: answers with a configurable check and records what it saw. */
        static final class FakeConnector implements ContextSourceConnector {
                private ContextAccessCheck nextCheck = ContextAccessCheck.ok();
                private final List<ContextSource> verified = new ArrayList<>();

                @Override
                public String providerId() {
                        return "jira";
                }

                @Override
                public ContextAccessCheck verifyAccess(ContextSource source) {
                        verified.add(source);
                        return nextCheck;
                }

                @Override
                public ContextEnricher enricherFor(ContextSource source) {
                        throw new UnsupportedOperationException();
                }
        }

        @BeforeEach
        void resetConnector() {
                JIRA.nextCheck = ContextAccessCheck.ok();
                JIRA.verified.clear();
        }

        private static final String PRIVATE_JIRA_RUN = """
                        {"repositoryUrl":"https://example.test/repo",
                         "context":{"provider":"jira","baseUrl":"https://acme.atlassian.net",
                                    "email":"dev@acme.test","apiToken":"s3cr3t-token"}}""";

        private ArgumentCaptor<PipelineRequest> submittedRequest() {
                ArgumentCaptor<PipelineRequest> request = ArgumentCaptor.forClass(PipelineRequest.class);
                verify(launcher).submit(request.capture(), any());
                return request;
        }

        @SuppressWarnings("unchecked")
        private Optional<ContextSource> submittedSource() {
                ArgumentCaptor<Optional<ContextSource>> source = ArgumentCaptor.forClass(Optional.class);
                verify(launcher).submit(any(), source.capture());
                return source.getValue();
        }

        private void postRun(String body, org.springframework.test.web.servlet.ResultMatcher expected)
                        throws Exception {
                mockMvc.perform(post("/api/pipeline/runs").contentType(MediaType.APPLICATION_JSON).content(body))
                                .andExpect(expected);
        }

        @Test
        void createWithoutContextSkipsVerificationAndDisablesContext() throws Exception {
                when(launcher.submit(any(), any())).thenReturn("exec-1");

                postRun("{\"repositoryUrl\":\"https://example.test/repo\"}", status().isAccepted());

                assertTrue(JIRA.verified.isEmpty());
                assertFalse(submittedRequest().getValue().context().enabled());
                assertEquals(Optional.empty(), submittedSource());
        }

        @Test
        void createWithNullContextSkipsVerification() throws Exception {
                when(launcher.submit(any(), any())).thenReturn("exec-1");

                postRun("{\"repositoryUrl\":\"https://example.test/repo\",\"context\":null}", status().isAccepted());

                assertTrue(JIRA.verified.isEmpty());
                assertFalse(submittedRequest().getValue().context().enabled());
        }

        @Test
        void createWithPublicJiraVerifiesAnonymously() throws Exception {
                when(launcher.submit(any(), any())).thenReturn("exec-1");

                postRun("""
                                {"repositoryUrl":"https://example.test/repo",
                                 "context":{"provider":"jira","baseUrl":"https://acme.atlassian.net"}}""",
                                status().isAccepted());

                assertEquals(1, JIRA.verified.size());
                assertFalse(JIRA.verified.get(0).authenticated());
                assertTrue(submittedRequest().getValue().context().enabled());
                assertEquals(Optional.of(JIRA.verified.get(0)), submittedSource());
        }

        @Test
        void createWithPrivateJiraVerifiesWithCredentials() throws Exception {
                when(launcher.submit(any(), any())).thenReturn("exec-1");

                postRun(PRIVATE_JIRA_RUN, status().isAccepted());

                assertTrue(JIRA.verified.get(0).authenticated());
                assertEquals("dev@acme.test", JIRA.verified.get(0).credentials().orElseThrow().principal());
        }

        @Test
        void createTreatsBlankCredentialsAsAnonymous() throws Exception {
                when(launcher.submit(any(), any())).thenReturn("exec-1");

                postRun("""
                                {"repositoryUrl":"https://example.test/repo",
                                 "context":{"provider":"jira","baseUrl":"https://acme.atlassian.net",
                                            "email":"","apiToken":""}}""",
                                status().isAccepted());

                assertFalse(JIRA.verified.get(0).authenticated());
        }

        @Test
        void createRejectsHalfCredentials() throws Exception {
                mockMvc.perform(post("/api/pipeline/runs").contentType(MediaType.APPLICATION_JSON).content("""
                                {"repositoryUrl":"https://example.test/repo",
                                 "context":{"provider":"jira","baseUrl":"https://acme.atlassian.net",
                                            "email":"dev@acme.test"}}"""))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.message").value("invalid request body"))
                                .andExpect(jsonPath("$.fieldErrors[0].message")
                                                .value("email and apiToken must be provided together"));
                verify(launcher, never()).submit(any(), any());
        }

        @Test
        void createRejectsMissingProvider() throws Exception {
                mockMvc.perform(post("/api/pipeline/runs").contentType(MediaType.APPLICATION_JSON).content("""
                                {"repositoryUrl":"https://example.test/repo",
                                 "context":{"baseUrl":"https://acme.atlassian.net"}}"""))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.fieldErrors[0].field").value("context.provider"));
        }

        @Test
        void createRejectsUnknownProvider() throws Exception {
                mockMvc.perform(post("/api/pipeline/runs").contentType(MediaType.APPLICATION_JSON).content("""
                                {"repositoryUrl":"https://example.test/repo",
                                 "context":{"provider":"github","baseUrl":"https://github.com"}}"""))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.message")
                                                .value("unsupported context provider 'github'; supported: jira"))
                                .andExpect(jsonPath("$.fieldErrors[0].field").value("context.provider"))
                                .andExpect(jsonPath("$.fieldErrors[0].message").value("must be one of: jira"));
                verify(launcher, never()).submit(any(), any());
                assertTrue(JIRA.verified.isEmpty());
        }

        @Test
        void createRejectsMalformedBaseUrl() throws Exception {
                mockMvc.perform(post("/api/pipeline/runs").contentType(MediaType.APPLICATION_JSON).content("""
                                {"repositoryUrl":"https://example.test/repo",
                                 "context":{"provider":"jira","baseUrl":"not a url"}}"""))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.message").value("invalid request body"))
                                .andExpect(jsonPath("$.fieldErrors[0].field").value("context.baseUrl"))
                                .andExpect(jsonPath("$.fieldErrors[0].message")
                                                .value("must be an absolute http or https URL"));
                verify(launcher, never()).submit(any(), any());
        }

        @Test
        void createRejectsCredentialsEmbeddedInBaseUrl() throws Exception {
                mockMvc.perform(post("/api/pipeline/runs").contentType(MediaType.APPLICATION_JSON).content("""
                                {"repositoryUrl":"https://example.test/repo",
                                 "context":{"provider":"jira","baseUrl":"https://dev%40acme.test:s3cr3t-token@acme.atlassian.net"}}"""))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.fieldErrors[0].field").value("context.baseUrl"))
                                .andExpect(content().string(not(containsString("s3cr3t-token"))));
                verify(launcher, never()).submit(any(), any());
                assertTrue(JIRA.verified.isEmpty());
        }

        @Test
        void createReturns400WhenJiraRequiresAuth() throws Exception {
                JIRA.nextCheck = new ContextAccessCheck(ContextAccessCheck.Outcome.AUTH_REQUIRED,
                                "Jira at acme.atlassian.net requires authentication; provide email and apiToken");

                mockMvc.perform(post("/api/pipeline/runs").contentType(MediaType.APPLICATION_JSON).content("""
                                {"repositoryUrl":"https://example.test/repo",
                                 "context":{"provider":"jira","baseUrl":"https://acme.atlassian.net"}}"""))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.message").value(
                                                "Jira at acme.atlassian.net requires authentication; provide email and apiToken"))
                                .andExpect(jsonPath("$.fieldErrors").isEmpty());
                verify(launcher, never()).submit(any(), any());
        }

        @Test
        void createReturns400WhenCredentialsRejected() throws Exception {
                JIRA.nextCheck = new ContextAccessCheck(ContextAccessCheck.Outcome.CREDENTIALS_REJECTED,
                                "Jira at acme.atlassian.net rejected the supplied credentials");

                mockMvc.perform(post("/api/pipeline/runs").contentType(MediaType.APPLICATION_JSON)
                                .content(PRIVATE_JIRA_RUN))
                                .andExpect(status().isBadRequest())
                                .andExpect(content().string(not(containsString("s3cr3t-token"))))
                                .andExpect(content().string(not(containsString("dev@acme.test"))));
                verify(launcher, never()).submit(any(), any());
        }

        @Test
        void createReturns502WhenJiraUnreachable() throws Exception {
                JIRA.nextCheck = new ContextAccessCheck(ContextAccessCheck.Outcome.UNREACHABLE,
                                "could not reach Jira at acme.atlassian.net to verify access");

                mockMvc.perform(post("/api/pipeline/runs").contentType(MediaType.APPLICATION_JSON)
                                .content(PRIVATE_JIRA_RUN))
                                .andExpect(status().isBadGateway())
                                .andExpect(jsonPath("$.message")
                                                .value("could not reach Jira at acme.atlassian.net to verify access"));
                verify(launcher, never()).submit(any(), any());
        }

        @Test
        void createIgnoresLegacyContextEnabledField() throws Exception {
                when(launcher.submit(any(), any())).thenReturn("exec-1");

                postRun("{\"repositoryUrl\":\"https://example.test/repo\",\"contextEnabled\":true}",
                                status().isAccepted());

                assertFalse(submittedRequest().getValue().context().enabled());
        }

        @Test
        void contextSourceRequestToStringRedactsSecrets() {
                String printed = new ContextSourceRequest("jira", "https://acme.atlassian.net", "dev@acme.test",
                                "s3cr3t-token").toString();

                assertFalse(printed.contains("dev@acme.test"));
                assertFalse(printed.contains("s3cr3t-token"));
        }

        private static RunSnapshot snapshot(RunStatus status) {
                return new RunSnapshot("exec-1", "run-1", status, Instant.parse("2026-09-15T10:00:00Z"),
                                status == RunStatus.RUNNING ? null : Instant.parse("2026-09-15T10:05:00Z"),
                                2, 1, 1, List.of("output/debt-report.json"));
        }

        @Test
        void createReturns202WithTheAllocatedExecutionId() throws Exception {
                when(launcher.submit(any(), any())).thenReturn("exec-1");

                mockMvc.perform(post("/api/pipeline/runs")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"repositoryUrl\":\"https://example.test/repo\"}"))
                                .andExpect(status().isAccepted())
                                .andExpect(jsonPath("$.executionId").value("exec-1"));
        }

        @Test
        void createRejectsABlankRepositoryUrl() throws Exception {
                mockMvc.perform(post("/api/pipeline/runs")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"repositoryUrl\":\"\"}"))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.message").value("invalid request body"))
                                .andExpect(jsonPath("$.fieldErrors[0].field").value("repositoryUrl"))
                                .andExpect(jsonPath("$.fieldErrors[0].message").isNotEmpty());
        }

        @Test
        void createReturns503WhenPersistenceCannotRecordTheStart() throws Exception {
                when(launcher.submit(any(), any())).thenThrow(new PipelineException(new PipelineError(
                                "persistence", "PERSISTENCE_WRITE_FAILED", "run store could not record run start",
                                null, true)));

                mockMvc.perform(post("/api/pipeline/runs")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"repositoryUrl\":\"https://example.test/repo\"}"))
                                .andExpect(status().isServiceUnavailable());
        }

        @Test
        void statusReturns200ForAKnownRun() throws Exception {
                when(runStore.findRun("exec-1")).thenReturn(Optional.of(snapshot(RunStatus.RUNNING)));

                mockMvc.perform(get("/api/pipeline/runs/exec-1"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.status").value("RUNNING"));
        }

        @Test
        void statusReturns404ForAnUnknownRun() throws Exception {
                when(runStore.findRun("missing")).thenReturn(Optional.empty());

                mockMvc.perform(get("/api/pipeline/runs/missing"))
                                .andExpect(status().isNotFound());
        }

        @Test
        void reportReturns409BeforeTheRunFinishes() throws Exception {
                when(runStore.findRun("exec-1")).thenReturn(Optional.of(snapshot(RunStatus.RUNNING)));

                mockMvc.perform(get("/api/pipeline/runs/exec-1/report"))
                                .andExpect(status().isConflict());
        }

        @Test
        void reportReturns404ForAnUnknownRun() throws Exception {
                when(runStore.findRun("missing")).thenReturn(Optional.empty());

                mockMvc.perform(get("/api/pipeline/runs/missing/report"))
                                .andExpect(status().isNotFound());
        }

        @Test
        void reportReturns200OnceTheRunHasCompleted() throws Exception {
                RunSnapshot finished = snapshot(RunStatus.COMPLETED);
                when(runStore.findRun("exec-1")).thenReturn(Optional.of(finished));
                when(runStore.findReport("exec-1"))
                                .thenReturn(Optional.of(new RunReport(finished, List.of(), List.of())));

                mockMvc.perform(get("/api/pipeline/runs/exec-1/report"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.summary.status").value("COMPLETED"));
        }

        @Test
        void cancelReturns202ForANonTerminalRun() throws Exception {
                when(runStore.findRun("exec-1")).thenReturn(Optional.of(snapshot(RunStatus.RUNNING)));
                when(launcher.cancel("exec-1")).thenReturn(PipelineRunLauncher.CancelOutcome.ACCEPTED);

                mockMvc.perform(post("/api/pipeline/runs/exec-1/cancel"))
                                .andExpect(status().isAccepted())
                                .andExpect(jsonPath("$.executionId").value("exec-1"))
                                .andExpect(jsonPath("$.outcome").value("ACCEPTED"));
        }

        @Test
        void cancelReportsUnknownWhenThisServerHasNoJobForTheRun() throws Exception {
                when(runStore.findRun("exec-1")).thenReturn(Optional.of(snapshot(RunStatus.RUNNING)));
                when(launcher.cancel("exec-1")).thenReturn(PipelineRunLauncher.CancelOutcome.UNKNOWN);

                mockMvc.perform(post("/api/pipeline/runs/exec-1/cancel"))
                                .andExpect(status().isAccepted())
                                .andExpect(jsonPath("$.outcome").value("UNKNOWN"));
        }

        @Test
        void reportReturnsRunLevelErrorsForAFailedRunWithNoItems() throws Exception {
                RunSnapshot failed = snapshot(RunStatus.FAILED);
                when(runStore.findRun("exec-1")).thenReturn(Optional.of(failed));
                when(runStore.findReport("exec-1")).thenReturn(Optional.of(new RunReport(failed, List.of(), List.of(
                                new PipelineError("workspace", "WORKSPACE_PREPARATION_FAILED",
                                                "repository could not be cloned", null, false)))));

                mockMvc.perform(get("/api/pipeline/runs/exec-1/report"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.items").isEmpty())
                                .andExpect(jsonPath("$.errors[0].code").value("WORKSPACE_PREPARATION_FAILED"))
                                .andExpect(jsonPath("$.errors[0].candidateId").value(nullValue()));
        }

        @Test
        void reportSerializesTypedStatusesAsEnumNames() throws Exception {
                RunSnapshot finished = snapshot(RunStatus.COMPLETED);
                RunReportItem item = new RunReportItem("candidate-1", "src/A.java", "save", 10, "// TODO fix",
                                true, "DESIGN", 0.9, ContextStatus.MATCHED, "JIRA-1", "Fix it", "https://jira/JIRA-1",
                                "class ATest {}", "openai", "gpt-test", ItemStatus.GENERATED, List.of(), List.of());
                when(runStore.findRun("exec-1")).thenReturn(Optional.of(finished));
                when(runStore.findReport("exec-1"))
                                .thenReturn(Optional.of(new RunReport(finished, List.of(item), List.of())));

                mockMvc.perform(get("/api/pipeline/runs/exec-1/report"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.items[0].contextStatus").value("MATCHED"))
                                .andExpect(jsonPath("$.items[0].testStatus").value("GENERATED"));
        }

        @Test
        void cancelReturns409ForAnAlreadyFinishedRun() throws Exception {
                when(runStore.findRun("exec-1")).thenReturn(Optional.of(snapshot(RunStatus.COMPLETED)));

                mockMvc.perform(post("/api/pipeline/runs/exec-1/cancel"))
                                .andExpect(status().isConflict());
        }

        @Test
        void cancelReturns404ForAnUnknownRun() throws Exception {
                when(runStore.findRun("missing")).thenReturn(Optional.empty());

                mockMvc.perform(post("/api/pipeline/runs/missing/cancel"))
                                .andExpect(status().isNotFound());
        }

        private PipelineRequest submittedRequest(String body) throws Exception {
                when(launcher.submit(any(), any())).thenReturn("exec-1");
                mockMvc.perform(post("/api/pipeline/runs")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                                .andExpect(status().isAccepted());
                ArgumentCaptor<PipelineRequest> captor = ArgumentCaptor.forClass(PipelineRequest.class);
                verify(launcher).submit(captor.capture(), any());
                return captor.getValue();
        }

        @Test
        void createDefaultsAMissingTestFrameworkToJUnit5() throws Exception {
                PipelineRequest request = submittedRequest("{\"repositoryUrl\":\"https://example.test/repo\"}");

                assertEquals("JUnit 5", request.testGeneration().framework());
        }

        @Test
        void createDefaultsABlankTestFrameworkToJUnit5() throws Exception {
                PipelineRequest request = submittedRequest(
                                "{\"repositoryUrl\":\"https://example.test/repo\",\"testFramework\":\"   \"}");

                assertEquals("JUnit 5", request.testGeneration().framework());
        }

        @Test
        void createKeepsAnExplicitTestFramework() throws Exception {
                PipelineRequest request = submittedRequest(
                                "{\"repositoryUrl\":\"https://example.test/repo\",\"testFramework\":\"TestNG\"}");

                assertEquals("TestNG", request.testGeneration().framework());
        }

        @Test
        void createIgnoresUnknownFields() throws Exception {
                PipelineRequest request = submittedRequest(
                                "{\"repositoryUrl\":\"https://example.test/repo\",\"testFrameWork\":\"TestNG\"}");

                assertEquals("JUnit 5", request.testGeneration().framework());
        }

        @Test
        void createReturnsApiErrorForMalformedJson() throws Exception {
                mockMvc.perform(post("/api/pipeline/runs")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"repositoryUrl\":"))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.message").value("malformed JSON request body"))
                                .andExpect(jsonPath("$.fieldErrors").isEmpty());
        }

        @Test
        void createReturnsApiErrorForAWrongFieldType() throws Exception {
                mockMvc.perform(post("/api/pipeline/runs")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"repositoryUrl\":\"https://example.test/repo\",\"allowHeuristicFallback\":\"yes\"}"))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.message").value("malformed JSON request body"));
        }

        @Test
        void createReturnsApiErrorForANonJsonContentType() throws Exception {
                mockMvc.perform(post("/api/pipeline/runs")
                                .contentType(MediaType.TEXT_PLAIN)
                                .content("repositoryUrl=https://example.test/repo"))
                                .andExpect(status().isUnsupportedMediaType())
                                .andExpect(jsonPath("$.message").isNotEmpty())
                                .andExpect(jsonPath("$.fieldErrors").isEmpty());
        }

        @Test
        void cancelReturnsApiErrorForTheWrongMethod() throws Exception {
                mockMvc.perform(get("/api/pipeline/runs/exec-1/cancel"))
                                .andExpect(status().isMethodNotAllowed())
                                .andExpect(jsonPath("$.message").isNotEmpty());
        }

        @Test
        void unexpectedFailureReturnsAGeneric500() throws Exception {
                when(runStore.findRun("exec-1")).thenThrow(new RuntimeException("db password=secret"));

                mockMvc.perform(get("/api/pipeline/runs/exec-1"))
                                .andExpect(status().isInternalServerError())
                                .andExpect(jsonPath("$.message").value("internal server error"))
                                .andExpect(content().string(not(containsString("secret"))));
        }
}
