package io.github.jonasfortes12.core.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class UrlSanitizerTest {

    @Test
    void passesThroughNullAndBlankValues() {
        assertNull(UrlSanitizer.sanitize(null));
        assertEquals("", UrlSanitizer.sanitize(""));
        assertEquals("  ", UrlSanitizer.sanitize("  "));
    }

    @Test
    void stripsUserinfoFromAnHttpsUrlButKeepsHostAndPath() {
        assertEquals(
                "https://example.com/owner/repository",
                UrlSanitizer.sanitize("https://user:secret-token@example.com/owner/repository"));
    }

    @Test
    void keepsAPlainHttpsUrlWithoutCredentialsUnchanged() {
        assertEquals(
                "https://example.com/owner/repository",
                UrlSanitizer.sanitize("https://example.com/owner/repository"));
    }

    @Test
    void stripsQueryAndFragmentFromAScpStyleReference() {
        assertEquals(
                "example.com:owner/repository.git",
                UrlSanitizer.sanitize("git@example.com:owner/repository.git?token=secret#fragment"));
    }

    @Test
    void redactsAnOpaqueUriGenerically() {
        assertEquals("[redacted URL]", UrlSanitizer.sanitize("urn:isbn:0451450523"));
    }

    @Test
    void redactsAnAuthorityThatResolvesToNoHost() {
        assertEquals("[redacted URL]", UrlSanitizer.sanitize("https://:secret@/owner/repository"));
    }

    @Test
    void redactsAValueThatCannotBeParsedAsAUri() {
        assertEquals("[redacted URL]", UrlSanitizer.sanitize("https://exa mple.com/ has space"));
    }

    @Test
    void neverLeaksTheOriginalCredentialSubstring() {
        String sanitized = UrlSanitizer.sanitize("https://user:super-secret-token@example.com/owner/repository");
        assertEquals(false, sanitized.contains("super-secret-token"));
    }
}
