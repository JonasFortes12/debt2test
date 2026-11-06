package io.github.jonasfortes12.core.context;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

import io.github.jonasfortes12.core.util.UrlSanitizer;

/**
 * Where a run's issue-tracker context comes from: which provider, which instance, and optionally
 * which credentials.
 *
 * <p>Runtime configuration passed alongside a {@code PipelineRequest}, never inside it, because it
 * may carry credentials (see {@code architecture.md} §14). {@link #toString()} never prints them.
 */
public record ContextSource(String providerId, String baseUrl, Optional<ContextCredentials> credentials) {

    private static final String INVALID_BASE_URL = "baseUrl must be an absolute http or https URL";

    public ContextSource {
        providerId = normaliseProviderId(providerId);
        baseUrl = normaliseBaseUrl(baseUrl);
        Objects.requireNonNull(credentials, "credentials must not be null");
    }

    /** Host name of {@link #baseUrl()}, safe to show in messages. */
    public String host() {
        return URI.create(baseUrl).getHost();
    }

    public boolean authenticated() {
        return credentials.isPresent();
    }

    @Override
    public String toString() {
        return "ContextSource[providerId=" + providerId
                + ", baseUrl=" + UrlSanitizer.sanitize(baseUrl)
                + ", credentials=" + (authenticated() ? "present" : "absent") + "]";
    }

    private static String normaliseProviderId(String providerId) {
        if (providerId == null || providerId.isBlank()) {
            throw new IllegalArgumentException("providerId must not be blank");
        }
        return providerId.trim().toLowerCase(Locale.ROOT);
    }

    private static String normaliseBaseUrl(String baseUrl) {
        if (baseUrl == null) {
            throw new IllegalArgumentException(INVALID_BASE_URL);
        }
        String trimmed = baseUrl.trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        if (!isAbsoluteHttpUrl(trimmed)) {
            throw new IllegalArgumentException(INVALID_BASE_URL);
        }
        return trimmed;
    }

    private static boolean isAbsoluteHttpUrl(String value) {
        try {
            URI uri = new URI(value);
            String scheme = uri.getScheme();
            boolean httpScheme = "http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme);
            boolean hasHost = uri.getHost() != null && !uri.getHost().isBlank();
            // Credentials in the URL would end up in issue links that are persisted and returned;
            // a query or fragment would corrupt every API path appended to the base URL.
            boolean onlySchemeHostPortPath = uri.getRawUserInfo() == null
                    && uri.getRawQuery() == null && uri.getRawFragment() == null;
            return httpScheme && hasHost && onlySchemeHostPortPath;
        } catch (URISyntaxException malformed) {
            return false;
        }
    }
}
