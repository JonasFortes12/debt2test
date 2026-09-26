package io.github.jonasfortes12.api.web;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.github.jonasfortes12.api.PipelineRunLauncher;
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
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

@RestController
@RequestMapping("/api/pipeline/runs")
public class PipelineRunController {

    private static final String CREATE_RUN_EXAMPLE = """
            {
              "repositoryUrl": "https://github.com/JonasFortes12/mock-debt-project",
              "revision": "main",
              "contextEnabled": true,
              "allowHeuristicFallback": true
            }
            """;

    private static final String EXECUTION_ID_EXAMPLE = "3f2b8c1e-9a4d-4e7b-8f6a-2c1d5e9b7a40";

    private final PipelineRunLauncher launcher;
    private final PipelineRunStore runStore;
    private final AppOrchestrator.CliOptions cliOptions;

    public PipelineRunController(
            PipelineRunLauncher launcher, PipelineRunStore runStore, AppOrchestrator.CliOptions cliOptions) {
        this.launcher = launcher;
        this.runStore = runStore;
        this.cliOptions = cliOptions;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(summary = "Start a pipeline run")
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "Run accepted"),
            @ApiResponse(responseCode = "400", description = "Invalid request body"),
            @ApiResponse(responseCode = "503", description = "Persistence unavailable")
    })
    public RunAcceptedResponse create(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(examples = @ExampleObject(
                            name = "mock-debt-project",
                            summary = "Default mock repository on main",
                            value = CREATE_RUN_EXAMPLE)))
            @Valid @RequestBody CreateRunRequest request) {
        String executionId = launcher.submit(toPipelineRequest(request));
        return new RunAcceptedResponse(executionId);
    }

    @GetMapping("/{executionId}")
    @Operation(summary = "Get a run's current status")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Run found"),
            @ApiResponse(responseCode = "404", description = "Unknown execution ID")
    })
    public RunStatusResponse status(
            @Parameter(description = "Execution ID returned when the run was started", example = EXECUTION_ID_EXAMPLE)
            @PathVariable("executionId") String executionId) {
        return RunStatusResponse.from(requireSnapshot(executionId));
    }

    @GetMapping("/{executionId}/report")
    @Operation(summary = "Get a finished run's report")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Report available"),
            @ApiResponse(responseCode = "404", description = "Unknown execution ID"),
            @ApiResponse(responseCode = "409", description = "Run has not finished yet")
    })
    public RunReportResponse report(
            @Parameter(description = "Execution ID returned when the run was started", example = EXECUTION_ID_EXAMPLE)
            @PathVariable("executionId") String executionId) {
        RunSnapshot snapshot = requireSnapshot(executionId);
        if (!snapshot.status().isTerminal()) {
            throw new RunNotFinishedException(executionId);
        }
        RunReport report = runStore.findReport(executionId).orElseThrow(() -> new RunNotFoundException(executionId));
        return RunReportResponse.from(report);
    }

    @PostMapping("/{executionId}/cancel")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(summary = "Best-effort cancel of a non-terminal run")
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "Cancellation accepted"),
            @ApiResponse(responseCode = "404", description = "Unknown execution ID"),
            @ApiResponse(responseCode = "409", description = "Run has already finished")
    })
    public void cancel(
            @Parameter(description = "Execution ID returned when the run was started", example = EXECUTION_ID_EXAMPLE)
            @PathVariable("executionId") String executionId) {
        RunSnapshot snapshot = requireSnapshot(executionId);
        if (snapshot.status().isTerminal()) {
            throw new RunAlreadyFinishedException(executionId);
        }
        launcher.cancel(executionId);
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
