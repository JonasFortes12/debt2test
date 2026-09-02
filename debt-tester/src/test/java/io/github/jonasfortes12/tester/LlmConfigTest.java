package io.github.jonasfortes12.tester;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

public class LlmConfigTest {

    @Test
    void explicitProviderModelAndEndpointValuesAreTrimmed() {
        LlmConfig config = new LlmConfig(
                "  gemini  ", " key ", "  gemini-test  ", "  http://localhost:1234  ");

        assertEquals("gemini", config.getProvider());
        assertEquals("key", config.getApiKey());
        assertEquals("gemini-test", config.getModel());
        assertEquals("http://localhost:1234", config.getEndpoint());
    }

    @Test
    void whitespaceOnlyExplicitApiKeyRemainsNoKey() {
        assertEquals("", new LlmConfig("openai", " \t", "model", "").getApiKey());
    }

    @Test
    void explicitBlankProviderAndModelValuesAreRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new LlmConfig(" \t", "key", "model", ""));
        assertThrows(IllegalArgumentException.class,
                () -> new LlmConfig("openai", "key", " \t", ""));
    }

    @Test
    void whitespaceEnvironmentValuesUseDefaultsAndNonblankValuesAreTrimmed() {
        assertEquals("openai", LlmConfig.normalizeEnvironmentValue(" \t", "openai"));
        assertEquals("gpt-4o", LlmConfig.normalizeEnvironmentValue("\n", "gpt-4o"));
        assertEquals("gemini", LlmConfig.normalizeEnvironmentValue("  gemini  ", "openai"));
        assertEquals("", LlmConfig.normalizeEnvironmentValue(null, ""));
    }

    @Test
    void anthropicUsesTheCurrentSonnetBaseline() {
        assertEquals("claude-sonnet-4-6", LlmConfig.defaultModelFor("anthropic"));
        assertEquals("gpt-4o", LlmConfig.defaultModelFor("openai"));
        assertEquals("gemini-3.5-flash-lite", LlmConfig.defaultModelFor("gemini"));
    }
}
