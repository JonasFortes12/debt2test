package io.github.jonasfortes12.orchestrator.reporting;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import io.github.jonasfortes12.core.error.PipelineException;
import io.github.jonasfortes12.core.model.ClassifiedDebt;
import io.github.jonasfortes12.core.model.EnrichedSatdDebt;
import io.github.jonasfortes12.core.model.ExternalReference;
import io.github.jonasfortes12.core.model.ExternalTaskSpec;
import io.github.jonasfortes12.core.model.GeneratedTest;
import io.github.jonasfortes12.core.model.PipelineError;
import io.github.jonasfortes12.core.model.PipelineItemResult;
import io.github.jonasfortes12.core.model.PipelineResult;
import io.github.jonasfortes12.core.model.Provenance;
import io.github.jonasfortes12.core.model.ReportArtifact;
import io.github.jonasfortes12.core.model.ReportOptions;
import io.github.jonasfortes12.core.port.ReportSink;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Pattern;

public final class FileReportSink implements ReportSink {

    private static final String DEBT_REPORT_NAME = "debt-report.json";
    private static final String TEST_REPORT_NAME = "debt-test-report.json";
    private static final String MARKDOWN_REPORT_NAME = "debt-test-report.md";
    private static final List<String> RECOVERY_PREFIXES = List.of(
            ".debt2test-report-staging-", ".report-backup-", ".debt2test-report-journal-");
    private static final String REPORT_ERROR_MESSAGE = "pipeline error recorded";
    private static final Set<String> REPORT_STAGES = Set.of(
            "repository", "extraction", "classification", "context", "generation", "orchestration", "report");
    private static final Set<String> REPORT_CODES = Set.of(
            "WORKSPACE_PREPARATION_FAILED", "WORKSPACE_CLEANUP_FAILED", "WORKSPACE_RELEASE_FAILED",
            "JAVA_TRAVERSAL_FAILED", "JAVA_PARSE_FAILED", "CANDIDATE_INVALID",
            "CLASSIFIER_MODEL_FALLBACK", "CLASSIFIER_MODEL_UNAVAILABLE", "CLASSIFIER_PREDICTION_FAILED",
            "LLM_REQUEST_INTERRUPTED", "LLM_TRANSPORT_FAILED", "LLM_REQUEST_INVALID", "LLM_RESPONSE_INVALID",
            "LLM_HTTP_BAD_REQUEST", "LLM_HTTP_UNAUTHORIZED", "LLM_HTTP_FORBIDDEN", "LLM_HTTP_RATE_LIMITED",
            "LLM_HTTP_SERVER_ERROR", "LLM_HTTP_FAILED", "LLM_GENERATION_FAILED",
            "EXTRACTION_STAGE_FAILED", "EXTRACTION_RESULT_MISSING", "EXTRACTION_CANDIDATES_MISSING",
            "EXTRACTION_NULL_RESULT_ENTRY", "EXTRACTION_ERRORS_MISSING", "DUPLICATE_CANDIDATE_ID",
            "CLASSIFICATION_STAGE_FAILED", "CLASSIFICATION_RESULT_MISSING", "CLASSIFICATION_VALUES_MISSING",
            "CLASSIFICATION_NULL_RESULT_ENTRY", "CLASSIFICATION_UNKNOWN_CANDIDATE",
            "DUPLICATE_CLASSIFICATION_ID", "CONTEXT_STAGE_FAILED", "CONTEXT_RESULT_MISSING",
            "CONTEXT_ERRORS_MISSING", "CONTEXT_ENRICHMENTS_MISSING", "CONTEXT_NULL_RESULT_ENTRY",
            "DUPLICATE_CONTEXT_RESULT_ID", "CONTEXT_UNKNOWN_CANDIDATE",
            "CONTEXT_NON_SATD_RESULT", "GENERATION_STAGE_FAILED", "GENERATION_RESULT_MISSING",
            "GENERATION_ERRORS_MISSING", "GENERATION_TESTS_MISSING", "GENERATION_NULL_RESULT_ENTRY",
            "DUPLICATE_GENERATION_RESULT_ID", "GENERATION_UNKNOWN_CANDIDATE", "GENERATION_NON_SATD_RESULT",
            "GENERATION_WITHOUT_CONTEXT", "REPORT_ARTIFACT_MISSING",
            "REPORT_WRITE_FAILED", "REPORT_ROLLBACK_FAILED", "RUN_ID_RESOLUTION_FAILED", "GENERATION_FAILED");
    private static final Pattern CONTEXT_PROVIDER_CODE = Pattern.compile(
            "CONTEXT_PROVIDER_[A-Z][A-Z0-9_-]{0,31}_(?:ID_FAILED|SUPPORTS_FAILED|FETCH_FAILED|NULL_RESOLUTION|TASK_MISMATCH|NOT_FOUND|RATE_LIMITED|UNAUTHORIZED|UNKNOWN)");
    private static final ConcurrentHashMap<Path, ReentrantLock> PROCESS_LOCKS = new ConcurrentHashMap<>();

