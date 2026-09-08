package io.github.jonasfortes12.context.provider.jira;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Objects;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Minimal Jira Cloud REST client for fetching a single issue by key.
 */
public final class JiraClient {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(CONNECT_TIMEOUT)
            .build();

    private final String baseUrl;
    private final String authorizationHeader;

    public JiraClient(String baseUrl, String email, String apiToken) {
        Objects.requireNonNull(baseUrl, "baseUrl must not be null");
        Objects.requireNonNull(email, "email must not be null");
        Objects.requireNonNull(apiToken, "apiToken must not be null");
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        String credentials = email + ":" + apiToken;
        this.authorizationHeader = "Basic "
                + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Fetches an issue by key.
     *
     * <p>Uses REST API v2 rather than v3: v2 returns {@code description} as a plain
     * (wiki-markup) string, while v3 returns it as an Atlassian Document Format JSON
     * object that {@link #parseIssue(String)} does not parse.
     *
     * @return the issue, or {@code null} if Jira reports it does not exist (HTTP 404) -
     *         that is a normal outcome, not an error.
     * @throws JiraFetchException if the request fails for any other reason (auth, rate limiting,
     *                            transport, or a malformed response).
     */
    public JiraIssue getIssue(String issueKey) throws JiraFetchException {
        Objects.requireNonNull(issueKey, "issueKey must not be null");

        String encodedKey = URLEncoder.encode(issueKey, StandardCharsets.UTF_8).replace("+", "%20");
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/rest/api/2/issue/" + encodedKey))
                .timeout(REQUEST_TIMEOUT)
                .header("Authorization", authorizationHeader)
                .header("Content-Type", "application/json")
                .GET()
                .build();

        HttpResponse<String> response = send(request);
        if (response.statusCode() == 404) {
            return null;
        }
        requireSuccessful(response);
        return parseIssue(response.body());
    }

    private HttpResponse<String> send(HttpRequest request) throws JiraFetchException {
        try {
            return HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new JiraFetchException("JIRA_REQUEST_INTERRUPTED", false);
        } catch (IOException transportFailure) {
            throw new JiraFetchException("JIRA_TRANSPORT_FAILED", true);
        }
    }

    private void requireSuccessful(HttpResponse<String> response) throws JiraFetchException {
        int status = response.statusCode();
        if (status == 200) {
            return;
        }
        if (status == 401 || status == 403) {
            throw new JiraFetchException("UNAUTHORIZED", false);
        }
        if (status == 429) {
            throw new JiraFetchException("RATE_LIMITED", true);
        }
        if (status >= 500 && status <= 599) {
            throw new JiraFetchException("JIRA_HTTP_SERVER_ERROR", true);
        }
        throw new JiraFetchException("JIRA_HTTP_FAILED", false);
    }

    private JiraIssue parseIssue(String body) throws JiraFetchException {
        try {
            JsonElement parsed = JsonParser.parseString(body);
            JsonObject root = parsed.getAsJsonObject();
            String key = root.get("key").getAsString();
            JsonObject fields = root.getAsJsonObject("fields");
            String summary = fields.get("summary").getAsString();
            String description = fields.has("description") && !fields.get("description").isJsonNull()
                    ? fields.get("description").getAsString()
                    : null;
            String status = fields.getAsJsonObject("status").get("name").getAsString();
            String url = baseUrl + "/browse/" + key;
            return new JiraIssue(key, summary, description, status, url);
        } catch (RuntimeException malformed) {
            throw new JiraFetchException("JIRA_RESPONSE_INVALID", false);
        }
    }
}
