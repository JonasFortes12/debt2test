package io.github.jonasfortes12.extractor;

import java.net.URI;

final class RepositoryUrlSanitizer {

    private RepositoryUrlSanitizer() {
    }

    static String sanitize(String repositoryUrl) {
        try {
            URI uri = URI.create(repositoryUrl);
            if (uri.getScheme() == null || uri.isOpaque()) {
                return sanitizeScpStyleUrl(repositoryUrl);
            }

            String authority = uri.getRawAuthority();
            String host = uri.getHost();
            if (authority != null && !authority.isEmpty() && host == null) {
                return "[redacted repository URL]";
            }

            StringBuilder sanitized = new StringBuilder();
            sanitized.append(uri.getScheme()).append(':');
            if (authority != null || repositoryUrl.startsWith(uri.getScheme() + "://")) {
                sanitized.append("//");
                if (host != null) {
                    if (host.indexOf(':') >= 0 && !host.startsWith("[")) {
                        sanitized.append('[').append(host).append(']');
                    } else {
                        sanitized.append(host);
                    }
                    if (uri.getPort() >= 0) {
                        sanitized.append(':').append(uri.getPort());
                    }
                }
            }
            if (uri.getRawPath() != null) {
                sanitized.append(uri.getRawPath());
            }
            return sanitized.toString();
        } catch (IllegalArgumentException exception) {
            return "[redacted repository URL]";
        }
    }

    private static String sanitizeScpStyleUrl(String repositoryUrl) {
        int queryStart = repositoryUrl.indexOf('?');
        int fragmentStart = repositoryUrl.indexOf('#');
        int suffixStart = queryStart < 0
                ? fragmentStart
                : fragmentStart < 0 ? queryStart : Math.min(queryStart, fragmentStart);
        String identifier = suffixStart < 0 ? repositoryUrl : repositoryUrl.substring(0, suffixStart);
        int at = identifier.lastIndexOf('@');
        int identifierStart = at > 0 ? at + 1 : 0;
        return identifier.substring(identifierStart);
    }
}
