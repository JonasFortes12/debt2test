package io.github.jonasfortes12.context.provider.jira;

import java.util.Optional;
import java.util.function.UnaryOperator;

import io.github.cdimascio.dotenv.Dotenv;
import io.github.jonasfortes12.core.context.ContextCredentials;
import io.github.jonasfortes12.core.context.ContextSource;

/**
 * Reads the CLI's Jira context source from environment variables or a local {@code .env} file.
 * The base URL alone means a public (anonymous) Jira; email and API token, set together, are for a
 * private one. See {@link #toContextSource()}.
 */
public final class JiraConfig {

    private final String baseUrl;
    private final String email;
    private final String apiToken;

    private static final String BASE_URL_VARIABLE = "DEBT_CONTEXT_JIRA_BASE_URL";
    private static final String EMAIL_VARIABLE = "DEBT_CONTEXT_JIRA_EMAIL";
    private static final String API_TOKEN_VARIABLE = "DEBT_CONTEXT_JIRA_API_TOKEN";

    /** Reads a process environment variable first, falling back to a local {@code .env}. */
    public JiraConfig() {
        this(processEnvironmentThenDotenv());
    }

    public JiraConfig(UnaryOperator<String> environment) {
        this.baseUrl = valueOf(environment, BASE_URL_VARIABLE);
        this.email = valueOf(environment, EMAIL_VARIABLE);
        this.apiToken = valueOf(environment, API_TOKEN_VARIABLE);
    }

    /**
     * The configured Jira source, or empty when no base URL is set (context is then skipped).
     *
     * @throws IllegalArgumentException if credentials are set without a base URL, if only one of
     *                                  email and API token is set, or if the base URL is malformed
     */
    public Optional<ContextSource> toContextSource() {
        boolean credentialsSet = !email.isEmpty() || !apiToken.isEmpty();
        if (baseUrl.isEmpty()) {
            if (credentialsSet) {
                throw new IllegalArgumentException(BASE_URL_VARIABLE + " is required when Jira credentials are set");
            }
            return Optional.empty();
        }
        return Optional.of(new ContextSource("jira", baseUrl, credentials()));
    }

    private Optional<ContextCredentials> credentials() {
        try {
            return ContextCredentials.optionalOf(email, apiToken);
        } catch (IllegalArgumentException unpaired) {
            throw new IllegalArgumentException(
                    EMAIL_VARIABLE + " and " + API_TOKEN_VARIABLE + " must be set together");
        }
    }

    private static String valueOf(UnaryOperator<String> environment, String key) {
        String value = environment.apply(key);
        return value == null ? "" : value.trim();
    }

    private static UnaryOperator<String> processEnvironmentThenDotenv() {
        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
        return key -> {
            String value = System.getenv(key);
            return value == null || value.isBlank() ? dotenv.get(key) : value;
        };
    }
}
