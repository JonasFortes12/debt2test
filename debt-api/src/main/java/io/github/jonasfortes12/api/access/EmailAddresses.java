package io.github.jonasfortes12.api.access;

import java.util.Locale;

/** Email normalization (trim + lowercase only) and masking for logs. */
public final class EmailAddresses {

    private EmailAddresses() {
    }

    public static String normalize(String raw) {
        return raw.trim().toLowerCase(Locale.ROOT);
    }

    /** {@code jonas@gmail.com} → {@code j***@gmail.com}; anything without a local part → {@code ***}. */
    public static String mask(String normalized) {
        int at = normalized.indexOf('@');
        if (at < 1) {
            return "***";
        }
        return normalized.charAt(0) + "***" + normalized.substring(at);
    }
}
