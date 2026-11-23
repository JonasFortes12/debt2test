package io.github.jonasfortes12.api.access.mail;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;

import org.junit.jupiter.api.Test;

class MailTemplatesTest {

    private static final String CODE = "D2T-ABCD-EFGH-JKMN-PQRS-TVWX-YZ01-2345";
    private static final Instant EXPIRES = Instant.parse("2026-11-09T12:00:00Z");

    @Test
    void codeMessageContainsCodeExpiryRunsAndTermsVersion() {
        MailMessage message = MailTemplates.codeMessage(CODE, EXPIRES, 2, "v1", "");

        assertEquals("Your Debt2Test access code", message.subject());
        assertTrue(message.body().contains("Hello,"));
        assertTrue(message.body().contains(CODE));
        assertTrue(message.body().contains("2026-11-09T12:00:00Z"));
        assertTrue(message.body().contains("2"));
        assertTrue(message.body().contains("v1"));
        assertFalse(message.body().contains("{{"), "every placeholder is filled");
    }

    @Test
    void codeMessageIncludesLinkOnlyWhenFrontendUrlSet() {
        assertFalse(MailTemplates.codeMessage(CODE, EXPIRES, 2, "v1", "").body().contains("?code="));
        assertFalse(MailTemplates.codeMessage(CODE, EXPIRES, 2, "v1", null).body().contains("?code="));

        String body = MailTemplates.codeMessage(CODE, EXPIRES, 2, "v1", "https://debt2test.example/").body();
        assertTrue(body.contains("https://debt2test.example/?code=" + CODE), body);
    }

    @Test
    void quotaMessageStatesLimitAndHasNoCode() {
        MailMessage message = MailTemplates.quotaMessage(3);

        assertEquals("Debt2Test: run limit reached", message.subject());
        assertTrue(message.body().contains("3"));
        assertFalse(message.body().contains("D2T-"));
        assertFalse(message.body().contains("{{"));
    }
}
