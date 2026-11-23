package io.github.jonasfortes12.api.access.mail;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;

/**
 * Fills the plain-text templates in {@code resources/mail/}; {@code {{key}}}
 * marks a placeholder.
 */
public final class MailTemplates {

    static final String CODE_SUBJECT = "Your Debt2Test access code";
    static final String QUOTA_SUBJECT = "Debt2Test: run limit reached";

    private MailTemplates() {
    }

    public static MailMessage codeMessage(String code, Instant expiresAt, int runsRemaining,
            String termsVersion, String frontendUrl) {
        String body = fill(load("mail/access-code.txt"), Map.of(
                "code", code,
                "linkLine", linkLine(frontendUrl, code),
                "expiresAt", expiresAt.toString(),
                "runsRemaining", Integer.toString(runsRemaining),
                "termsVersion", termsVersion));
        return new MailMessage(CODE_SUBJECT, body);
    }

    public static MailMessage quotaMessage(int runQuota) {
        String body = fill(load("mail/quota-reached.txt"), Map.of(
                "runQuota", Integer.toString(runQuota)));
        return new MailMessage(QUOTA_SUBJECT, body);
    }

    /**
     * Empty unless a front-end URL is configured: then a link that pre-fills the
     * code.
     */
    private static String linkLine(String frontendUrl, String code) {
        if (frontendUrl == null || frontendUrl.isBlank()) {
            return "";
        }
        String base = frontendUrl.endsWith("/") ? frontendUrl.substring(0, frontendUrl.length() - 1) : frontendUrl;
        return "\nOr open: " + base + "/?code=" + code + "\n";
    }

    private static String fill(String template, Map<String, String> values) {
        String filled = template;
        for (Map.Entry<String, String> value : values.entrySet()) {
            filled = filled.replace("{{" + value.getKey() + "}}", value.getValue());
        }
        return filled;
    }

    private static String load(String resource) {
        try (InputStream in = MailTemplates.class.getClassLoader().getResourceAsStream(resource)) {
            if (in == null) {
                throw new IllegalStateException("missing mail template " + resource);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException failure) {
            throw new UncheckedIOException(failure);
        }
    }
}
