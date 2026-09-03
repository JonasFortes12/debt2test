package io.github.jonasfortes12.tester;

import java.util.ArrayList;
import java.util.List;

import io.github.jonasfortes12.core.model.EnrichedSatdDebt;
import io.github.jonasfortes12.core.model.GeneratedTest;
import io.github.jonasfortes12.core.model.ItemStatus;
import io.github.jonasfortes12.core.model.PipelineError;
import io.github.jonasfortes12.core.model.TestGenerationOptions;
import io.github.jonasfortes12.core.model.ValidationStatus;
import io.github.jonasfortes12.core.port.TestGenerator;
import io.github.jonasfortes12.core.result.TestGenerationResult;

public class TestGeneratorService implements TestGenerator {

    private static final String INTERRUPTION_ERROR_CODE = "LLM_REQUEST_INTERRUPTED";

    private final LlmConfig config;
    private final LlmProvider provider;

    public TestGeneratorService(LlmConfig config) {
        this(config, providerFor(config));
    }

    public TestGeneratorService(LlmConfig config, LlmProvider provider) {
        this.config = requireValue(config, "config");
        this.provider = requireValue(provider, "provider");
    }

    @Override
    public TestGenerationResult generate(List<EnrichedSatdDebt> debts, TestGenerationOptions options) {
        if (debts == null || options == null) {
            throw new IllegalArgumentException("debts and options are required");
        }
        for (EnrichedSatdDebt debt : debts) {
            if (debt == null) {
                throw new IllegalArgumentException("debts must not contain null");
            }
        }

        List<GeneratedTest> generatedTests = new ArrayList<>(debts.size());
        List<PipelineError> errors = new ArrayList<>();
        for (EnrichedSatdDebt debt : debts) {
            GeneratedTest generated = generateOne(debt, options, errors);
            generatedTests.add(generated);
            if (shouldStopAfter(generated)) {
                break;
            }
        }
        return new TestGenerationResult(generatedTests, errors);
    }

    private GeneratedTest generateOne(
            EnrichedSatdDebt debt, TestGenerationOptions options, List<PipelineError> stageErrors) {
        String candidateId = debt.candidateId();
        if (config.getApiKey().isBlank()) {
            String sourceCode = "// Mock Test generated because API key is missing\n@Test\nvoid test"
                    + debt.classifiedDebt().candidate().methodName() + "Debt() {\n    // TODO: Pay off "
                    + debt.classifiedDebt().debtType() + "\n}";
            return generated(candidateId, sourceCode, options, "mock", "none", List.of());
        }

        try {
            var candidate = debt.classifiedDebt().candidate();
            String sourceCode = provider.generateTest(new TestPrompt(
                    candidate.comment(),
                    debt.classifiedDebt().debtType(),
                    candidate.methodSourceCode(),
                    debt.externalTask(),
                    options.framework()));
            return generated(candidateId, sourceCode, options,
                    config.getProvider(), config.getModel(), List.of());
        } catch (LlmProviderException failure) {
            if (isInterruption(failure)) {
                Thread.currentThread().interrupt();
            }
            return failed(candidateId, options, failure.code(), failure.recoverable(), stageErrors);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            return failed(candidateId, options, "LLM_GENERATION_FAILED", false, stageErrors);
        } catch (Exception ignored) {
            return failed(candidateId, options, "LLM_GENERATION_FAILED", false, stageErrors);
        }
    }

    private GeneratedTest generated(
            String candidateId,
            String sourceCode,
            TestGenerationOptions options,
            String providerName,
            String modelName,
            List<PipelineError> errors) {
        return new GeneratedTest(
                candidateId,
                sourceCode,
                options.framework(),
                providerName,
                modelName,
                options.promptVersion(),
                ItemStatus.GENERATED,
                ValidationStatus.NOT_RUN,
                errors);
    }

    private GeneratedTest failed(
            String candidateId,
            TestGenerationOptions options,
            String code,
            boolean recoverable,
            List<PipelineError> stageErrors) {
        PipelineError error = new PipelineError(
                "generation",
                code,
                "LLM test generation failed.",
                candidateId,
                recoverable);
        stageErrors.add(error);
        return new GeneratedTest(
                candidateId,
                null,
                options.framework(),
                config.getProvider(),
                config.getModel(),
                options.promptVersion(),
                ItemStatus.GENERATION_FAILED,
                ValidationStatus.NOT_RUN,
                List.of(error));
    }

    private static boolean shouldStopAfter(GeneratedTest generated) {
        return Thread.currentThread().isInterrupted()
                || generated.errors().stream()
                        .anyMatch(error -> INTERRUPTION_ERROR_CODE.equals(error.code()) && !error.recoverable());
    }

    private static boolean isInterruption(LlmProviderException failure) {
        return INTERRUPTION_ERROR_CODE.equals(failure.code()) && !failure.recoverable();
    }

    private static LlmProvider providerFor(LlmConfig config) {
        LlmConfig checkedConfig = requireValue(config, "config");
        if (checkedConfig.getProvider().equalsIgnoreCase("anthropic")) {
            return new AnthropicProvider(checkedConfig);
        }
        if (checkedConfig.getProvider().equalsIgnoreCase("gemini")) {
            return new GeminiProvider(checkedConfig);
        }
        if (checkedConfig.getProvider().equalsIgnoreCase("openai")) {
            return new OpenAiProvider(checkedConfig);
        }
        throw new IllegalArgumentException("Unsupported LLM provider: " + checkedConfig.getProvider());
    }

    private static <T> T requireValue(T value, String field) {
        if (value == null) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value;
    }
}
