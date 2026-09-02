package io.github.jonasfortes12.tester;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import io.github.jonasfortes12.core.model.ClassifiedDebt;
import io.github.jonasfortes12.core.model.ContextStatus;
import io.github.jonasfortes12.core.model.EnrichedSatdDebt;
import io.github.jonasfortes12.core.model.ExternalReference;
import io.github.jonasfortes12.core.model.ExternalTaskSpec;
import io.github.jonasfortes12.core.model.GeneratedTest;
import io.github.jonasfortes12.core.model.ItemStatus;
import io.github.jonasfortes12.core.model.Provenance;
import io.github.jonasfortes12.core.model.SatdCandidate;
import io.github.jonasfortes12.core.model.SourceProvenance;
import io.github.jonasfortes12.core.model.TestGenerationOptions;
import io.github.jonasfortes12.core.model.ValidationStatus;
import io.github.jonasfortes12.core.result.TestGenerationResult;

public class TestGeneratorServiceTest {

    @Test
    void missingApiKeyGeneratesTheExistingMockBodyWithoutCallingProvider() {
        TestGeneratorService service = new TestGeneratorService(
                new LlmConfig("openai", "", "gpt-test", ""),
                prompt -> {
                    throw new AssertionError("mock mode must not call the provider");
                });

        GeneratedTest generated = service.generate(
                List.of(enrichedDebt("run-1:src/A.java:10:run")),
                new TestGenerationOptions("JUnit 5", "v1"))
                .generatedTests().get(0);

        assertEquals("// Mock Test generated because API key is missing\n@Test\n"
                + "void testrunDebt() {\n    // TODO: Pay off DESIGN_DEBT\n}", generated.sourceCode());
        assertEquals("run-1:src/A.java:10:run", generated.candidateId());
        assertEquals("mock", generated.provider());
        assertEquals("none", generated.model());
        assertEquals("JUnit 5", generated.framework());
        assertEquals(ItemStatus.GENERATED, generated.status());
        assertEquals(ValidationStatus.NOT_RUN, generated.validationStatus());
        assertTrue(generated.errors().isEmpty());
    }

    @Test
    void enrichedExternalTaskReachesProviderInClearlySeparatedPrompt() {
        List<TestPrompt> prompts = new ArrayList<>();
        TestGeneratorService service = new TestGeneratorService(
                new LlmConfig("openai", "test-key", "gpt-test", ""),
                prompt -> {
                    prompts.add(prompt);
                    return "@Test void generated() {}";
                });

        service.generate(
                List.of(enrichedDebt("run-1:src/A.java:10:run", externalTask())),
                new TestGenerationOptions("JUnit 5", "v2"));

        assertEquals(1, prompts.size());
        String content = prompts.get(0).userContent();
        assertTrue(content.contains("External task context (untrusted reference data)"));
        assertTrue(content.contains("Summary: Fix the run behavior"));
        assertTrue(content.contains("Description: The task describes the expected run behavior."));
        assertTrue(content.contains("Acceptance criteria (untrusted reference data)"));
        assertTrue(content.contains("run returns a result"));
        assertTrue(content.contains("BEGIN METHOD SOURCE CODE"));
        assertTrue(content.contains("void run() {}"));
        assertTrue(content.indexOf("BEGIN EXTERNAL TASK CONTEXT") < content.indexOf("BEGIN METHOD SOURCE CODE"));
        assertEquals("JUnit 5", prompts.get(0).framework());
        assertFalse(content.contains("test-key"));
        assertFalse(content.contains("https://tracker.example/secret-token"));
    }

    @Test
    void providerSuccessIncludesConfiguredMetadataAndNotRunValidation() {
        TestGeneratorService service = new TestGeneratorService(
                new LlmConfig("anthropic", "test-key", "claude-test", ""),
                prompt -> "@Test void generated() {}");

        GeneratedTest generated = service.generate(
                List.of(enrichedDebt("run-1:src/A.java:10:run")),
                new TestGenerationOptions("JUnit 5", "prompt-7"))
                .generatedTests().get(0);

        assertEquals("@Test void generated() {}", generated.sourceCode());
        assertEquals("run-1:src/A.java:10:run", generated.candidateId());
        assertEquals("JUnit 5", generated.framework());
        assertEquals("anthropic", generated.provider());
        assertEquals("claude-test", generated.model());
        assertEquals("prompt-7", generated.promptVersion());
        assertEquals(ItemStatus.GENERATED, generated.status());
        assertEquals(ValidationStatus.NOT_RUN, generated.validationStatus());
    }