    private final Gson gson = new GsonBuilder()
            .serializeNulls()
            .setPrettyPrinting()
            .create();

    @Override
    public ReportArtifact write(PipelineResult result, ReportOptions options) {
        Objects.requireNonNull(result, "result must not be null");
        Objects.requireNonNull(options, "options must not be null");

        Path outputDirectory = options.outputDirectory();
        Path normalizedOutputDirectory = realOutputIdentity(outputDirectory);
        ReentrantLock processLock = PROCESS_LOCKS.computeIfAbsent(
                normalizedOutputDirectory, ignored -> new ReentrantLock());
        processLock.lock();
        try {
            return writeWithCrossProcessLock(result, outputDirectory, normalizedOutputDirectory);
        } catch (PipelineException failure) {
            throw failure;
        } catch (IOException | RuntimeException failure) {
            throw reportFailure("REPORT_WRITE_FAILED", "pipeline reports could not be written");
        } finally {
            processLock.unlock();
        }
    }

    private ReportArtifact writeWithCrossProcessLock(
            PipelineResult result, Path outputDirectory, Path normalizedOutputDirectory) throws IOException {
        Files.createDirectories(outputDirectory);
        Path lockPath = Path.of(
                System.getProperty("java.io.tmpdir"),
                ".debt2test-report-" + sha256(normalizedOutputDirectory.toString()) + ".lock");
        try (FileChannel channel = FileChannel.open(lockPath, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
                FileLock ignored = channel.lock()) {
            cleanupStaleState(outputDirectory);
            return writeUnderLock(result, outputDirectory);
        }
    }

    private static Path realOutputIdentity(Path outputDirectory) {
        Path absolute = outputDirectory.toAbsolutePath().normalize();
        List<String> missingComponents = new ArrayList<>();
        Path current = absolute;
        while (current != null && !Files.exists(current, LinkOption.NOFOLLOW_LINKS)) {
            if (current.getFileName() != null) {
                missingComponents.add(0, current.getFileName().toString());
            }
            current = current.getParent();
        }
        if (current == null) {
            return absolute;
        }
        try {
            Path identity = current.toRealPath();
            for (String component : missingComponents) {
                identity = identity.resolve(component);
            }
            return identity.normalize();
        } catch (IOException | RuntimeException ignored) {
            return absolute;
        }
    }

    private static void cleanupStaleState(Path outputDirectory) throws IOException {
        try (var entries = Files.list(outputDirectory)) {
            for (Path entry : entries.filter(FileReportSink::isRecoveryState).toList()) {
                if (!deleteDirectory(entry)) {
                    throw new IOException("stale report state could not be removed");
                }
            }
        }
    }

    private static boolean isRecoveryState(Path path) {
        String name = path.getFileName().toString();
        return RECOVERY_PREFIXES.stream().anyMatch(name::startsWith);
    }

    private ReportArtifact writeUnderLock(PipelineResult result, Path outputDirectory) throws IOException {
        Path debtReport = outputDirectory.resolve(DEBT_REPORT_NAME);
        Path testReport = outputDirectory.resolve(TEST_REPORT_NAME);
        Path markdownReport = outputDirectory.resolve(MARKDOWN_REPORT_NAME);
        List<Path> finalPaths = List.of(debtReport, testReport, markdownReport);
        List<Backup> backups = new ArrayList<>();
        List<Path> promotedPaths = new ArrayList<>();
        Path stagingDirectory = null;
        try {
            // Three public files cannot be committed atomically as one filesystem object. The lock serializes
            // writers, startup recovery removes stale transaction state, staging prevents partial rendering,
            // and each replacement is atomic where supported. A crash can still leave different generations in
            // the three files; this does not claim reader-atomicity.
            stagingDirectory = Files.createTempDirectory(outputDirectory, ".debt2test-report-staging-");
            List<PipelineItemResult> satdItems = result.items().stream()
                    .filter(item -> item != null && item.classification() != null && item.classification().satd())
                    .toList();
            List<DebtReportItem> debtItems = satdItems.stream()
                    .map(item -> debtItem(result, item))
                    .toList();
            List<TestReportItem> testItems = satdItems.stream()
                    .map(item -> testItem(result, item))
                    .toList();

            String debtJson = jsonReport(debtItems, result);
            String testJson = jsonReport(testItems, result);
            String markdown = markdown(result, testItems);
            validateJson(debtJson);
            validateJson(testJson);
            validateText(markdown);

            List<Path> temporaryPaths = List.of(
                    writeTemporary(stagingDirectory, ".debt-report-", debtJson),
                    writeTemporary(stagingDirectory, ".debt-test-report-", testJson),
                    writeTemporary(stagingDirectory, ".debt-test-report-", markdown));
            backupExisting(finalPaths, stagingDirectory, backups);
            for (int index = 0; index < finalPaths.size(); index++) {
                moveReplacing(temporaryPaths.get(index), finalPaths.get(index));
                promotedPaths.add(finalPaths.get(index));
            }
            validateFiles(finalPaths);
            if (!deleteDirectory(stagingDirectory)) {
                throw new IOException("report staging cleanup failed");
            }
            stagingDirectory = null;
            return new ReportArtifact(finalPaths);
        } catch (IOException | RuntimeException failure) {
            boolean rollbackFailed = rollback(promotedPaths, backups);
            boolean cleanupFailed = stagingDirectory != null && !deleteDirectory(stagingDirectory);
            throw rollbackFailed || cleanupFailed
                    ? reportFailure("REPORT_ROLLBACK_FAILED", "pipeline report rollback could not be completed")
                    : reportFailure("REPORT_WRITE_FAILED", "pipeline reports could not be written");
        }
    }

    private String jsonReport(List<?> items, PipelineResult result) {
        if (result.status() != io.github.jonasfortes12.core.model.RunStatus.COMPLETED
                || !result.errors().isEmpty()) {
            return gson.toJson(new ReportEnvelope(
                    result.runId(), result.status().name(), reportErrors(result.errors()), items));
        }
        return gson.toJson(items);
    }

    private static void validateJson(String content) {
        validateText(content);
        JsonParser.parseString(content);
    }

    private static void validateText(String content) {
        if (content == null || content.isBlank()) {
            throw new IllegalStateException("report content is empty");
        }
    }

    private static Path writeTemporary(Path outputDirectory, String prefix, String content) throws IOException {
        Path temporary = Files.createTempFile(outputDirectory, prefix, ".tmp");
        Files.writeString(temporary, content, StandardCharsets.UTF_8);
        validateFiles(List.of(temporary));
        return temporary;
    }

    private static void backupExisting(
            List<Path> finalPaths, Path stagingDirectory, List<Backup> backups) throws IOException {
        for (Path finalPath : finalPaths) {
            if (!Files.exists(finalPath, LinkOption.NOFOLLOW_LINKS)
                    || Files.isDirectory(finalPath, LinkOption.NOFOLLOW_LINKS)) {
                continue;
            }
            Path backupPath = Files.createTempFile(stagingDirectory, ".report-backup-", ".tmp");
            try {
                Files.copy(finalPath, backupPath,
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING,
                        LinkOption.NOFOLLOW_LINKS);
                backups.add(new Backup(finalPath, backupPath));
            } catch (IOException | RuntimeException failure) {
                try {
                    Files.deleteIfExists(backupPath);
                } catch (IOException | RuntimeException ignored) {
                    // Cleanup remains best effort after a failed backup.
                }
                throw failure;
            }
        }
    }

    private static void moveReplacing(Path source, Path target) throws IOException {
        try {
            Files.move(source, target,
                    java.nio.file.StandardCopyOption.ATOMIC_MOVE,
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException unsupported) {
            Files.move(source, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static void validateFiles(List<Path> paths) throws IOException {
        for (Path path : paths) {
            if (path == null || !Files.exists(path) || !Files.isRegularFile(path)) {
                throw new IOException("report artifact is not a regular file");
            }
            if (Files.size(path) == 0) {
                throw new IOException("report artifact is empty");
            }
        }
    }

    private static boolean rollback(List<Path> promotedPaths, List<Backup> backups) {
        boolean failed = false;
        for (int index = promotedPaths.size() - 1; index >= 0; index--) {
            Path promoted = promotedPaths.get(index);
            Backup backup = backups.stream()
                    .filter(candidate -> candidate.finalPath().equals(promoted))
                    .findFirst()
                    .orElse(null);
            try {
                if (backup == null) {
                    Files.deleteIfExists(promoted);
                } else {
                    moveReplacing(backup.backupPath(), backup.finalPath());
                }
            } catch (IOException | RuntimeException ignored) {
                failed = true;
            }
        }
        return failed;
    }

    private static boolean deleteDirectory(Path directory) {
        if (directory == null) {
            return true;
        }
        try (var paths = Files.walk(directory)) {
            paths.sorted(java.util.Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException exception) {
                    throw new java.io.UncheckedIOException(exception);
                }
            });
            return true;
        } catch (IOException | RuntimeException ignored) {
            return false;
        }
    }

    private static String sha256(String value) {
        try {
            byte[] digest = java.security.MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(digest.length * 2);
            for (byte current : digest) {
                result.append(String.format("%02x", current));
            }
            return result.toString();
        } catch (java.security.NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is unavailable", impossible);
        }
    }

    private DebtReportItem debtItem(PipelineResult result, PipelineItemResult item) {
        ClassifiedDebt classification = item.classification();
        EnrichedSatdDebt enrichment = item.enrichment();
        return new DebtReportItem(
                item.candidate().filePath(),
                item.candidate().methodName(),
                item.candidate().lineNumber(),
                item.candidate().comment(),
                item.candidate().methodSourceCode(),
                classification.satd(),
                classification.debtType(),
                classification.confidence(),
                item.candidate().candidateId(),
                enrichment == null || enrichment.contextStatus() == null
                        ? null
                        : enrichment.contextStatus().name(),
                references(enrichment),
                externalTask(enrichment),
                provenance(classification.provenance()),
                errors(item),
                result.runId(),
                ReportUrlSanitizer.sanitize(item.candidate().sourceProvenance().repositoryUrl()),
                item.candidate().sourceProvenance().revision());
    }

    private TestReportItem testItem(PipelineResult result, PipelineItemResult item) {
        DebtReportItem debtItem = debtItem(result, item);
        GeneratedTest generated = item.generatedTest();
        return new TestReportItem(
                debtItem.filePath(),
                debtItem.methodName(),
                debtItem.lineNumber(),
                debtItem.comment(),
                debtItem.methodSourceCode(),
                debtItem.isSatd(),
                debtItem.debtType(),
                debtItem.confidence(),
                generated == null ? null : generated.sourceCode(),
                generated == null || generated.status() == null ? null : generated.status().name(),
                debtItem.candidateId(),
                debtItem.contextStatus(),
                debtItem.issueReferences(),
                debtItem.externalTask(),
                debtItem.classificationProvenance(),
                debtItem.errors(),
                generated == null ? null : generated.provider(),
                generated == null ? null : generated.model(),
                generated == null ? null : generated.promptVersion(),
                generated == null || generated.validationStatus() == null
                        ? null
                        : generated.validationStatus().name(),
                debtItem.runId(),
                debtItem.repositoryUrl(),
                debtItem.revision());
    }

    private static List<DebtReportItem.IssueReference> references(EnrichedSatdDebt enrichment) {
        if (enrichment == null || enrichment.references() == null) {
            return List.of();
        }
        return enrichment.references().stream()
                .filter(Objects::nonNull)
                .map(FileReportSink::reference)
                .toList();
    }

    private static DebtReportItem.IssueReference reference(ExternalReference reference) {
        return new DebtReportItem.IssueReference(ReportUrlSanitizer.sanitize(reference.value()), reference.source());
    }

    private static DebtReportItem.ExternalTask externalTask(EnrichedSatdDebt enrichment) {
        if (enrichment == null || enrichment.externalTask() == null) {
            return null;
        }
        ExternalTaskSpec task = enrichment.externalTask();
        return new DebtReportItem.ExternalTask(
                task.provider(),
                task.key(),
                task.summary(),
                task.description(),
                task.acceptanceCriteria(),
                task.labels(),
                ReportUrlSanitizer.sanitize(task.url()));
    }

    private static DebtReportItem.ClassificationProvenance provenance(Provenance provenance) {
        if (provenance == null) {
            return null;
        }
        return new DebtReportItem.ClassificationProvenance(
                provenance.provider(), provenance.strategy(), provenance.version());
    }

    private static List<DebtReportItem.ReportError> errors(PipelineItemResult item) {
        List<DebtReportItem.ReportError> errors = new ArrayList<>();
        if (item.errors() != null) {
            addErrors(errors, item.errors(), item.candidate().candidateId());
        }
        if (item.enrichment() != null) {
            addErrors(errors, item.enrichment().errors(), item.candidate().candidateId());
        }
        if (item.generatedTest() != null) {
            addErrors(errors, item.generatedTest().errors(), item.candidate().candidateId());
        }
        addErrors(errors, item.classification().errors(), item.candidate().candidateId());
        return List.copyOf(errors);
    }

    private static void addErrors(
            List<DebtReportItem.ReportError> target, List<PipelineError> source, String currentCandidateId) {
        if (source == null) {
            return;
        }
        for (PipelineError error : source) {
            if (error == null) {
                continue;
            }
            DebtReportItem.ReportError reportError = new DebtReportItem.ReportError(
                    normalizeStage(error.stage()),
                    normalizeCode(error.code()),
                    currentCandidateId,
                    error.recoverable(),
                    REPORT_ERROR_MESSAGE);
            if (!target.contains(reportError)) {
                target.add(reportError);
            }
        }
    }

    private static List<DebtReportItem.ReportError> reportErrors(List<PipelineError> source) {
        List<DebtReportItem.ReportError> errors = new ArrayList<>();
        addErrors(errors, source, null);
        return List.copyOf(errors);
    }

    private static String normalizeStage(String stage) {
        if (stage == null) {
            return "unknown";
        }
        String normalized = stage.trim().toLowerCase(Locale.ROOT);
        return REPORT_STAGES.contains(normalized) ? normalized : "unknown";
    }

    private static String normalizeCode(String code) {
        if (code == null) {
            return "UNKNOWN_ERROR";
        }
        String normalized = code.trim().toUpperCase(Locale.ROOT);
        return REPORT_CODES.contains(normalized) || CONTEXT_PROVIDER_CODE.matcher(normalized).matches()
                ? normalized
                : "UNKNOWN_ERROR";
    }

    private static String markdown(PipelineResult result, List<TestReportItem> items) {
        StringBuilder report = new StringBuilder()
                .append("# Technical Debt Test Generation Report\n\n")
                .append("Generated test cases to pay off self-admitted technical debt (SATD).\n\n");
        if (result.status() != io.github.jonasfortes12.core.model.RunStatus.COMPLETED
                || !result.errors().isEmpty()) {
            report.append("- **Run ID:** ").append(codeSpan(result.runId())).append("\n")
                    .append("- **Status:** ").append(codeSpan(result.status().name())).append("\n")
                    .append("- **Errors:**\n");
            for (DebtReportItem.ReportError error : reportErrors(result.errors())) {
                report.append("  - ").append(codeSpan(error.code())).append("\n");
            }
            report.append("\n");
        }
        for (TestReportItem item : items) {
            String fileName = Path.of(item.filePath()).getFileName().toString();
            report.append("## ")
                    .append(safeHeading(fileName))
                    .append(" -> ")
                    .append(safeHeading(item.methodName()))
                    .append("()\n\n")
                    .append("- **Debt Type:** ").append(codeSpan(item.debtType())).append("\n")
                    .append("- **Line Number:** ").append(codeSpan(Integer.toString(item.lineNumber()))).append("\n")
                    .append("- **Status:** ").append(codeSpan(statusForMarkdown(item))).append("\n")
                    .append("- **Comment:** ").append(codeSpan(item.comment())).append("\n\n")
                    .append(fencedCode(item.methodSourceCode()))
                    .append("\n\n")
                    .append("### Generated Test Case\n\n")
                    .append(fencedCode(item.generatedTestCode()))
                    .append("\n\n")
                    .append("---\n\n");
        }
        return report.toString();
    }

    private static String statusForMarkdown(TestReportItem item) {
        return item.status() == null ? "NOT_GENERATED" : item.status();
    }

    private static String safeHeading(String value) {
        return safePlainText(value)
                .replace("\\", "\\\\")
                .replace("`", "\\`")
                .replace("#", "\\#")
                .replace("*", "\\*")
                .replace("_", "\\_")
                .replace("[", "\\[")
                .replace("]", "\\]")
                .replace("<", "\\<")
                .replace(">", "\\>");
    }

    private static String codeSpan(String value) {
        String safe = safePlainText(value);
        String delimiter = "`".repeat(Math.max(1, longestBacktickRun(safe) + 1));
        return longestBacktickRun(safe) == 0
                ? delimiter + safe + delimiter
                : delimiter + " " + safe + " " + delimiter;
    }

    private static String fencedCode(String value) {
        String safe = safeCode(value);
        String delimiter = "`".repeat(Math.max(3, longestBacktickRun(safe) + 1));
        return delimiter + "java\n" + safe + "\n" + delimiter;
    }

    private static String safePlainText(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder safe = new StringBuilder(value.length());
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            if (character == '\r' || character == '\n' || character == '\u2028' || character == '\u2029') {
                safe.append(' ');
            } else if (Character.isISOControl(character)) {
                safe.append(' ');
            } else {
                safe.append(character);
            }
        }
        return safe.toString();
    }

    private static String safeCode(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder safe = new StringBuilder(value.length());
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            if (character == '\r') {
                if (index + 1 >= value.length() || value.charAt(index + 1) != '\n') {
                    safe.append('\n');
                }
            } else if (character == '\n' || character == '\t' || !Character.isISOControl(character)) {
                safe.append(character);
            } else {
                safe.append(' ');
            }
        }
        return safe.toString().replace("\r\n", "\n");
    }

    private static int longestBacktickRun(String value) {
        int longest = 0;
        int current = 0;
        for (int index = 0; index < value.length(); index++) {
            if (value.charAt(index) == '`') {
                current++;
                longest = Math.max(longest, current);
            } else {
                current = 0;
            }
        }
        return longest;
    }

    private static PipelineException reportFailure(String code, String message) {
        return new PipelineException(new io.github.jonasfortes12.core.model.PipelineError(
                "report",
                code,
                message,
                null,
                false));
    }

    private record Backup(Path finalPath, Path backupPath) {
    }

    private record ReportEnvelope(
            String runId,
            String status,
            List<DebtReportItem.ReportError> errors,
            List<?> items) {
    }
}
