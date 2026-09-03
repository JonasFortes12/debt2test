package io.github.jonasfortes12.orchestrator.reporting;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.jonasfortes12.core.error.PipelineException;
import io.github.jonasfortes12.core.model.ClassifiedDebt;
import io.github.jonasfortes12.core.model.ContextStatus;
import io.github.jonasfortes12.core.model.EnrichedSatdDebt;
import io.github.jonasfortes12.core.model.ExternalReference;
import io.github.jonasfortes12.core.model.ExternalTaskSpec;
import io.github.jonasfortes12.core.model.GeneratedTest;
import io.github.jonasfortes12.core.model.ItemStatus;
import io.github.jonasfortes12.core.model.PipelineError;
import io.github.jonasfortes12.core.model.PipelineItemResult;
import io.github.jonasfortes12.core.model.PipelineResult;
import io.github.jonasfortes12.core.model.Provenance;
import io.github.jonasfortes12.core.model.ReportArtifact;
import io.github.jonasfortes12.core.model.ReportOptions;
import io.github.jonasfortes12.core.model.RepositoryWorkspace;
import io.github.jonasfortes12.core.model.RunStatus;
import io.github.jonasfortes12.core.model.SatdCandidate;
import io.github.jonasfortes12.core.model.SourceProvenance;
import io.github.jonasfortes12.core.model.ValidationStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileReportSinkTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void writesThreeUtf8ReportsWithLegacyFieldsAndSatdMetadata() throws Exception {
        Path outputDirectory = temporaryDirectory.resolve("nested").resolve("reports");
        PipelineResult result = resultWithMatchedNotFoundAndFailedItems();

        ReportArtifact artifact = new FileReportSink().write(result, new ReportOptions(outputDirectory));

        assertEquals(List.of(
                outputDirectory.resolve("debt-report.json"),
                outputDirectory.resolve("debt-test-report.json"),
                outputDirectory.resolve("debt-test-report.md")), artifact.paths());
        try (var files = Files.list(outputDirectory)) {
            assertEquals(3, files.count());
        }

        JsonArray debtReport = readJsonArray(outputDirectory.resolve("debt-report.json"));
        JsonArray testReport = readJsonArray(outputDirectory.resolve("debt-test-report.json"));
        assertEquals(3, debtReport.size());
        assertEquals(3, testReport.size());

        JsonObject matchedDebt = itemById(debtReport, "run-7:src/main/Save.java:12:save");
        assertEquals("src/main/Save.java", matchedDebt.get("filePath").getAsString());
        assertEquals("save", matchedDebt.get("methodName").getAsString());
        assertEquals(12, matchedDebt.get("lineNumber").getAsInt());
        assertEquals("TODO: save it", matchedDebt.get("comment").getAsString());
        assertEquals("void save() {}", matchedDebt.get("methodSourceCode").getAsString());
        assertTrue(matchedDebt.get("isSatd").getAsBoolean());
        assertEquals("DESIGN", matchedDebt.get("debtType").getAsString());
        assertEquals(0.91, matchedDebt.get("confidence").getAsDouble(), 0.0001);
        assertEquals("MATCHED", matchedDebt.get("contextStatus").getAsString());
        assertEquals("run-7:src/main/Save.java:12:save", matchedDebt.get("candidateId").getAsString());
        assertEquals("run-7", matchedDebt.get("runId").getAsString());
        assertEquals("https://example.com/repository", matchedDebt.get("repositoryUrl").getAsString());
        assertEquals("abc123", matchedDebt.get("revision").getAsString());
        assertEquals("DebtHunter", matchedDebt.getAsJsonObject("classificationProvenance")
                .get("provider").getAsString());
        assertEquals("model", matchedDebt.getAsJsonObject("classificationProvenance")
                .get("strategy").getAsString());
        assertEquals("v1", matchedDebt.getAsJsonObject("classificationProvenance")
                .get("version").getAsString());
        assertEquals("TASK-7", matchedDebt.getAsJsonObject("externalTask").get("key").getAsString());
        assertEquals("example.com:tasks/TASK-7", matchedDebt.getAsJsonObject("externalTask").get("url").getAsString());
        assertEquals("TASK-7", matchedDebt.getAsJsonArray("issueReferences")
                .get(0).getAsJsonObject().get("value").getAsString());
        assertEquals("https://example.com/path", matchedDebt.getAsJsonArray("issueReferences")
                .get(1).getAsJsonObject().get("value").getAsString());
        assertEquals("[redacted URL]", matchedDebt.getAsJsonArray("issueReferences")
                .get(2).getAsJsonObject().get("value").getAsString());

        JsonObject matchedTest = itemById(testReport, "run-7:src/main/Save.java:12:save");
        assertEquals("// generated test", matchedTest.get("generatedTestCode").getAsString());
        assertEquals("GENERATED", matchedTest.get("status").getAsString());
        assertEquals(0.91, matchedTest.get("confidence").getAsDouble(), 0.0001);
        assertEquals("openai", matchedTest.get("provider").getAsString());
        assertEquals("gpt-test", matchedTest.get("model").getAsString());
        assertEquals("v1", matchedTest.get("promptVersion").getAsString());
        assertEquals("NOT_RUN", matchedTest.get("validationStatus").getAsString());

        JsonObject failedTest = itemById(testReport, "run-7:src/main/Fail.java:30:fail");
        assertTrue(failedTest.get("generatedTestCode").isJsonNull());
        assertEquals("GENERATION_FAILED", failedTest.get("status").getAsString());
        assertEquals("GENERATION_FAILED", failedTest.getAsJsonArray("errors")
                .get(0).getAsJsonObject().get("code").getAsString());
        assertFalse(failedTest.toString().contains("SYNTHETIC_API_KEY"));
        assertFalse(failedTest.toString().contains("raw provider payload"));
        assertFalse(testReport.toString().contains("repo-secret"));
        assertFalse(testReport.toString().contains("repo-token"));
        assertFalse(testReport.toString().contains("reference-secret"));
        assertFalse(testReport.toString().contains("reference-token"));
        assertFalse(testReport.toString().contains("scp-secret"));
        assertFalse(testReport.toString().contains("task-token"));
        assertFalse(testReport.toString().contains("opaque-secret"));
        assertFalse(testReport.toString().contains("opaque-token"));
        assertFalse(testReport.toString().contains("workspace-secret"));
        assertFalse(testReport.toString().contains("workspace-token"));
        assertFalse(testReport.toString().contains("fragment"));
        assertFalse(testReport.toString().contains("Authorization"));

        JsonObject notFoundTest = itemById(testReport, "run-7:hostile:20:lookup");
        assertEquals("NOT_FOUND", notFoundTest.get("contextStatus").getAsString());
        assertTrue(notFoundTest.get("generatedTestCode").isJsonNull());
        assertTrue(testReport.toString().contains("é"));

        String markdown = Files.readString(outputDirectory.resolve("debt-test-report.md"), StandardCharsets.UTF_8);
        assertTrue(markdown.startsWith("# Technical Debt Test Generation Report\n\n"));
        assertTrue(markdown.contains("## Save.java -> save()"));
        assertTrue(markdown.contains("- **Debt Type:** `DESIGN`"));
        assertTrue(markdown.contains("- **Line Number:** `12`"));
        assertTrue(markdown.contains("- **Status:** `GENERATED`"));
        assertTrue(markdown.contains("- **Comment:** `TODO: save it`"));
        assertTrue(markdown.contains("### Generated Test Case"));
        assertTrue(markdown.contains("```java\nvoid save() {}\n```"));
        assertFalse(markdown.contains("SYNTHETIC_API_KEY"));
        assertFalse(markdown.contains("raw provider payload"));
        assertFalse(markdown.contains("line two\n"));
        assertTrue(markdown.contains("line two"));
        assertFalse(markdown.contains("\n# injected"));
        assertTrue(markdown.contains("````` TODO: line one line two `single ``` triple ```` mixed é `````"));
    }

    @Test
    void createsAnEmptyOutputDirectoryAndReportsWriteFailuresWithoutDetails() throws Exception {
        Path outputDirectory = temporaryDirectory.resolve("new-output");
        ReportArtifact artifact = new FileReportSink().write(
                resultWithMatchedNotFoundAndFailedItems(), new ReportOptions(outputDirectory));

        assertEquals(3, artifact.paths().size());
        assertTrue(Files.isDirectory(outputDirectory));

        Path blocked = temporaryDirectory.resolve("blocked");
        Files.writeString(blocked, "not a directory", StandardCharsets.UTF_8);
        PipelineException failure = assertThrows(PipelineException.class, () -> new FileReportSink().write(
                resultWithMatchedNotFoundAndFailedItems(), new ReportOptions(blocked)));
        assertEquals("REPORT_WRITE_FAILED", failure.error().code());
        assertFalse(failure.getMessage().contains("not a directory"));
        assertFalse(failure.getMessage().contains("SYNTHETIC_API_KEY"));
    }

    @Test
    void representsFailedRunsWithAnEnvelopeInsteadOfAFalseEmptyArray() throws Exception {
        PipelineResult failedRun = new PipelineResult(
                "failed-run",
                RunStatus.FAILED,
                null,
                List.of(),
                List.of(error("classification", "CLASSIFICATION_STAGE_FAILED", null, false,
                        "raw provider payload SYNTHETIC_API_KEY")),
                null);
        Path outputDirectory = temporaryDirectory.resolve("failed-run");

        new FileReportSink().write(failedRun, new ReportOptions(outputDirectory));

        JsonObject debtEnvelope = JsonParser.parseString(Files.readString(
                outputDirectory.resolve("debt-report.json"), StandardCharsets.UTF_8)).getAsJsonObject();
        JsonObject testEnvelope = JsonParser.parseString(Files.readString(
                outputDirectory.resolve("debt-test-report.json"), StandardCharsets.UTF_8)).getAsJsonObject();
        for (JsonObject envelope : List.of(debtEnvelope, testEnvelope)) {
            assertEquals("failed-run", envelope.get("runId").getAsString());
            assertEquals("FAILED", envelope.get("status").getAsString());
            assertTrue(envelope.getAsJsonArray("items").isEmpty());
            assertEquals("CLASSIFICATION_STAGE_FAILED", envelope.getAsJsonArray("errors")
                    .get(0).getAsJsonObject().get("code").getAsString());
            assertFalse(envelope.toString().contains("SYNTHETIC_API_KEY"));
            assertFalse(envelope.toString().contains("raw provider payload"));
        }
    }

    @Test
    void wrapsNonemptyRunsWithErrorsAndStatusInBothJsonReports() throws Exception {
        PipelineResult baseResult = resultWithMatchedNotFoundAndFailedItems();
        for (RunStatus status : List.of(RunStatus.COMPLETED_WITH_ERRORS, RunStatus.FAILED)) {
            Path outputDirectory = temporaryDirectory.resolve(status.name().toLowerCase());
            new FileReportSink().write(baseResult.withStatus(status), new ReportOptions(outputDirectory));

            JsonObject debtEnvelope = JsonParser.parseString(Files.readString(
                    outputDirectory.resolve("debt-report.json"), StandardCharsets.UTF_8)).getAsJsonObject();
            JsonObject testEnvelope = JsonParser.parseString(Files.readString(
                    outputDirectory.resolve("debt-test-report.json"), StandardCharsets.UTF_8)).getAsJsonObject();
            for (JsonObject envelope : List.of(debtEnvelope, testEnvelope)) {
                assertEquals("run-7", envelope.get("runId").getAsString());
                assertEquals(status.name(), envelope.get("status").getAsString());
                assertEquals(3, envelope.getAsJsonArray("items").size());
                assertEquals("GENERATION_FAILED", envelope.getAsJsonArray("errors")
                        .get(0).getAsJsonObject().get("code").getAsString());
            }

            String markdown = Files.readString(
                    outputDirectory.resolve("debt-test-report.md"), StandardCharsets.UTF_8);
            assertTrue(markdown.contains("- **Run ID:** `run-7`"));
            assertTrue(markdown.contains("- **Status:** `" + status + "`"));
            assertTrue(markdown.contains("- **Errors:**"));
            assertTrue(markdown.contains("  - `GENERATION_FAILED`"));
        }
    }

    @Test
    void keepsLegacyArrayRootsForCleanSuccessfulReports() throws Exception {
        PipelineResult baseResult = resultWithMatchedNotFoundAndFailedItems();
        PipelineResult successfulResult = new PipelineResult(
                "successful-run",
                RunStatus.COMPLETED,
                baseResult.workspace(),
                List.of(baseResult.items().get(0)),
                List.of(),
                null);
        Path outputDirectory = temporaryDirectory.resolve("successful-run");

        new FileReportSink().write(successfulResult, new ReportOptions(outputDirectory));

        var debtReport = JsonParser.parseString(Files.readString(
                outputDirectory.resolve("debt-report.json"), StandardCharsets.UTF_8));
        var testReport = JsonParser.parseString(Files.readString(
                outputDirectory.resolve("debt-test-report.json"), StandardCharsets.UTF_8));
        assertTrue(debtReport.isJsonArray());
        assertTrue(testReport.isJsonArray());
        assertEquals(1, debtReport.getAsJsonArray().size());
        assertEquals(1, testReport.getAsJsonArray().size());
    }

    @Test
    void mapsMaliciousPipelineErrorMetadataToSafeCurrentItemData() throws Exception {
        PipelineResult baseResult = resultWithMatchedNotFoundAndFailedItems();
        SatdCandidate candidate = baseResult.items().get(0).candidate();
        PipelineError malicious = error(
                "attacker-stage\nAuthorization: Bearer hidden", "secret-code\nraw-provider-payload",
                "other-candidate\nstack-trace", false, "exception stack trace API_KEY=hidden");
        PipelineError knownDiagnostic = error(
                "extraction", "EXTRACTION_CANDIDATES_MISSING", "other-candidate", true, "safe diagnostic");
        PipelineItemResult item = new PipelineItemResult(
                candidate,
                baseResult.items().get(0).classification(),
                baseResult.items().get(0).enrichment(),
                baseResult.items().get(0).generatedTest(),
                List.of(malicious, knownDiagnostic));
        PipelineResult result = new PipelineResult(
                baseResult.runId(), baseResult.status(), baseResult.workspace(),
                List.of(item), List.of(malicious), null);
        Path outputDirectory = temporaryDirectory.resolve("malicious-error");

        new FileReportSink().write(result, new ReportOptions(outputDirectory));

        JsonObject envelope = JsonParser.parseString(Files.readString(
                outputDirectory.resolve("debt-report.json"), StandardCharsets.UTF_8))
                .getAsJsonObject();
        JsonObject globalError = envelope.getAsJsonArray("errors").get(0).getAsJsonObject();
        assertEquals("unknown", globalError.get("stage").getAsString());
        assertEquals("UNKNOWN_ERROR", globalError.get("code").getAsString());
        assertTrue(globalError.get("candidateId").isJsonNull());
        JsonObject report = envelope.getAsJsonArray("items").get(0).getAsJsonObject();
        JsonObject reportError = report.getAsJsonArray("errors").get(0).getAsJsonObject();
        assertEquals("unknown", reportError.get("stage").getAsString());
        assertEquals("UNKNOWN_ERROR", reportError.get("code").getAsString());
        assertEquals("pipeline error recorded", reportError.get("message").getAsString());
        assertEquals(candidate.candidateId(), reportError.get("candidateId").getAsString());
        JsonObject knownError = report.getAsJsonArray("errors").get(1).getAsJsonObject();
        assertEquals("EXTRACTION_CANDIDATES_MISSING", knownError.get("code").getAsString());
        assertEquals(candidate.candidateId(), knownError.get("candidateId").getAsString());
        assertFalse(report.toString().contains("Authorization"));
        assertFalse(report.toString().contains("hidden"));
        assertFalse(report.toString().contains("stack-trace"));
    }

    private static JsonArray readJsonArray(Path file) throws Exception {
        var report = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8));
        return report.isJsonArray()
                ? report.getAsJsonArray()
                : report.getAsJsonObject().getAsJsonArray("items");
    }

    private static JsonObject itemById(JsonArray items, String candidateId) {
        for (var item : items) {
            JsonObject object = item.getAsJsonObject();
            if (candidateId.equals(object.get("candidateId").getAsString())) {
                return object;
            }
        }
        throw new AssertionError("Missing report item " + candidateId);
    }

    private static PipelineResult resultWithMatchedNotFoundAndFailedItems() {
        SatdCandidate matchedCandidate = candidate(
                "run-7:src/main/Save.java:12:save", "src/main/Save.java", "save", 12, "TODO: save it");
        SatdCandidate notFoundCandidate = candidate(
                "run-7:hostile:20:lookup", "src/main/Lookup\n# injected`file```Name.java", "lookup", 20,
                "TODO: line one\nline two `single ``` triple ```` mixed\né");
        SatdCandidate failedCandidate = candidate(
                "run-7:src/main/Fail.java:30:fail", "src/main/Fail.java", "fail", 30, "FIXME: fail");
        SatdCandidate nonSatdCandidate = candidate(
                "run-7:src/main/Plain.java:40:plain", "src/main/Plain.java", "plain", 40, "ordinary comment");

        ClassifiedDebt matchedDebt = classified(matchedCandidate, true, "DESIGN", 0.91);
        ClassifiedDebt notFoundDebt = classified(notFoundCandidate, true, "TEST");
        ClassifiedDebt failedDebt = classified(failedCandidate, true, "DEFECT");
        ClassifiedDebt nonSatd = classified(nonSatdCandidate, false, "NONE");

        EnrichedSatdDebt matchedContext = new EnrichedSatdDebt(
                matchedDebt,
                new ExternalTaskSpec("jira", "TASK-7", "Save task", "Description", List.of("criterion"),
                        List.of("satd"), "scp-user:scp-secret@example.com:tasks/TASK-7?token=task-token#fragment"),
                List.of(
                        new ExternalReference("TASK-7", "comment"),
                        new ExternalReference(
                                "https://reference-user:reference-secret@example.com/path?token=reference-token#fragment",
                                "comment"),
                        new ExternalReference("opaque:opaque-secret?token=opaque-token#fragment", "comment")),
                ContextStatus.MATCHED,
                List.of());
        EnrichedSatdDebt notFoundContext = new EnrichedSatdDebt(
                notFoundDebt, null, List.of(), ContextStatus.NOT_FOUND,
                List.of(error("context", "CONTEXT_RESULT_MISSING", notFoundCandidate.candidateId(), true,
                        "raw provider payload")));
        PipelineError generationError = error(
                "generation", "GENERATION_FAILED", failedCandidate.candidateId(), true,
                "SYNTHETIC_API_KEY=do-not-write raw provider payload");
        EnrichedSatdDebt failedContext = new EnrichedSatdDebt(
                failedDebt, new ExternalTaskSpec("jira", "TASK-FAIL", "Fail task", null, List.of(), List.of(), null),
                List.of(new ExternalReference("TASK-FAIL", "comment")), ContextStatus.MATCHED, List.of());

        GeneratedTest generated = new GeneratedTest(
                matchedCandidate.candidateId(), "// generated test", "JUnit 5", "openai", "gpt-test", "v1",
                ItemStatus.GENERATED, ValidationStatus.NOT_RUN, List.of());
        GeneratedTest failed = new GeneratedTest(
                failedCandidate.candidateId(), null, "JUnit 5", "openai", "gpt-test", "v1",
                ItemStatus.GENERATION_FAILED, ValidationStatus.NOT_RUN, List.of(generationError));

        return new PipelineResult(
                "run-7",
                RunStatus.COMPLETED_WITH_ERRORS,
                new RepositoryWorkspace(Path.of("/tmp/workspace"),
                        "https://workspace-user:workspace-secret@example.com/workspace?token=workspace-token#fragment",
                        "abc123"),
                List.of(
                        new PipelineItemResult(matchedCandidate, matchedDebt, matchedContext, generated, List.of()),
                        new PipelineItemResult(notFoundCandidate, notFoundDebt, notFoundContext, null,
                                notFoundContext.errors()),
                        new PipelineItemResult(failedCandidate, failedDebt, failedContext, failed,
                                List.of(generationError)),
                        new PipelineItemResult(nonSatdCandidate, nonSatd, null, null, List.of())),
                List.of(generationError),
                null);
    }

    private static SatdCandidate candidate(String id, String file, String method, int line, String comment) {
        return new SatdCandidate(id, file, method, line, comment, "void " + method + "() {}",
                new SourceProvenance(
                        "https://report-user:repo-secret@example.com/repository?token=repo-token#fragment",
                        "abc123", file));
    }

    private static ClassifiedDebt classified(SatdCandidate candidate, boolean satd, String debtType) {
        return classified(candidate, satd, debtType, null);
    }

    private static ClassifiedDebt classified(
            SatdCandidate candidate, boolean satd, String debtType, Double confidence) {
        return new ClassifiedDebt(candidate, satd, debtType, confidence,
                new Provenance("DebtHunter", "model", "v1"),
                satd ? ItemStatus.CLASSIFIED : ItemStatus.NOT_SATD, List.of());
    }

    private static PipelineError error(
            String stage, String code, String candidateId, boolean recoverable, String message) {
        return new PipelineError(stage, code, message, candidateId, recoverable);
    }
}