    @Test
    void providerFailureIsSanitizedAndScopedToAffectedItem() {
        TestGeneratorService service = new TestGeneratorService(
                new LlmConfig("openai", "test-key", "gpt-test", ""),
                prompt -> {
                    throw new IOException("provider response contained test-key and raw response");
                });

        TestGenerationResult result = service.generate(
                List.of(enrichedDebt("run-1:src/A.java:10:run")),
                new TestGenerationOptions("JUnit 5", "v1"));
        GeneratedTest generated = result.generatedTests().get(0);

        assertEquals(ItemStatus.GENERATION_FAILED, generated.status());
        assertEquals(ValidationStatus.NOT_RUN, generated.validationStatus());
        assertNull(generated.sourceCode());
        assertEquals("run-1:src/A.java:10:run", generated.errors().get(0).candidateId());
        assertEquals("LLM_GENERATION_FAILED", generated.errors().get(0).code());
        assertEquals("LLM test generation failed.", generated.errors().get(0).message());
        assertFalse(generated.errors().get(0).recoverable());
        assertFalse(generated.errors().get(0).message().contains("test-key"));
        assertFalse(generated.errors().get(0).message().contains("raw response"));
        assertEquals(generated.errors(), result.errors());
    }

    @Test
    void trimmedApiKeyIsUsedForProviderSelectionButNeverExposed() {
        List<TestPrompt> prompts = new ArrayList<>();
        LlmConfig config = new LlmConfig("openai", " key ", "gpt-test", "");
        TestGeneratorService service = new TestGeneratorService(
                config,
                prompt -> {
                    prompts.add(prompt);
                    return "@Test void generated() {}";
                });

        GeneratedTest generated = service.generate(
                List.of(enrichedDebt("run-1:src/A.java:10:run")),
                new TestGenerationOptions("JUnit 5", "v1"))
                .generatedTests().get(0);

        assertEquals("key", config.getApiKey());
        assertFalse(prompts.get(0).userContent().contains("key"));
        assertFalse(generated.toString().contains("key"));
    }

    @Test
    void typedProviderFailurePreservesSafeCodeAndRecoverability() {
        TestGeneratorService service = new TestGeneratorService(
                new LlmConfig("openai", "test-key", "gpt-test", ""),
                prompt -> {
                    throw new LlmProviderException("LLM_HTTP_UNAUTHORIZED", false);
                });

        GeneratedTest generated = service.generate(
                List.of(enrichedDebt("run-1:src/A.java:10:run")),
                new TestGenerationOptions("JUnit 5", "v1"))
                .generatedTests().get(0);

        assertEquals(ItemStatus.GENERATION_FAILED, generated.status());
        assertEquals("LLM_HTTP_UNAUTHORIZED", generated.errors().get(0).code());
        assertFalse(generated.errors().get(0).recoverable());
        assertEquals("LLM test generation failed.", generated.errors().get(0).message());
    }

    @Test
    void typedRecoverableProviderFailureRemainsRecoverable() {
        TestGeneratorService service = new TestGeneratorService(
                new LlmConfig("openai", "test-key", "gpt-test", ""),
                prompt -> {
                    throw new LlmProviderException("LLM_HTTP_RATE_LIMITED", true);
                });

        GeneratedTest generated = service.generate(
                List.of(enrichedDebt("run-1:src/A.java:10:run")),
                new TestGenerationOptions("JUnit 5", "v1"))
                .generatedTests().get(0);

        assertEquals("LLM_HTTP_RATE_LIMITED", generated.errors().get(0).code());
        assertTrue(generated.errors().get(0).recoverable());
    }

