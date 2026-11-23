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
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import io.github.jonasfortes12.core.context.ContextAccessCheck.Outcome;

/**
 * Minimal Jira Cloud REST client: fetches a single issue by key and checks whether the site can be
 * browsed. Works anonymously (no {@code Authorization} header) for public sites, or with Basic
 * {@code email:apiToken} auth for private ones.
 */
public final class JiraClient {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);
    /** The access check blocks an HTTP request in debt-api, so it gives up sooner than a fetch. */
    private static final Duration ACCESS_CHECK_TIMEOUT = Duration.ofSeconds(5);
    private static final String ACCESS_CHECK_PATH = "/rest/api/2/mypermissions?permissions=BROWSE_PROJECTS";
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(CONNECT_TIMEOUT)
            .build();

    private final String baseUrl;
    private final Optional<String> authorizationHeader;

    /** Anonymous client for a public Jira site. */
    public JiraClient(String baseUrl) {
        this.baseUrl = withoutTrailingSlash(Objects.requireNonNull(baseUrl, "baseUrl must not be null"));
        this.authorizationHeader = Optional.empty();
    }

    public JiraClient(String baseUrl, String email, String apiToken) {
        Objects.requireNonNull(baseUrl, "baseUrl must not be null");
        Objects.requireNonNull(email, "email must not be null");
        Objects.requireNonNull(apiToken, "apiToken must not be null");
        this.baseUrl = withoutTrailingSlash(baseUrl);
        String credentials = email + ":" + apiToken;
        this.authorizationHeader = Optional.of("Basic "
                + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8)));
    }

    private static String withoutTrailingSlash(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    /**
     * Checks whether this client (anonymous or authenticated) can browse at least one project.
     * Never throws: every failure is mapped to an {@link Outcome}.
     *
     * <p>How a private Jira Cloud site answers an anonymous probe ({@code 401}, a redirect to the
     * login page, or {@code 200} without the permission) is an unverified assumption; all three
     * map to {@link Outcome#AUTH_REQUIRED} so any of them yields the intended result.
     */
    public Outcome checkAccess() {
        HttpRequest request = requestFor(ACCESS_CHECK_PATH, ACCESS_CHECK_TIMEOUT);
        // HttpRequest.timeout only bounds the wait for response headers; the deadline on the
        // future also bounds reading the body, so a server that trickles it cannot hold the caller.
        CompletableFuture<HttpResponse<String>> pending =
                HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString());
        try {
            return accessOutcome(pending.get(ACCESS_CHECK_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS));
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            pending.cancel(true);
            return Outcome.UNREACHABLE;
        } catch (TimeoutException | ExecutionException | RuntimeException failure) {
            pending.cancel(true);
            return Outcome.UNREACHABLE;
        }
    }

    private Outcome accessOutcome(HttpResponse<String> response) {
        int status = response.statusCode();
        boolean anonymous = authorizationHeader.isEmpty();
        if (status == 200) {
            return browsePermission(response.body())
                    .map(permitted -> permitted ? Outcome.OK
                            : anonymous ? Outcome.AUTH_REQUIRED : Outcome.NO_PROJECT_ACCESS)
                    .orElse(Outcome.UNREACHABLE);
        }
        if (status == 401 || status == 403) {
            return anonymous ? Outcome.AUTH_REQUIRED : Outcome.CREDENTIALS_REJECTED;
        }
        if (status >= 300 && status <= 399) {
            return anonymous ? Outcome.AUTH_REQUIRED : Outcome.NOT_A_SOURCE;
        }
        if (status == 429 || status >= 500) {
            return Outcome.UNREACHABLE;
        }
        return Outcome.NOT_A_SOURCE;
    }

    private static Optional<Boolean> browsePermission(String body) {
        try {
            JsonObject permission = JsonParser.parseString(body).getAsJsonObject()
                    .getAsJsonObject("permissions")
                    .getAsJsonObject("BROWSE_PROJECTS");
            return Optional.of(permission.get("havePermission").getAsBoolean());
        } catch (RuntimeException malformed) {
            return Optional.empty();
        }
    }

    private HttpRequest requestFor(String pathAndQuery, Duration timeout) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + pathAndQuery))
                .timeout(timeout)
                .header("Content-Type", "application/json")
                .GET();
        authorizationHeader.ifPresent(header -> builder.header("Authorization", header));
        return builder.build();
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
        HttpRequest request = requestFor("/rest/api/2/issue/" + encodedKey, REQUEST_TIMEOUT);

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
