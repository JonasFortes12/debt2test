package io.github.jonasfortes12.orchestrator;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import io.github.jonasfortes12.classifier.WekaDebtHunterClassifier;
import io.github.jonasfortes12.context.chain.ContextHandler;
import io.github.jonasfortes12.context.extraction.IssueReferenceExtractor;
import io.github.jonasfortes12.context.provider.ContextProvider;
import io.github.jonasfortes12.context.provider.jira.JiraClient;
import io.github.jonasfortes12.context.provider.jira.JiraConfig;
import io.github.jonasfortes12.context.provider.jira.JiraContextProvider;
import io.github.jonasfortes12.core.model.ClassificationOptions;
import io.github.jonasfortes12.core.model.ContextRequest;
import io.github.jonasfortes12.core.model.ExtractionOptions;
import io.github.jonasfortes12.core.model.PipelineRequest;
import io.github.jonasfortes12.core.model.PipelineResult;
import io.github.jonasfortes12.core.model.ReportOptions;
import io.github.jonasfortes12.core.model.RepositoryRequest;
import io.github.jonasfortes12.core.model.RepositoryWorkspace;
import io.github.jonasfortes12.core.model.RunStatus;
import io.github.jonasfortes12.core.model.TestGenerationOptions;
import io.github.jonasfortes12.core.port.ContextEnricher;
import io.github.jonasfortes12.core.port.DebtClassifier;
import io.github.jonasfortes12.core.port.ReportSink;
import io.github.jonasfortes12.core.port.RepositoryWorkspaceProvider;
import io.github.jonasfortes12.core.port.SatdExtractor;
import io.github.jonasfortes12.core.port.TestGenerator;
import io.github.jonasfortes12.core.util.UrlSanitizer;
import io.github.jonasfortes12.extractor.AstCommentExtractor;
import io.github.jonasfortes12.extractor.GitCloneService;
import io.github.jonasfortes12.orchestrator.application.PipelineApplicationService;
import io.github.jonasfortes12.core.port.PipelineRunStore;
import io.github.jonasfortes12.orchestrator.persistence.PersistenceContext;
import io.github.jonasfortes12.orchestrator.reporting.CompositeReportSink;
import io.github.jonasfortes12.orchestrator.reporting.FileReportSink;
import io.github.jonasfortes12.orchestrator.util.OrchestrationUtils;
import io.github.jonasfortes12.tester.LlmConfig;
import io.github.jonasfortes12.tester.TestGeneratorService;

public final class AppOrchestrator {

    private static final String DEFAULT_REPOSITORY_URL = "https://github.com/JonasFortes12/mock-debt-project";
    private static final String DEFAULT_BINARY_MODEL = "preTrainedModels/DHbinaryClassifier.model";
    private static final String DEFAULT_MULTI_MODEL = "preTrainedModels/DHmultiClassifier.model";
    private static final Path DEFAULT_OUTPUT_DIRECTORY = Path.of("output");

    private AppOrchestrator() {
    }

    public static void main(String[] args) {
        System.exit(executeFromCli(args).code());
    }

    public static ExitCode executeFromCli(String[] args) {
        try {
            BootstrappedPipeline pipeline = CliBootstrapper.bootstrap(args);
            try {
                return executePipeline(pipeline.options(), pipeline.application()::run);
            } finally {
                pipeline.persistence().ifPresent(PersistenceContext::close);
            }
        } catch (IllegalArgumentException ignored) {
            return reportFailure(ExitCode.INVALID_ARGUMENTS);
        } catch (RuntimeException ignored) {
            return reportFailure(ExitCode.FATAL_ERROR);
        }
    }

    private static ExitCode reportFailure(ExitCode exitCode) {
        System.out.println("status=FAILED");
        System.out.println(exitCode.description());
        return exitCode;
    }

    static ExitCode executePipeline(CliOptions options, PipelineExecutor executor) {
        PipelineResult result = Objects.requireNonNull(
                executor.execute(createRequest(options)),
                "pipeline result must not be null");
        System.out.println("status=" + result.status());
        if (result.reportArtifact() != null) {
            for (Path path : result.reportArtifact().paths()) {
                System.out.println("artifact=" + path);
            }
        }
        ExitCode exitCode = result.status() == RunStatus.COMPLETED ? ExitCode.SUCCESS : ExitCode.FATAL_ERROR;
        System.out.println(exitCode.description());
        return exitCode;
    }

