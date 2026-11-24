package io.github.jonasfortes12.api.web;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.github.jonasfortes12.api.PipelineRunLauncher;
import io.github.jonasfortes12.api.access.AccessService;
import io.github.jonasfortes12.api.access.web.AccessSecurity;
import io.github.jonasfortes12.api.access.web.StudyKey;
import io.github.jonasfortes12.api.access.web.BearerTokens;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import java.util.Optional;
import java.util.function.Supplier;

import io.github.jonasfortes12.api.dto.CancelResponse;
import io.github.jonasfortes12.api.dto.ContextSourceRequest;
import io.github.jonasfortes12.api.dto.CreateRunRequest;
import io.github.jonasfortes12.api.dto.RunAcceptedResponse;
import io.github.jonasfortes12.api.dto.RunReportResponse;
import io.github.jonasfortes12.api.dto.RunStatusResponse;
import io.github.jonasfortes12.core.context.ContextAccessCheck;
import io.github.jonasfortes12.core.context.ContextCredentials;
import io.github.jonasfortes12.core.context.ContextSource;
import io.github.jonasfortes12.core.model.ClassificationOptions;
import io.github.jonasfortes12.core.model.ContextRequest;
import io.github.jonasfortes12.core.model.ExtractionOptions;
import io.github.jonasfortes12.core.model.PipelineRequest;
import io.github.jonasfortes12.core.model.ReportOptions;
import io.github.jonasfortes12.core.model.RepositoryRequest;
import io.github.jonasfortes12.core.model.RunReport;
import io.github.jonasfortes12.core.model.RunSnapshot;
import io.github.jonasfortes12.core.model.TestGenerationOptions;
import io.github.jonasfortes12.core.port.PipelineRunStore;
import io.github.jonasfortes12.orchestrator.AppOrchestrator;
import io.github.jonasfortes12.orchestrator.application.PipelineApplicationService;
import io.github.jonasfortes12.orchestrator.context.ContextSources;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/pipeline/runs")
@Tag(name = "Pipeline runs", description = "Start a run, poll its status, read its report, or cancel it. Every "
                + "operation needs an access code (Authorization: Bearer <code>), or the study service key "
                + "(X-Study-Key) when the server has one configured.")
@SecurityRequirement(name = AccessSecurity.SCHEME)
@SecurityRequirement(name = StudyKey.SCHEME)
public class PipelineRunController {

        private static final String JSON = "application/json";
        private static final String EXECUTION_ID_DESCRIPTION = "Execution ID returned when the run was started. "
                        + "A value that is not a UUID is treated as unknown (404).";

        private final PipelineRunLauncher launcher;
        private final PipelineRunStore runStore;
        private final AppOrchestrator.CliOptions cliOptions;
        private final ContextSources contextSources;
        private final AccessService accessService;

        public PipelineRunController(
                        PipelineRunLauncher launcher, PipelineRunStore runStore,
                        AppOrchestrator.CliOptions cliOptions, ContextSources contextSources,
                        AccessService accessService) {
                this.launcher = launcher;
                this.runStore = runStore;
                this.cliOptions = cliOptions;
                this.contextSources = contextSources;
                this.accessService = accessService;
        }

