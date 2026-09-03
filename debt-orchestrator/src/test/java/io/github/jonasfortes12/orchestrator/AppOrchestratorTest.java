package io.github.jonasfortes12.orchestrator;

import io.github.jonasfortes12.classifier.WekaDebtHunterClassifier;
import io.github.jonasfortes12.context.chain.ContextHandler;
import io.github.jonasfortes12.core.model.PipelineRequest;
import io.github.jonasfortes12.core.model.PipelineResult;
import io.github.jonasfortes12.core.model.ReportOptions;
import io.github.jonasfortes12.core.model.RepositoryWorkspace;
import io.github.jonasfortes12.core.model.RunStatus;
import io.github.jonasfortes12.orchestrator.application.PipelineApplicationService;
import io.github.jonasfortes12.extractor.AstCommentExtractor;
import io.github.jonasfortes12.extractor.GitCloneService;
import io.github.jonasfortes12.orchestrator.reporting.FileReportSink;
import io.github.jonasfortes12.tester.LlmConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class AppOrchestratorTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void parsesDefaultAndPositionalCliValues() {
        AppOrchestrator.CliOptions defaults = AppOrchestrator.parseArguments(new String[0]);
        assertEquals("https://github.com/apache/dubbo", defaults.repositoryUrl());
        assertEquals("preTrainedModels/DHbinaryClassifier.model", defaults.binaryModelPath());
        assertEquals("preTrainedModels/DHmultiClassifier.model", defaults.multiModelPath());
        assertEquals(Path.of("output"), defaults.outputDirectory());

        AppOrchestrator.CliOptions overrides = AppOrchestrator.parseArguments(new String[]{
                "https://example.test/repository", "binary.model", "multi.model", "reports"});
        assertEquals("https://example.test/repository", overrides.repositoryUrl());
        assertEquals("binary.model", overrides.binaryModelPath());
        assertEquals("multi.model", overrides.multiModelPath());
        assertEquals(Path.of("reports"), overrides.outputDirectory());

        PipelineRequest request = AppOrchestrator.createRequest(overrides, "run-7");
        assertEquals("run-7", request.runId());
        assertEquals("run-7", request.extraction().runId());
        assertEquals(overrides.repositoryUrl(), request.repository().repositoryUrl());
        assertEquals(overrides.outputDirectory(), request.report().outputDirectory());

        AppOrchestrator.CliOptions sameOptions = AppOrchestrator.parseArguments(new String[]{
                "https://example.test/repository", "binary.model", "multi.model", "reports"});
        assertEquals(AppOrchestrator.runIdFor(overrides), AppOrchestrator.runIdFor(sameOptions));
        assertEquals(PipelineApplicationService.AUTOMATIC_RUN_ID,
                AppOrchestrator.createRequest(overrides).runId());
        assertNotEquals(AppOrchestrator.runIdFor(overrides), AppOrchestrator.runIdFor(
                AppOrchestrator.parseArguments(new String[]{
                        "https://example.test/repository", "other-binary.model", "multi.model", "reports"})));

        AppOrchestrator.CliOptions explicit = AppOrchestrator.parseArguments(new String[]{
                "https://example.test/repository", "binary.model", "multi.model", "reports", "fixed-run", "main"});
        assertEquals("fixed-run", explicit.runIdOverride());
        assertEquals("main", explicit.revision());
        assertEquals("fixed-run", AppOrchestrator.createRequest(explicit).runId());
        assertEquals("main", AppOrchestrator.createRequest(explicit).repository().revision());
    }

    @Test
    void distinguishesAutomaticDefaultFromExplicitAutoRunId() {
        PipelineRequest automatic = AppOrchestrator.createRequest(
                AppOrchestrator.parseArguments(new String[0]));
        AppOrchestrator.CliOptions explicitAuto = AppOrchestrator.parseArguments(new String[]{
                "https://example.test/repository", "binary.model", "multi.model", "reports", "auto", "main"});
        PipelineRequest explicit = AppOrchestrator.createRequest(explicitAuto);

        assertEquals(PipelineApplicationService.AUTOMATIC_RUN_ID, automatic.runId());
        assertEquals("auto", explicit.runId());
        assertEquals("main", explicit.repository().revision());
    }

    @Test
    void identicalArgumentsProduceIdenticalArtifactContent() throws Exception {
        AppOrchestrator.CliOptions options = AppOrchestrator.parseArguments(new String[]{
                "https://example.test/repository", "binary.model", "multi.model", "reports"});
        String runId = AppOrchestrator.createRequest(options).runId();
        PipelineResult result = result(runId, RunStatus.COMPLETED);
        Path firstOutput = temporaryDirectory.resolve("first");
        Path secondOutput = temporaryDirectory.resolve("second");

        new FileReportSink().write(result, new ReportOptions(firstOutput));
        new FileReportSink().write(result, new ReportOptions(secondOutput));

        for (String fileName : List.of("debt-report.json", "debt-test-report.json", "debt-test-report.md")) {
            assertEquals(
                    Files.readString(firstOutput.resolve(fileName), StandardCharsets.UTF_8),
                    Files.readString(secondOutput.resolve(fileName), StandardCharsets.UTF_8));
        }
    }

    @Test
    void changesToRevisionOrResolvedLlmIdentityChangeDefaultRunIdWithoutUsingApiKeys() {
        AppOrchestrator.CliOptions main = AppOrchestrator.parseArguments(new String[]{
                "https://example.test/repository", "binary.model", "multi.model", "reports", "", "main"});
        AppOrchestrator.CliOptions release = AppOrchestrator.parseArguments(new String[]{
                "https://example.test/repository", "binary.model", "multi.model", "reports", "", "release"});
        LlmConfig openAi = new LlmConfig(
                "openai", "synthetic-api-key", "gpt-test", "https://llm.example.test/v1?token=endpoint-token#fragment");
        LlmConfig anthropic = new LlmConfig(
                "anthropic", "synthetic-api-key", "gpt-test", "https://llm.example.test/v1?token=endpoint-token#fragment");
        LlmConfig differentModel = new LlmConfig(
                "openai", "synthetic-api-key", "other-model", "https://llm.example.test/v1?token=endpoint-token#fragment");
        LlmConfig differentEndpoint = new LlmConfig(
                "openai", "synthetic-api-key", "gpt-test", "https://other-llm.example.test/v1");

        String mainId = AppOrchestrator.runIdFor(main, openAi);
        assertEquals(mainId, AppOrchestrator.runIdFor(main, openAi));
        assertNotEquals(mainId, AppOrchestrator.runIdFor(release, openAi));
        assertNotEquals(mainId, AppOrchestrator.runIdFor(main, anthropic));
        assertNotEquals(mainId, AppOrchestrator.runIdFor(main, differentModel));
        assertNotEquals(mainId, AppOrchestrator.runIdFor(main, differentEndpoint));
        assertEquals(mainId, AppOrchestrator.runIdFor(main, new LlmConfig(
                "openai", "different-api-key", "gpt-test",
                "https://llm.example.test/v1?token=endpoint-token#fragment")));
        assertNotEquals(mainId, AppOrchestrator.runIdFor(main, openAi,
                new RepositoryWorkspace(Path.of("workspace"), "https://example.test/repository", "resolved-commit")));
        assertFalse(mainId.contains("synthetic-api-key"));
        assertFalse(mainId.contains("endpoint-token"));
    }

    @Test
    void changesToModelContentsChangeTheDefaultRunId() throws Exception {
        Path binary = temporaryDirectory.resolve("binary.model");
        Path multi = temporaryDirectory.resolve("multi.model");
        Files.writeString(binary, "binary-v1", StandardCharsets.UTF_8);
        Files.writeString(multi, "multi-v1", StandardCharsets.UTF_8);
        AppOrchestrator.CliOptions options = AppOrchestrator.parseArguments(new String[]{
                "https://example.test/repository", binary.toString(), multi.toString(), "reports", "", "main"});
        LlmConfig config = new LlmConfig("openai", "synthetic-api-key", "gpt-test", "");

        String initial = AppOrchestrator.runIdFor(options, config);
        Files.writeString(binary, "binary-v2", StandardCharsets.UTF_8);
        String changedBinary = AppOrchestrator.runIdFor(options, config);
        Files.writeString(multi, "multi-v2", StandardCharsets.UTF_8);
        String changedMulti = AppOrchestrator.runIdFor(options, config);

        assertNotEquals(initial, changedBinary);
        assertNotEquals(changedBinary, changedMulti);
        assertFalse(initial.contains("synthetic-api-key"));
    }

    @Test
    void executePipelineMapsPipelineStatusesToExitCodes() {
        AppOrchestrator.CliOptions options = AppOrchestrator.parseArguments(new String[0]);
        List<String> runIds = new ArrayList<>();
        ExitCode completed = AppOrchestrator.executePipeline(options, request -> {
            runIds.add(request.runId());
            return result(request.runId(), RunStatus.COMPLETED);
        });
        ExitCode completedWithErrors = AppOrchestrator.executePipeline(options, request ->
                result(request.runId(), RunStatus.COMPLETED_WITH_ERRORS));
        ExitCode failed = AppOrchestrator.executePipeline(options, request ->
                result(request.runId(), RunStatus.FAILED));

        assertEquals(ExitCode.SUCCESS, completed);
        assertEquals(ExitCode.FATAL_ERROR, completedWithErrors);
        assertEquals(ExitCode.FATAL_ERROR, failed);
        assertEquals(1, runIds.size());
        assertEquals(PipelineApplicationService.AUTOMATIC_RUN_ID, runIds.get(0));
    }

    @Test
    void executeFromCliReturnsAnInvalidArgumentExitCodeWithoutInvokingThePipeline() {
        assertEquals(ExitCode.INVALID_ARGUMENTS,
                AppOrchestrator.executeFromCli(new String[]{"repo", "binary", "multi", "output", "run", "revision", "extra"}));
    }

    @Test
    void buildsThePipelineWithConcreteAdaptersWithoutStartingAClone() throws Exception {
        AppOrchestrator.CliOptions options = AppOrchestrator.parseArguments(new String[]{
                "https://example.test/repository", "missing-binary.model", "missing-multi.model", "reports"});
        PipelineApplicationService application = AppOrchestrator.createApplication(
                options, new LlmConfig("openai", "", "gpt-test", ""));

        assertNotNull(application);
        assertEquals(GitCloneService.class, implementationType(application, "workspaceProvider"));
        assertEquals(AstCommentExtractor.class, implementationType(application, "extractor"));
        assertEquals(WekaDebtHunterClassifier.class, implementationType(application, "classifier"));
        assertEquals(ContextHandler.class, implementationType(application, "contextEnricher"));
        assertEquals(io.github.jonasfortes12.tester.TestGeneratorService.class,
                implementationType(application, "testGenerator"));
        assertEquals(io.github.jonasfortes12.orchestrator.reporting.FileReportSink.class,
                implementationType(application, "reportSink"));
    }

    private static Class<?> implementationType(PipelineApplicationService application, String fieldName)
            throws Exception {
        Field field = PipelineApplicationService.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        Object value = field.get(application);
        return value.getClass();
    }

    private static PipelineResult result(String runId, RunStatus status) {
        return new PipelineResult(runId, status, null, List.of(), List.of(), null);
    }
}
