package io.github.jonasfortes12.tester;

public class LlmConfig {
    private String provider;
    private String apiKey;
    private String model;
    private String endpoint;

    public LlmConfig() {
        this.provider = System.getenv().getOrDefault("DEBT_TESTER_PROVIDER", "openai");
        this.apiKey = System.getenv().getOrDefault("DEBT_TESTER_API_KEY", "");
        this.model = System.getenv().getOrDefault("DEBT_TESTER_MODEL", provider.equalsIgnoreCase("anthropic") ? "claude-3-5-sonnet-20241022" : "gpt-4o");
        this.endpoint = System.getenv().getOrDefault("DEBT_TESTER_ENDPOINT", "");
    }

    public String getProvider() { return provider; }
    public String getApiKey() { return apiKey; }
    public String getModel() { return model; }
    public String getEndpoint() { return endpoint; }
}