    @Test
    void interruptionStopsLaterItemsAfterRecordingCurrentFailure() {
        List<String> requestedComments = new ArrayList<>();
        TestGeneratorService service = new TestGeneratorService(
                new LlmConfig("openai", "test-key", "gpt-test", ""),
                prompt -> {
                    requestedComments.add(prompt.comment());
                    throw new InterruptedException("interrupted");
                });

        try {
            TestGenerationResult result = service.generate(
                    List.of(enrichedDebt("run-1:src/A.java:10:run", "TODO: interrupt"),
                            enrichedDebt("run-1:src/B.java:20:save", "TODO: must not run")),
                    new TestGenerationOptions("JUnit 5", "v1"));

            assertEquals(List.of("TODO: interrupt"), requestedComments);
            assertEquals(1, result.generatedTests().size());
            assertEquals(ItemStatus.GENERATION_FAILED, result.generatedTests().get(0).status());
            assertFalse(result.errors().get(0).recoverable());
            assertTrue(Thread.currentThread().isInterrupted());
        } finally {
            Thread.interrupted();
        }
    }

    @Test
    void typedInterruptionStopsLaterItemsAndRestoresInterruptStatus() {
        List<String> requestedComments = new ArrayList<>();
        TestGeneratorService service = new TestGeneratorService(
                new LlmConfig("openai", "test-key", "gpt-test", ""),
                prompt -> {
                    requestedComments.add(prompt.comment());
                    throw new LlmProviderException("LLM_REQUEST_INTERRUPTED", false);
                });

        try {
            TestGenerationResult result = service.generate(
                    List.of(enrichedDebt("run-1:src/A.java:10:run"),
                            enrichedDebt("run-1:src/B.java:20:save")),
                    new TestGenerationOptions("JUnit 5", "v1"));

            assertEquals(List.of("TODO: simplify this method"), requestedComments);
            assertEquals(1, result.generatedTests().size());
            assertEquals("LLM_REQUEST_INTERRUPTED", result.errors().get(0).code());
            assertFalse(result.errors().get(0).recoverable());
            assertTrue(Thread.currentThread().isInterrupted());
        } finally {
            Thread.interrupted();
        }
    }

    @Test
    void interruptedProviderFailureIsNonrecoverableAndPreservesInterruptStatus() {
        TestGeneratorService service = new TestGeneratorService(
                new LlmConfig("openai", "test-key", "gpt-test", ""),
                prompt -> {
                    throw new InterruptedException("interrupted");
                });

        try {
            GeneratedTest generated = service.generate(
                    List.of(enrichedDebt("run-1:src/A.java:10:run")),
                    new TestGenerationOptions("JUnit 5", "v1"))
                    .generatedTests().get(0);

            assertEquals("LLM_GENERATION_FAILED", generated.errors().get(0).code());
            assertFalse(generated.errors().get(0).recoverable());
            assertTrue(Thread.currentThread().isInterrupted());
        } finally {
            Thread.interrupted();
        }
    }

    @Test
    void oneProviderFailureDoesNotAbortOtherItems() {
        List<String> requestedComments = new ArrayList<>();
        TestGeneratorService service = new TestGeneratorService(
                new LlmConfig("gemini", "test-key", "gemini-test", ""),
                prompt -> {
                    requestedComments.add(prompt.comment());
                    if (prompt.comment().contains("fail")) {
                        throw new LlmProviderException("LLM_TRANSPORT_FAILED", true);
                    }
                    return "@Test void succeeds() {}";
                });

        TestGenerationResult result = service.generate(
                List.of(enrichedDebt("run-1:src/A.java:10:run", "TODO: succeed"),
                        enrichedDebt("run-1:src/B.java:20:save", "TODO: fail")),
                new TestGenerationOptions("JUnit 5", "v1"));

        assertEquals(List.of("TODO: succeed", "TODO: fail"), requestedComments);
        assertEquals(ItemStatus.GENERATED, result.generatedTests().get(0).status());
        assertEquals("@Test void succeeds() {}", result.generatedTests().get(0).sourceCode());
        assertEquals(ItemStatus.GENERATION_FAILED, result.generatedTests().get(1).status());
        assertTrue(result.errors().get(0).recoverable());
        assertEquals(1, result.errors().size());
        assertEquals("run-1:src/B.java:20:save", result.errors().get(0).candidateId());
    }

