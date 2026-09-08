package io.github.jonasfortes12.context.provider.jira;

import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

import io.github.jonasfortes12.context.provider.ContextProvider;
import io.github.jonasfortes12.context.provider.ProviderResolution;
import io.github.jonasfortes12.core.model.ExternalReference;
import io.github.jonasfortes12.core.model.ExternalTaskSpec;
import io.github.jonasfortes12.core.model.PipelineError;

/**
 * {@link ContextProvider} adapter backed by the Jira Cloud REST API.
 */
public final class JiraContextProvider implements ContextProvider {

    private static final Pattern ISSUE_KEY = Pattern.compile("^[A-Z][A-Z0-9]{1,9}-[0-9]+$");
    private static final String PROVIDER_ID = "jira";

    private final JiraClient client;

    public JiraContextProvider(JiraClient client) {
        this.client = Objects.requireNonNull(client, "client must not be null");
    }

    @Override
    public String providerId() {
        return PROVIDER_ID;
    }

    @Override
    public boolean supports(ExternalReference reference) {
        if (reference == null || reference.value() == null) {
            return false;
        }
        return ISSUE_KEY.matcher(reference.value()).matches();
    }

    @Override
    public ProviderResolution fetch(ExternalReference reference) {
        try {
            JiraIssue issue = client.getIssue(reference.value());
            if (issue == null) {
                return ProviderResolution.notFound();
            }
            return ProviderResolution.matched(toTaskSpec(issue));
        } catch (JiraFetchException fetchFailure) {
            return ProviderResolution.failed(new PipelineError(
                    "context", fetchFailure.code(), "Jira issue fetch failed.", null, fetchFailure.recoverable()));
        } catch (RuntimeException unexpected) {
            return ProviderResolution.failed(new PipelineError(
                    "context", "JIRA_UNEXPECTED_ERROR", "Unexpected error while fetching Jira issue.",
                    null, false));
        }
    }

    private ExternalTaskSpec toTaskSpec(JiraIssue issue) {
        return new ExternalTaskSpec(
                PROVIDER_ID,
                issue.key(),
                issue.summary(),
                issue.description(),
                List.of(),
                List.of(),
                issue.url());
    }
}
