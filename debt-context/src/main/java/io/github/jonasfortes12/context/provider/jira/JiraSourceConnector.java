package io.github.jonasfortes12.context.provider.jira;

import java.util.Optional;

import io.github.jonasfortes12.context.chain.ContextHandler;
import io.github.jonasfortes12.context.extraction.IssueReferenceExtractor;
import io.github.jonasfortes12.core.context.ContextAccessCheck;
import io.github.jonasfortes12.core.context.ContextAccessCheck.Outcome;
import io.github.jonasfortes12.core.context.ContextCredentials;
import io.github.jonasfortes12.core.context.ContextSource;
import io.github.jonasfortes12.core.port.ContextEnricher;
import io.github.jonasfortes12.core.port.ContextSourceConnector;

/**
 * {@link ContextSourceConnector} for Jira Cloud: checks a site before a run and builds the run's
 * enricher. Credentials, when present, are Jira's account email (principal) and API token (secret).
 */
public final class JiraSourceConnector implements ContextSourceConnector {

    private static final String PROVIDER_ID = "jira";

    @Override
    public String providerId() {
        return PROVIDER_ID;
    }

    @Override
    public ContextAccessCheck verifyAccess(ContextSource source) {
        Outcome outcome = clientFor(source).checkAccess();
        if (outcome == Outcome.OK) {
            return ContextAccessCheck.ok();
        }
        return new ContextAccessCheck(outcome, messageFor(outcome, source.host()));
    }

    @Override
    public ContextEnricher enricherFor(ContextSource source) {
        JiraContextProvider provider = new JiraContextProvider(clientFor(source));
        return new ContextHandler(new IssueReferenceExtractor(), Optional.of(provider));
    }

    private JiraClient clientFor(ContextSource source) {
        if (!PROVIDER_ID.equals(source.providerId())) {
            throw new IllegalArgumentException("not a Jira context source: " + source.providerId());
        }
        return source.credentials()
                .map(credentials -> authenticatedClient(source.baseUrl(), credentials))
                .orElseGet(() -> new JiraClient(source.baseUrl()));
    }

    private static JiraClient authenticatedClient(String baseUrl, ContextCredentials credentials) {
        return new JiraClient(baseUrl, credentials.principal(), credentials.secret());
    }

    private static String messageFor(Outcome outcome, String host) {
        return switch (outcome) {
            case AUTH_REQUIRED -> "Jira at " + host + " requires authentication; provide email and apiToken";
            case CREDENTIALS_REJECTED -> "Jira at " + host + " rejected the supplied credentials";
            case NO_PROJECT_ACCESS -> "Jira at " + host + " accepted the credentials but they cannot browse any project";
            case NOT_A_SOURCE -> host + " does not look like a Jira site";
            case UNREACHABLE -> "could not reach Jira at " + host + " to verify access";
            case OK -> "ok";
        };
    }
}
