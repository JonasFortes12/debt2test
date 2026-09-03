package io.github.jonasfortes12.tester;

import io.github.cdimascio.dotenv.Dotenv;

public class LlmConfig {
    private String provider;
    private String apiKey;
    private String model;
    private String endpoint;

    public LlmConfig() {
        Dotenv dotenv = Dotenv.configure()
                .ignoreIfMissing()
                .load();

        this.provider = requireSupportedProvider(getConfigValue(dotenv, "DEBT_TESTER_PROVIDER", "openai"));
        this.apiKey = getConfigValue(dotenv, "DEBT_TESTER_API_KEY", "");

        this.model = getConfigValue(dotenv, "DEBT_TESTER_MODEL", defaultModelFor(provider));
        this.endpoint = getConfigValue(dotenv, "DEBT_TESTER_ENDPOINT", "");
    }

    public LlmConfig(String provider, String apiKey, String model, String endpoint) {
        this.provider = requireSupportedProvider(provider);
        this.apiKey = normalizeEnvironmentValue(apiKey, "");
        this.model = requireValue(model, "model");
        this.endpoint = endpoint == null ? "" : endpoint.trim();
    }

    private String getConfigValue(Dotenv dotenv, String key, String defaultValue) {
        String val = System.getenv(key);
        if (val == null || val.isBlank()) {
            val = dotenv.get(key);
        }
        return normalizeEnvironmentValue(val, defaultValue);
    }

    private static String requireValue(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value.trim();
    }

    static String normalizeEnvironmentValue(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }

    static String defaultModelFor(String provider) {
        if (provider.equalsIgnoreCase("anthropic")) {
            return "claude-sonnet-4-6";
        }
        if (provider.equalsIgnoreCase("gemini")) {
            return "gemini-3.5-flash-lite";
        }
        return "gpt-4o";
    }

    private static String requireSupportedProvider(String provider) {
        String value = requireValue(provider, "provider");
        if (!value.equalsIgnoreCase("openai")
                && !value.equalsIgnoreCase("anthropic")
                && !value.equalsIgnoreCase("gemini")) {
            throw new IllegalArgumentException(
                    "Unsupported LLM provider: " + value + ". Supported providers: openai, anthropic, gemini");
        }
        return value;
    }

    public String getProvider() {
        return provider;
    }

    public String getApiKey() {
        return apiKey;
    }

    public String getModel() {
        return model;
    }

    public String getEndpoint() {
        return endpoint;
    }
}
