package io.github.jonasfortes12.orchestrator;

import io.github.jonasfortes12.classifier.WekaDebtHunterClassifier;
import io.github.jonasfortes12.context.chain.ContextHandler;
import io.github.jonasfortes12.context.extraction.IssueReferenceExtractor;
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
import io.github.jonasfortes12.orchestrator.reporting.FileReportSink;
import io.github.jonasfortes12.tester.LlmConfig;
import io.github.jonasfortes12.tester.TestGeneratorService;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Objects;

public final class AppOrchestrator {

    private static final String DEFAULT_REPOSITORY_URL = "https://github.com/apache/dubbo";
    private static final String DEFAULT_BINARY_MODEL = "preTrainedModels/DHbinaryClassifier.model";
    private static final String DEFAULT_MULTI_MODEL = "preTrainedModels/DHmultiClassifier.model";
    private static final Path DEFAULT_OUTPUT_DIRECTORY = Path.of("output");

    private AppOrchestrator() {
    }

    public static void main(String[] args) {
        System.exit(run(args));
    }

    public static int run(String[] args) {
        CliOptions options;
        try {
            options = parseArguments(args);
        } catch (IllegalArgumentException ignored) {
            System.out.println("status=FAILED");
            return 2;
        }
        try {
            LlmConfig llmConfig = new LlmConfig();
            PipelineApplicationService application = createApplication(options, llmConfig);
            return run(options, application::run);
        } catch (RuntimeException ignored) {
            System.out.println("status=FAILED");
            return 1;
        }
    }

    public static int run(String[] args, PipelineExecutor executor) {
        CliOptions options;
        try {
            options = parseArguments(args);
        } catch (IllegalArgumentException ignored) {
            System.out.println("status=FAILED");
            return 2;
        }
        try {
            return run(options, executor);
        } catch (RuntimeException ignored) {
            System.out.println("status=FAILED");
            return 1;
        }
    }

    private static int run(CliOptions options, PipelineExecutor executor) {
        PipelineResult result = Objects.requireNonNull(
                executor.execute(createRequest(options)),
                "pipeline result must not be null");
        System.out.println("status=" + result.status());
        if (result.reportArtifact() != null) {
            for (Path path : result.reportArtifact().paths()) {
                System.out.println("artifact=" + path);
            }
        }
        return result.status() == RunStatus.COMPLETED ? 0 : 1;
    }

    public static CliOptions parseArguments(String[] args) {
        if (args == null || args.length > 6) {
            throw new IllegalArgumentException("expected at most six optional arguments");
        }
        return new CliOptions(
                argumentOrDefault(args, 0, DEFAULT_REPOSITORY_URL),
                argumentOrDefault(args, 1, DEFAULT_BINARY_MODEL),
                argumentOrDefault(args, 2, DEFAULT_MULTI_MODEL),
                Path.of(argumentOrDefault(args, 3, DEFAULT_OUTPUT_DIRECTORY.toString())),
                argumentOrDefault(args, 4, null),
                argumentOrDefault(args, 5, null));
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
                valueOrEmpty(revision),
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
            StringBuilder runId = new StringBuilder(digest.length * 2);
            for (byte value : digest) {
                runId.append(String.format("%02x", value));
            }
            return runId.toString();
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
        Objects.requireNonNull(options, "options must not be null");
        Objects.requireNonNull(llmConfig, "llmConfig must not be null");
        return createApplication(
                new FileReportSink(),
                new GitCloneService(),
                new AstCommentExtractor(),
                new WekaDebtHunterClassifier(options.binaryModelPath(), options.multiModelPath()),
                new ContextHandler(new IssueReferenceExtractor(), List.of()),
                new TestGeneratorService(llmConfig),
                (request, workspace) -> request.runId().equals(PipelineApplicationService.AUTOMATIC_RUN_ID)
                        ? runIdFor(options, llmConfig, workspace)
                        : request.runId());
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

    private static String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }

    private static String modelDigest(String modelPath) {
        try (InputStream input = Files.newInputStream(Path.of(modelPath))) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int read;
            while ((read = input.read(buffer)) >= 0) {
                digest.update(buffer, 0, read);
            }
            return hex(digest.digest());
        } catch (IOException | NoSuchAlgorithmException | RuntimeException ignored) {
            return "unavailable";
        }
    }

    private static String hex(byte[] digest) {
        StringBuilder result = new StringBuilder(digest.length * 2);
        for (byte value : digest) {
            result.append(String.format("%02x", value));
        }
        return result.toString();
    }

    private static String argumentOrDefault(String[] args, int index, String defaultValue) {
        return index < args.length && args[index] != null && !args[index].isBlank()
                ? args[index]
                : defaultValue;
    }
}