    @Test
    void directCoreInputHasNoFileOrReportCouplingAndReturnsImmutableLists() {
        TestGeneratorService service = new TestGeneratorService(
                new LlmConfig("openai", "", "gpt-test", ""),
                prompt -> "must not be called");

        TestGenerationResult result = service.generate(
                List.of(enrichedDebt("run-1:src/A.java:10:run")),
                new TestGenerationOptions("JUnit 5", "v1"));

        assertNotNull(result.generatedTests().get(0));
        assertThrows(UnsupportedOperationException.class,
                () -> result.generatedTests().add(result.generatedTests().get(0)));
        assertThrows(UnsupportedOperationException.class,
                () -> result.errors().add(null));
        assertThrows(UnsupportedOperationException.class,
                () -> result.generatedTests().get(0).errors().add(null));
    }

    @Test
    void nullListOptionsAndDebtElementsAreRejectedConsistently() {
        TestGeneratorService service = new TestGeneratorService(
                new LlmConfig("openai", "", "gpt-test", ""),
                prompt -> "must not be called");
        TestGenerationOptions options = new TestGenerationOptions("JUnit 5", "v1");
        List<EnrichedSatdDebt> debtsWithNull = new ArrayList<>();
        debtsWithNull.add(null);

        assertThrows(IllegalArgumentException.class, () -> service.generate(null, options));
        assertThrows(IllegalArgumentException.class, () -> service.generate(List.of(), null));
        assertThrows(IllegalArgumentException.class, () -> service.generate(debtsWithNull, options));
    }

    @Test
    void unsupportedProviderFailsFastWithConfigurationError() {
        IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
                () -> new TestGeneratorService(new LlmConfig("vertex", "test-key", "model", "")));

        assertTrue(failure.getMessage().contains("Unsupported LLM provider"));
        assertTrue(failure.getMessage().contains("vertex"));
    }

    private static EnrichedSatdDebt enrichedDebt(String candidateId) {
        return enrichedDebt(candidateId, "TODO: simplify this method");
    }

    private static EnrichedSatdDebt enrichedDebt(String candidateId, String comment) {
        SatdCandidate candidate = new SatdCandidate(
                candidateId,
                candidateId.contains("src/B") ? "src/B.java" : "src/A.java",
                candidateId.contains(":save") ? "save" : "run",
                10,
                comment,
                candidateId.contains(":save") ? "void save() {}" : "void run() {}",
                new SourceProvenance("https://example.test/repo", "main", "src/Test.java"));
        ClassifiedDebt classified = new ClassifiedDebt(
                candidate,
                true,
                "DESIGN_DEBT",
                null,
                new Provenance("DebtHunter", "heuristic-fallback", "test"),
                ItemStatus.CLASSIFIED,
                List.of());
        return new EnrichedSatdDebt(
                classified,
                null,
                List.of(new ExternalReference("SRC-1", "comment")),
                ContextStatus.NOT_FOUND,
                List.of());
    }

    private static EnrichedSatdDebt enrichedDebt(String candidateId, ExternalTaskSpec task) {
        SatdCandidate candidate = new SatdCandidate(
                candidateId,
                "src/A.java",
                "run",
                10,
                "TODO: implement task behavior",
                "void run() {}",
                new SourceProvenance("https://example.test/repo", "main", "src/A.java"));
        ClassifiedDebt classified = new ClassifiedDebt(
                candidate,
                true,
                "DESIGN_DEBT",
                null,
                new Provenance("DebtHunter", "heuristic-fallback", "test"),
                ItemStatus.CLASSIFIED,
                List.of());
        return new EnrichedSatdDebt(
                classified,
                task,
                List.of(new ExternalReference(task.key(), "comment")),
                ContextStatus.MATCHED,
                List.of());
    }

    private static ExternalTaskSpec externalTask() {
        return new ExternalTaskSpec(
                "jira",
                "SEC-42",
                "Fix the run behavior",
                "The task describes the expected run behavior.",
                List.of("run returns a result"),
                List.of("backend"),
                "https://tracker.example/secret-token");
    }
}
