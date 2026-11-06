package io.github.jonasfortes12.context.provider.jira;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import io.github.jonasfortes12.core.context.ContextSource;

class JiraConfigTest {

    private static final String BASE_URL = "DEBT_CONTEXT_JIRA_BASE_URL";
    private static final String EMAIL = "DEBT_CONTEXT_JIRA_EMAIL";
    private static final String TOKEN = "DEBT_CONTEXT_JIRA_API_TOKEN";

    @Test
    void noVariablesMeansNoSource() {
        assertEquals(Optional.empty(), config(Map.of()).toContextSource());
    }

    @Test
    void baseUrlAloneIsAnonymousSource() {
        ContextSource source = config(Map.of(BASE_URL, "https://acme.atlassian.net")).toContextSource().orElseThrow();

        assertEquals("jira", source.providerId());
        assertEquals("https://acme.atlassian.net", source.baseUrl());
        assertFalse(source.authenticated());
    }

    @Test
    void fullTripleIsAuthenticatedSource() {
        ContextSource source = config(Map.of(BASE_URL, "https://acme.atlassian.net", EMAIL, "dev@x.com", TOKEN, "tok"))
                .toContextSource().orElseThrow();

        assertTrue(source.authenticated());
    }

    @Test
    void unpairedCredentialsFail() {
        IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
                () -> config(Map.of(BASE_URL, "https://acme.atlassian.net", EMAIL, "dev@x.com")).toContextSource());

        assertEquals("DEBT_CONTEXT_JIRA_EMAIL and DEBT_CONTEXT_JIRA_API_TOKEN must be set together",
                failure.getMessage());
    }

    @Test
    void credentialsWithoutBaseUrlFail() {
        IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
                () -> config(Map.of(EMAIL, "dev@x.com", TOKEN, "tok")).toContextSource());

        assertEquals("DEBT_CONTEXT_JIRA_BASE_URL is required when Jira credentials are set", failure.getMessage());
    }

    private static JiraConfig config(Map<String, String> environment) {
        return new JiraConfig(environment::get);
    }
}
