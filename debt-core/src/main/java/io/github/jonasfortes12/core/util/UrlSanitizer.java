package io.github.jonasfortes12.core.util;

import java.net.URI;

public final class UrlSanitizer {

    private UrlSanitizer() {
    }

    public static String sanitize(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }
        if (isScpLike(value)) {
            return sanitizeSchemeLess(value);
        }
        try {
            URI uri = URI.create(value);
            if (uri.getScheme() == null) {
                return sanitizeSchemeLess(value);
            }
            if (uri.isOpaque()) {
                return "[redacted URL]";
            }
            String host = uri.getHost();
            if (uri.getRawAuthority() != null && host == null) {
                return "[redacted URL]";
            }

            StringBuilder sanitized = new StringBuilder(uri.getScheme()).append(':');
            if (uri.getRawAuthority() != null || value.startsWith(uri.getScheme() + "://")) {
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
        } catch (IllegalArgumentException ignored) {
            return "[redacted URL]";
        }
    }

    private static String sanitizeSchemeLess(String value) {
        String withoutSuffix = stripQueryAndFragment(value);
        int at = withoutSuffix.lastIndexOf('@');
        return at >= 0 ? withoutSuffix.substring(at + 1) : withoutSuffix;
    }

    private static boolean isScpLike(String value) {
        return !value.contains("://") && value.indexOf('@') >= 0 && value.indexOf(':') >= 0;
    }

    private static String stripQueryAndFragment(String value) {
        int query = value.indexOf('?');
        int fragment = value.indexOf('#');
        int suffix = query < 0 ? fragment : fragment < 0 ? query : Math.min(query, fragment);
        return suffix < 0 ? value : value.substring(0, suffix);
    }
}