        @PostMapping
        @ResponseStatus(HttpStatus.ACCEPTED)
        @Operation(summary = "Start a pipeline run", description = "Records the run as QUEUED and returns at once; the stages run in the background on "
                        + "a single worker. When the request has a context source, the server first checks, "
                        + "synchronously, that it can browse that issue tracker; a failed check answers 400 "
                        + "(or 502 when the tracker is unreachable) and no run is recorded. Problems found "
                        + "while running (unreachable repository, unknown revision) end the run as FAILED "
                        + "instead of failing this call. Model paths and the output directory are server "
                        + "configuration and cannot be set per request. Needs an UNUSED access code, which this "
                        + "call spends: afterwards the code reads, polls and cancels only this run. A request "
                        + "rejected with 400, 502 or 503 does not spend the code. If the run is created but cannot "
                        + "be linked to the code, it is cancelled, the code is released, and the call answers 503. "
                        + "With the study service key (X-Study-Key) no access code is needed or spent, no participant "
                        + "quota is used, and the run is recorded as triggered by 'study'; the same key then reads, "
                        + "polls and cancels any run.")
        @ApiResponses({
                        @ApiResponse(responseCode = "202", description = "Run accepted and queued. Poll its status next.", content = @Content(mediaType = JSON, schema = @Schema(implementation = RunAcceptedResponse.class), examples = @ExampleObject(value = ApiExamples.RUN_ACCEPTED))),
                        @ApiResponse(responseCode = "400", description = "The body is not valid JSON, has a wrongly typed "
                                        + "field, or fails validation (fieldErrors lists each failing field); or the "
                                        + "context source was rejected: unsupported provider, malformed baseUrl, "
                                        + "email without apiToken (or the reverse), a private tracker without "
                                        + "credentials, rejected credentials, credentials that cannot browse any "
                                        + "project, or a baseUrl that is not a Jira site. No run is recorded.", content = @Content(mediaType = JSON, schema = @Schema(implementation = ApiError.class), examples = {
                                                        @ExampleObject(name = "validation", value = ApiExamples.INVALID_BODY),
                                                        @ExampleObject(name = "malformed", value = ApiExamples.MALFORMED_BODY),
                                                        @ExampleObject(name = "unsupportedProvider", value = ApiExamples.UNSUPPORTED_CONTEXT_PROVIDER),
                                                        @ExampleObject(name = "authRequired", value = ApiExamples.CONTEXT_AUTH_REQUIRED),
                                                        @ExampleObject(name = "credentialsRejected", value = ApiExamples.CONTEXT_CREDENTIALS_REJECTED) })),
                        @ApiResponse(responseCode = "502", description = "The context source's tracker could not be reached, "
                                        + "timed out, was rate-limiting, or failed while its access was being checked. "
                                        + "No run is recorded; retry later or check baseUrl.", content = @Content(mediaType = JSON, schema = @Schema(implementation = ApiError.class), examples = @ExampleObject(value = ApiExamples.CONTEXT_UNREACHABLE))),
                        @ApiResponse(responseCode = "415", description = "Content-Type is not application/json.", content = @Content(mediaType = JSON, schema = @Schema(implementation = ApiError.class))),
                        @ApiResponse(responseCode = "403", description = "The access code has already started its run, "
                                        + "or its email has reached its run quota. Ask for a new code to start another run.", content = @Content(mediaType = JSON, schema = @Schema(implementation = ApiError.class), examples = @ExampleObject(value = ApiExamples.ACCESS_CODE_USED))),
                        @ApiResponse(responseCode = "503", description = "The run could not be recorded because the "
                                        + "database is unavailable. Nothing was started; retry later.", content = @Content(mediaType = JSON, schema = @Schema(implementation = ApiError.class), examples = @ExampleObject(value = ApiExamples.PERSISTENCE_UNAVAILABLE)))
        })
        public RunAcceptedResponse create(
                        @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Repository to analyse and run options. Only repositoryUrl is required.", content = @Content(mediaType = JSON, examples = {
                        @ExampleObject(name = "private-jira", summary = "Mock repository with context from a private Jira", value = ApiExamples.CREATE_RUN),
                        @ExampleObject(name = "public-jira", summary = "Context from a public Jira (no credentials)", value = ApiExamples.CREATE_RUN_PUBLIC_JIRA),
                        @ExampleObject(name = "no-context", summary = "Skip issue-tracker context", value = ApiExamples.CREATE_RUN_WITHOUT_CONTEXT) })) @Valid @RequestBody CreateRunRequest request,
                        @Parameter(hidden = true) @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
                        @Parameter(hidden = true) @RequestAttribute(name = StudyKey.REQUEST_ATTRIBUTE, required = false) Boolean studyRun) {
                Optional<ContextSource> contextSource = verifiedContextSource(request.context());
                Supplier<String> submitRun = () -> launcher.submit(toPipelineRequest(request, contextSource), contextSource);
                String executionId = Boolean.TRUE.equals(studyRun)
                                ? startStudyRun(submitRun)
                                : accessService.startRun(BearerTokens.from(authorization), submitRun, launcher::cancel);
                return new RunAcceptedResponse(executionId);
        }

        private String startStudyRun(Supplier<String> submitRun) {
                String executionId = submitRun.get();
                runStore.runTriggeredBy(executionId, StudyKey.TRIGGER);
                return executionId;
        }

        @GetMapping("/{executionId}")
        @Operation(summary = "Get a run's current status", description = "Poll this every few seconds until status is terminal (COMPLETED, "
                        + "COMPLETED_WITH_ERRORS, FAILED or CANCELLED), then fetch the report. The counters are "
                        + "filled in only when the run finishes.")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "The run's current status.", content = @Content(mediaType = JSON, schema = @Schema(implementation = RunStatusResponse.class), examples = @ExampleObject(value = ApiExamples.RUN_STATUS_RUNNING))),
                        @ApiResponse(responseCode = "404", description = "No run has this execution ID, or this access code did not start it.", content = @Content(mediaType = JSON, schema = @Schema(implementation = ApiError.class), examples = @ExampleObject(value = ApiExamples.RUN_NOT_FOUND)))
        })
        public RunStatusResponse status(
                        @Parameter(description = EXECUTION_ID_DESCRIPTION, example = ApiExamples.EXECUTION_ID) @PathVariable("executionId") String executionId) {
                return RunStatusResponse.from(requireSnapshot(executionId));
        }

        @GetMapping("/{executionId}/report")
        @Operation(summary = "Get a finished run's report", description = "Available for every terminal status, including FAILED (items may be empty; the "
                        + "run-level errors say why) and CANCELLED (only what was recorded before cancelling). "
                        + "A terminal run's report never changes.")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "The run's summary, items and run-level errors.", content = @Content(mediaType = JSON, schema = @Schema(implementation = RunReportResponse.class), examples = @ExampleObject(value = ApiExamples.RUN_REPORT))),
                        @ApiResponse(responseCode = "404", description = "No run has this execution ID, or this access code did not start it.", content = @Content(mediaType = JSON, schema = @Schema(implementation = ApiError.class), examples = @ExampleObject(value = ApiExamples.RUN_NOT_FOUND))),
                        @ApiResponse(responseCode = "409", description = "The run is still QUEUED or RUNNING; keep polling "
                                        + "its status.", content = @Content(mediaType = JSON, schema = @Schema(implementation = ApiError.class), examples = @ExampleObject(value = ApiExamples.RUN_NOT_FINISHED)))
        })
        public RunReportResponse report(
                        @Parameter(description = EXECUTION_ID_DESCRIPTION, example = ApiExamples.EXECUTION_ID) @PathVariable("executionId") String executionId) {
                RunSnapshot snapshot = requireSnapshot(executionId);
                if (!snapshot.status().isTerminal()) {
                        throw new RunNotFinishedException(executionId);
                }
                RunReport report = runStore.findReport(executionId)
                                .orElseThrow(() -> new RunNotFoundException(executionId));
                return RunReportResponse.from(report);
        }

        @PostMapping("/{executionId}/cancel")
        @ResponseStatus(HttpStatus.ACCEPTED)
        @Operation(summary = "Best-effort cancel of a non-terminal run", description = "Marks the run CANCELLED immediately, and it stays CANCELLED. A QUEUED run never "
                        + "starts. A RUNNING run's stages do not check for interruption, so they may keep using "
                        + "resources (including LLM calls) in the background; their results are discarded. If "
                        + "the run reaches another terminal status at the same instant, that status stands; "
                        + "read the run's status to confirm.")
        @ApiResponses({
                        @ApiResponse(responseCode = "202", description = "The run is now CANCELLED; outcome says whether "
                                        + "this server was actually running it.", content = @Content(mediaType = JSON, schema = @Schema(implementation = CancelResponse.class), examples = @ExampleObject(value = ApiExamples.CANCEL_ACCEPTED))),
                        @ApiResponse(responseCode = "404", description = "No run has this execution ID, or this access code did not start it.", content = @Content(mediaType = JSON, schema = @Schema(implementation = ApiError.class), examples = @ExampleObject(value = ApiExamples.RUN_NOT_FOUND))),
                        @ApiResponse(responseCode = "409", description = "The run has already reached a terminal status.", content = @Content(mediaType = JSON, schema = @Schema(implementation = ApiError.class), examples = @ExampleObject(value = ApiExamples.RUN_ALREADY_FINISHED)))
        })
        public CancelResponse cancel(
                        @Parameter(description = EXECUTION_ID_DESCRIPTION, example = ApiExamples.EXECUTION_ID) @PathVariable("executionId") String executionId) {
                RunSnapshot snapshot = requireSnapshot(executionId);
                if (snapshot.status().isTerminal()) {
                        throw new RunAlreadyFinishedException(executionId);
                }
                return new CancelResponse(executionId, launcher.cancel(executionId));
        }

        private RunSnapshot requireSnapshot(String executionId) {
                return runStore.findRun(executionId).orElseThrow(() -> new RunNotFoundException(executionId));
        }

        /**
         * Turns the request's context object into a checked source, or empty when the request has none.
         * Throws (answered 400/502) before anything is recorded when the source is unusable.
         */
        private Optional<ContextSource> verifiedContextSource(ContextSourceRequest context) {
                if (context == null) {
                        return Optional.empty();
                }
                ContextSource source = toContextSource(context);
                ContextAccessCheck check = contextSources.verify(source);
                if (!check.passed()) {
                        throw new ContextSourceRejectedException(check);
                }
                return Optional.of(source);
        }

        private ContextSource toContextSource(ContextSourceRequest context) {
                if (!contextSources.supports(context.provider())) {
                        String supported = String.join(", ", contextSources.supportedProviders());
                        throw new InvalidContextSourceException("context.provider", "must be one of: " + supported,
                                        "unsupported context provider '" + context.provider().trim() + "'; supported: "
                                                        + supported);
                }
                Optional<ContextCredentials> credentials = ContextCredentials.optionalOf(context.email(),
                                context.apiToken());
                try {
                        return new ContextSource(context.provider(), context.baseUrl(), credentials);
                } catch (IllegalArgumentException malformedBaseUrl) {
                        throw new InvalidContextSourceException("context.baseUrl",
                                        "must be an absolute http or https URL", "invalid request body");
                }
        }

        private PipelineRequest toPipelineRequest(CreateRunRequest request, Optional<ContextSource> contextSource) {
                String runId = PipelineApplicationService.AUTOMATIC_RUN_ID;
                return new PipelineRequest(
                                runId,
                                new RepositoryRequest(request.repositoryUrl(), request.revision()),
                                new ExtractionOptions(runId),
                                new ClassificationOptions(request.allowHeuristicFallback()),
                                new ContextRequest(contextSource.isPresent()),
                                new TestGenerationOptions(request.testFramework()),
                                new ReportOptions(cliOptions.outputDirectory()));
        }
}
