package io.github.jonasfortes12.api.web;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.github.jonasfortes12.api.PipelineRunLauncher;
import io.github.jonasfortes12.api.dto.CancelResponse;
import io.github.jonasfortes12.api.dto.CreateRunRequest;
import io.github.jonasfortes12.api.dto.RunAcceptedResponse;
import io.github.jonasfortes12.api.dto.RunReportResponse;
import io.github.jonasfortes12.api.dto.RunStatusResponse;
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
@Tag(name = "Pipeline runs", description = "Start a run, poll its status, read its report, or cancel it.")
public class PipelineRunController {

        private static final String JSON = "application/json";
        private static final String EXECUTION_ID_DESCRIPTION = "Execution ID returned when the run was started. "
                        + "A value that is not a UUID is treated as unknown (404).";

        private final PipelineRunLauncher launcher;
        private final PipelineRunStore runStore;
        private final AppOrchestrator.CliOptions cliOptions;

        public PipelineRunController(
                        PipelineRunLauncher launcher, PipelineRunStore runStore,
                        AppOrchestrator.CliOptions cliOptions) {
                this.launcher = launcher;
                this.runStore = runStore;
                this.cliOptions = cliOptions;
        }

        @PostMapping
        @ResponseStatus(HttpStatus.ACCEPTED)
        @Operation(summary = "Start a pipeline run", description = "Records the run as QUEUED and returns at once; the stages run in the background on "
                        + "a single worker. A well-formed request is always accepted: problems found while "
                        + "running (unreachable repository, unknown revision) end the run as FAILED instead of "
                        + "failing this call. Model paths and the output directory are server configuration "
                        + "and cannot be set per request.")
        @ApiResponses({
                        @ApiResponse(responseCode = "202", description = "Run accepted and queued. Poll its status next.", content = @Content(mediaType = JSON, schema = @Schema(implementation = RunAcceptedResponse.class), examples = @ExampleObject(value = ApiExamples.RUN_ACCEPTED))),
                        @ApiResponse(responseCode = "400", description = "The body is not valid JSON, has a wrongly typed "
                                        + "field, or fails validation (fieldErrors lists each failing field).", content = @Content(mediaType = JSON, schema = @Schema(implementation = ApiError.class), examples = {
                                                        @ExampleObject(name = "validation", value = ApiExamples.INVALID_BODY),
                                                        @ExampleObject(name = "malformed", value = ApiExamples.MALFORMED_BODY) })),
                        @ApiResponse(responseCode = "415", description = "Content-Type is not application/json.", content = @Content(mediaType = JSON, schema = @Schema(implementation = ApiError.class))),
                        @ApiResponse(responseCode = "503", description = "The run could not be recorded because the "
                                        + "database is unavailable. Nothing was started; retry later.", content = @Content(mediaType = JSON, schema = @Schema(implementation = ApiError.class), examples = @ExampleObject(value = ApiExamples.PERSISTENCE_UNAVAILABLE)))
        })
        public RunAcceptedResponse create(
                        @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Repository to analyse and run options. Only repositoryUrl is required.", content = @Content(mediaType = JSON, examples = @ExampleObject(name = "mock-debt-project", summary = "Default mock repository on main", value = ApiExamples.CREATE_RUN))) @Valid @RequestBody CreateRunRequest request) {
                String executionId = launcher.submit(toPipelineRequest(request));
                return new RunAcceptedResponse(executionId);
        }

        @GetMapping("/{executionId}")
        @Operation(summary = "Get a run's current status", description = "Poll this every few seconds until status is terminal (COMPLETED, "
                        + "COMPLETED_WITH_ERRORS, FAILED or CANCELLED), then fetch the report. The counters are "
                        + "filled in only when the run finishes.")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "The run's current status.", content = @Content(mediaType = JSON, schema = @Schema(implementation = RunStatusResponse.class), examples = @ExampleObject(value = ApiExamples.RUN_STATUS_RUNNING))),
                        @ApiResponse(responseCode = "404", description = "No run has this execution ID.", content = @Content(mediaType = JSON, schema = @Schema(implementation = ApiError.class), examples = @ExampleObject(value = ApiExamples.RUN_NOT_FOUND)))
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
                        @ApiResponse(responseCode = "404", description = "No run has this execution ID.", content = @Content(mediaType = JSON, schema = @Schema(implementation = ApiError.class), examples = @ExampleObject(value = ApiExamples.RUN_NOT_FOUND))),
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
                        @ApiResponse(responseCode = "404", description = "No run has this execution ID.", content = @Content(mediaType = JSON, schema = @Schema(implementation = ApiError.class), examples = @ExampleObject(value = ApiExamples.RUN_NOT_FOUND))),
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

        private PipelineRequest toPipelineRequest(CreateRunRequest request) {
                String runId = PipelineApplicationService.AUTOMATIC_RUN_ID;
                return new PipelineRequest(
                                runId,
                                new RepositoryRequest(request.repositoryUrl(), request.revision()),
                                new ExtractionOptions(runId),
                                new ClassificationOptions(request.allowHeuristicFallback()),
                                new ContextRequest(request.contextEnabled()),
                                new TestGenerationOptions(request.testFramework()),
                                new ReportOptions(cliOptions.outputDirectory()));
        }
}
