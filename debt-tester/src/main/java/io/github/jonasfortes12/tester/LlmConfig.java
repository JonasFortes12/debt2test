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

        this.provider = getConfigValue(dotenv, "DEBT_TESTER_PROVIDER", "openai");
        this.apiKey = getConfigValue(dotenv, "DEBT_TESTER_API_KEY", "");
        
        String defaultModel = "gpt-4o";
        if (provider.equalsIgnoreCase("anthropic")) {
            defaultModel = "claude-3-5-sonnet-20241022";
        } else if (provider.equalsIgnoreCase("gemini")) {
            defaultModel = "gemini-3.5-flash-lite";
        }
        
        this.model = getConfigValue(dotenv, "DEBT_TESTER_MODEL", defaultModel);
        this.endpoint = getConfigValue(dotenv, "DEBT_TESTER_ENDPOINT", "");
    }

    private String getConfigValue(Dotenv dotenv, String key, String defaultValue) {
        String val = System.getenv(key);
        if (val == null || val.isEmpty()) {
            val = dotenv.get(key);
        }
        if (val == null || val.isEmpty()) {
            return defaultValue;
        }
        return val;
    }

    public String getProvider() { return provider; }
    public String getApiKey() { return apiKey; }
    public String getModel() { return model; }
    public String getEndpoint() { return endpoint; }
}
