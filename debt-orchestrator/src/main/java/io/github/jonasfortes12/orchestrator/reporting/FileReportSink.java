package io.github.jonasfortes12.orchestrator.reporting;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
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
import io.github.jonasfortes12.core.util.UrlSanitizer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

public final class FileReportSink implements ReportSink {

    private static final String DEBT_REPORT_NAME = "debt-report.json";
    private static final String TEST_REPORT_NAME = "debt-test-report.json";
    private static final String MARKDOWN_REPORT_NAME = "debt-test-report.md";
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
            "EXTRACTION_ERRORS_MISSING",
            "CLASSIFICATION_STAGE_FAILED", "CLASSIFICATION_RESULT_MISSING", "CLASSIFICATION_VALUES_MISSING",
            "CONTEXT_STAGE_FAILED", "CONTEXT_RESULT_MISSING",
            "CONTEXT_ERRORS_MISSING", "CONTEXT_ENRICHMENTS_MISSING",
            "GENERATION_STAGE_FAILED", "GENERATION_RESULT_MISSING",
            "GENERATION_ERRORS_MISSING", "GENERATION_TESTS_MISSING",
            "REPORT_ARTIFACT_MISSING",
            "REPORT_WRITE_FAILED", "RUN_ID_RESOLUTION_FAILED", "GENERATION_FAILED");
    private static final java.util.regex.Pattern CONTEXT_PROVIDER_CODE = java.util.regex.Pattern.compile(
            "CONTEXT_PROVIDER_[A-Z][A-Z0-9_-]{0,31}_(?:ID_FAILED|SUPPORTS_FAILED|FETCH_FAILED|NULL_RESOLUTION|TASK_MISMATCH|NOT_FOUND|RATE_LIMITED|UNAUTHORIZED|UNKNOWN)");

    private final Gson gson = new GsonBuilder()
            .serializeNulls()
            .setPrettyPrinting()
            .create();

    @Override
    public ReportArtifact write(PipelineResult result, ReportOptions options) {
        Objects.requireNonNull(result, "result must not be null");
        Objects.requireNonNull(options, "options must not be null");

        Path outputDirectory = options.outputDirectory();
        try {
            Files.createDirectories(outputDirectory);

            List<PipelineItemResult> satdItems = result.items().stream()
                    .filter(item -> item != null && item.classification() != null && item.classification().satd())
                    .toList();
            List<DebtReportItem> debtItems = satdItems.stream().map(item -> debtItem(result, item)).toList();
            List<TestReportItem> testItems = satdItems.stream().map(item -> testItem(result, item)).toList();

            Path debtReport = outputDirectory.resolve(DEBT_REPORT_NAME);
            Path testReport = outputDirectory.resolve(TEST_REPORT_NAME);
            Path markdownReport = outputDirectory.resolve(MARKDOWN_REPORT_NAME);

            Files.writeString(debtReport, jsonReport(debtItems, result), StandardCharsets.UTF_8);
            Files.writeString(testReport, jsonReport(testItems, result), StandardCharsets.UTF_8);
            Files.writeString(markdownReport, markdown(result, testItems), StandardCharsets.UTF_8);

            return new ReportArtifact(List.of(debtReport, testReport, markdownReport));
        } catch (IOException | RuntimeException failure) {
            throw reportFailure("REPORT_WRITE_FAILED", "pipeline reports could not be written");
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
                UrlSanitizer.sanitize(item.candidate().sourceProvenance().repositoryUrl()),
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
        return new DebtReportItem.IssueReference(UrlSanitizer.sanitize(reference.value()), reference.source());
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
                UrlSanitizer.sanitize(task.url()));
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

    private record ReportEnvelope(
            String runId,
            String status,
            List<DebtReportItem.ReportError> errors,
            List<?> items) {
    }
}
