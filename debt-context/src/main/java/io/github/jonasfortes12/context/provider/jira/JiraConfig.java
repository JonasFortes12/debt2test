package io.github.jonasfortes12.context.provider.jira;

import io.github.cdimascio.dotenv.Dotenv;

/**
 * Reads Jira adapter configuration from environment variables or a local {@code .env} file.
 * The Jira provider is only enabled when all three values are present; see {@link #isConfigured()}.
 */
public final class JiraConfig {

    private final String baseUrl;
    private final String email;
    private final String apiToken;

    public JiraConfig() {
        Dotenv dotenv = Dotenv.configure()
                .ignoreIfMissing()
                .load();

        this.baseUrl = getConfigValue(dotenv, "DEBT_CONTEXT_JIRA_BASE_URL");
        this.email = getConfigValue(dotenv, "DEBT_CONTEXT_JIRA_EMAIL");
        this.apiToken = getConfigValue(dotenv, "DEBT_CONTEXT_JIRA_API_TOKEN");
    }

    private static String getConfigValue(Dotenv dotenv, String key) {
        String value = System.getenv(key);
        if (value == null || value.isBlank()) {
            value = dotenv.get(key);
        }
        return value == null ? "" : value.trim();
    }

    public boolean isConfigured() {
        return !baseUrl.isEmpty() && !email.isEmpty() && !apiToken.isEmpty();
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public String getEmail() {
        return email;
    }

    public String getApiToken() {
        return apiToken;
    }
}