    public static CliOptions parseArguments(String[] args) {
        if (args == null || args.length > 6) {
            throw new IllegalArgumentException("expected at most six optional arguments");
        }
        return new CliOptions(
                OrchestrationUtils.argumentOrDefault(args, 0, DEFAULT_REPOSITORY_URL),
                OrchestrationUtils.argumentOrDefault(args, 1, DEFAULT_BINARY_MODEL),
                OrchestrationUtils.argumentOrDefault(args, 2, DEFAULT_MULTI_MODEL),
                Path.of(OrchestrationUtils.argumentOrDefault(args, 3, DEFAULT_OUTPUT_DIRECTORY.toString())),
                OrchestrationUtils.argumentOrDefault(args, 4, null),
                OrchestrationUtils.argumentOrDefault(args, 5, null));
    }

    public static String runIdFor(CliOptions options) {
        return runIdFor(options, new LlmConfig());
    }

    public static String runIdFor(CliOptions options, LlmConfig llmConfig) {
        return runIdFor(options, llmConfig, null);
    }

    public static String runIdFor(
            CliOptions options, LlmConfig llmConfig, RepositoryWorkspace workspace) {
        Objects.requireNonNull(options, "options must not be null");
        if (options.runIdOverride() != null) {
            return options.runIdOverride();
        }
        Objects.requireNonNull(llmConfig, "llmConfig must not be null");
        String repositoryUrl = workspace == null ? options.repositoryUrl() : workspace.repositoryUrl();
        String revision = workspace == null ? options.revision() : workspace.revision();
        String material = String.join("\n",
                UrlSanitizer.sanitize(repositoryUrl),
                OrchestrationUtils.valueOrEmpty(revision),
                options.binaryModelPath(),
                modelDigest(options.binaryModelPath()),
                options.multiModelPath(),
                modelDigest(options.multiModelPath()),
                llmConfig.getProvider().toLowerCase(java.util.Locale.ROOT),
                llmConfig.getModel(),
                resolvedEndpoint(llmConfig),
                "allowHeuristicFallback=true",
                "contextEnabled=true",
                "framework=JUnit 5",
                "promptVersion=v1");
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(material.getBytes(StandardCharsets.UTF_8));
            return OrchestrationUtils.toHexString(digest);
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is unavailable", impossible);
        }
    }

    public static PipelineRequest createRequest(CliOptions options) {
        return createRequest(options, options.runIdOverride() == null
                ? PipelineApplicationService.AUTOMATIC_RUN_ID
                : options.runIdOverride());
    }

    public static PipelineRequest createRequest(CliOptions options, String runId) {
        Objects.requireNonNull(options, "options must not be null");
        Objects.requireNonNull(runId, "runId must not be null");
        return new PipelineRequest(
                runId,
                new RepositoryRequest(options.repositoryUrl(), options.revision()),
                new ExtractionOptions(runId),
                new ClassificationOptions(true),
                new ContextRequest(true),
                new TestGenerationOptions("JUnit 5", "v1"),
                new ReportOptions(options.outputDirectory()));
    }

    public static PipelineApplicationService createApplication(CliOptions options, LlmConfig llmConfig) {
        return createApplication(options, llmConfig, Optional.empty());
    }

    public static PipelineApplicationService createApplication(
            CliOptions options, LlmConfig llmConfig, Optional<PersistenceContext> persistence) {
        Objects.requireNonNull(options, "options must not be null");
        Objects.requireNonNull(llmConfig, "llmConfig must not be null");
        Objects.requireNonNull(persistence, "persistence must not be null");

        ReportSink fileSink = new FileReportSink();
        ReportSink reportSink = persistence
                .map(context -> (ReportSink) new CompositeReportSink(fileSink, List.of(context.reportSink())))
                .orElse(fileSink);
        PipelineRunStore runStore = persistence
                .map(PersistenceContext::runStore)
                .orElse(PipelineRunStore.NO_OP);

        return new PipelineApplicationService(
                new GitCloneService(),
                new AstCommentExtractor(),
                new WekaDebtHunterClassifier(options.binaryModelPath(), options.multiModelPath()),
                new ContextHandler(new IssueReferenceExtractor(), jiraContextProvider()),
                new TestGeneratorService(llmConfig),
                reportSink,
                (request, workspace) -> request.runId().equals(PipelineApplicationService.AUTOMATIC_RUN_ID)
                        ? runIdFor(options, llmConfig, workspace)
                        : request.runId(),
                runStore);
    }

    private static Optional<ContextProvider> jiraContextProvider() {
        JiraConfig jiraConfig = new JiraConfig();
        if (!jiraConfig.isConfigured()) {
            return Optional.empty();
        }
        JiraClient client = new JiraClient(jiraConfig.getBaseUrl(), jiraConfig.getEmail(), jiraConfig.getApiToken());
        return Optional.of(new JiraContextProvider(client));
    }

    public static PipelineApplicationService createApplication(
            ReportSink reportSink,
            RepositoryWorkspaceProvider workspaceProvider,
            SatdExtractor extractor,
            DebtClassifier classifier,
            ContextEnricher contextEnricher,
            TestGenerator testGenerator) {
        return createApplication(
                reportSink, workspaceProvider, extractor, classifier, contextEnricher, testGenerator,
                (request, workspace) -> request.runId());
    }

    public static PipelineApplicationService createApplication(
            ReportSink reportSink,
            RepositoryWorkspaceProvider workspaceProvider,
            SatdExtractor extractor,
            DebtClassifier classifier,
            ContextEnricher contextEnricher,
            TestGenerator testGenerator,
            PipelineApplicationService.RunIdResolver runIdResolver) {
        return new PipelineApplicationService(
                workspaceProvider,
                extractor,
                classifier,
                contextEnricher,
                testGenerator,
                reportSink,
                runIdResolver);
    }

    public record CliOptions(
            String repositoryUrl,
            String binaryModelPath,
            String multiModelPath,
            Path outputDirectory,
            String runIdOverride,
            String revision) {

        public CliOptions(String repositoryUrl, String binaryModelPath, String multiModelPath, Path outputDirectory) {
            this(repositoryUrl, binaryModelPath, multiModelPath, outputDirectory, null, null);
        }

        public CliOptions(
                String repositoryUrl,
                String binaryModelPath,
                String multiModelPath,
                Path outputDirectory,
                String runIdOverride) {
            this(repositoryUrl, binaryModelPath, multiModelPath, outputDirectory, runIdOverride, null);
        }

        public CliOptions {
            if (repositoryUrl == null || repositoryUrl.isBlank()
                    || binaryModelPath == null || binaryModelPath.isBlank()
                    || multiModelPath == null || multiModelPath.isBlank()
                    || outputDirectory == null) {
                throw new IllegalArgumentException("CLI options must be complete");
            }
            if (runIdOverride != null && runIdOverride.isBlank()) {
                throw new IllegalArgumentException("run ID override must not be blank");
            }
        }
    }

    @FunctionalInterface
    public interface PipelineExecutor {
        PipelineResult execute(PipelineRequest request);
    }

    private static String resolvedEndpoint(LlmConfig llmConfig) {
        String endpoint = llmConfig.getEndpoint();
        if (endpoint.isBlank()) {
            endpoint = switch (llmConfig.getProvider().toLowerCase(java.util.Locale.ROOT)) {
                case "anthropic" -> "https://api.anthropic.com/v1/messages";
                case "gemini" -> "https://generativelanguage.googleapis.com/v1beta";
                default -> "https://api.openai.com/v1/chat/completions";
            };
        }
        return UrlSanitizer.sanitize(endpoint);
    }

    private static String modelDigest(String modelPath) {
        try (InputStream input = Files.newInputStream(Path.of(modelPath))) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int read;
            while ((read = input.read(buffer)) >= 0) {
                digest.update(buffer, 0, read);
            }
            return OrchestrationUtils.toHexString(digest.digest());
        } catch (IOException | NoSuchAlgorithmException | RuntimeException ignored) {
            return "unavailable";
        }
    }
}
