package io.github.jonasfortes12.context.provider.jira;

import static io.github.jonasfortes12.core.util.Validation.requireText;

/**
 * A Jira issue as fetched from the Jira Cloud REST API.
 */
public record JiraIssue(String key, String summary, String description, String status, String url) {

    public JiraIssue {
        requireText(key, "key");
        requireText(summary, "summary");
        requireText(status, "status");
        requireText(url, "url");
    }
}
