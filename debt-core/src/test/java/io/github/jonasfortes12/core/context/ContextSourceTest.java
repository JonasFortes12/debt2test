package io.github.jonasfortes12.core.context;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ContextSourceTest {

    @Test
    void normalisesProviderAndBaseUrl() {
        ContextSource source = new ContextSource(" JIRA ", " https://acme.atlassian.net/ ", Optional.empty());

        assertEquals("jira", source.providerId());
        assertEquals("https://acme.atlassian.net", source.baseUrl());
        assertEquals("acme.atlassian.net", source.host());
        assertFalse(source.authenticated());
    }

    @ParameterizedTest
    @ValueSource(strings = { "not a url", "ftp://acme", "https://", "/relative", "" })
    void rejectsMalformedBaseUrl(String url) {
        IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
                () -> new ContextSource("jira", url, Optional.empty()));

        assertEquals("baseUrl must be an absolute http or https URL", failure.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "https://dev%40acme.com:TOKEN@acme.atlassian.net",
            "https://user@acme.atlassian.net",
            "https://acme.atlassian.net?x=1",
            "https://acme.atlassian.net#frag" })
    void rejectsBaseUrlWithUserInfoQueryOrFragment(String url) {
        IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
                () -> new ContextSource("jira", url, Optional.empty()));

        assertEquals("baseUrl must be an absolute http or https URL", failure.getMessage());
        assertFalse(failure.getMessage().contains("TOKEN"));
    }

    @Test
    void rejectsBlankProvider() {
        assertThrows(IllegalArgumentException.class,
                () -> new ContextSource(" ", "https://acme.atlassian.net", Optional.empty()));
    }

    @Test
    void blankCredentialPairIsAbsent() {
        assertEquals(Optional.empty(), ContextCredentials.optionalOf("", " "));
        assertEquals(Optional.empty(), ContextCredentials.optionalOf(null, null));
    }

    @Test
    void halfCredentialPairIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> ContextCredentials.optionalOf("dev@x.com", ""));
        assertThrows(IllegalArgumentException.class, () -> ContextCredentials.optionalOf(null, "tok"));
    }

    @Test
    void completeCredentialPairMakesTheSourceAuthenticated() {
        ContextSource source = new ContextSource("jira", "https://acme.atlassian.net",
                ContextCredentials.optionalOf("dev@x.com", "tok"));

        assertTrue(source.authenticated());
        assertEquals("dev@x.com", source.credentials().orElseThrow().principal());
    }

    @Test
    void toStringNeverRevealsCredentials() {
        ContextSource source = new ContextSource("jira", "https://acme.atlassian.net",
                ContextCredentials.optionalOf("dev@x.com", "s3cr3t-token"));

        assertFalse(source.toString().contains("dev@x.com"));
        assertFalse(source.toString().contains("s3cr3t-token"));
        assertTrue(source.toString().contains("credentials=present"));
        assertFalse(source.credentials().orElseThrow().toString().contains("s3cr3t-token"));
        assertFalse(source.credentials().orElseThrow().toString().contains("dev@x.com"));
    }

    @Test
    void okCheckPasses() {
        assertTrue(ContextAccessCheck.ok().passed());
        assertFalse(new ContextAccessCheck(ContextAccessCheck.Outcome.UNREACHABLE, "x").passed());
    }
}
