package io.github.jonasfortes12.context.provider.jira;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import io.github.jonasfortes12.core.context.ContextAccessCheck;
import io.github.jonasfortes12.core.context.ContextAccessCheck.Outcome;
import io.github.jonasfortes12.core.context.ContextCredentials;
import io.github.jonasfortes12.core.context.ContextSource;
import io.github.jonasfortes12.core.model.ClassifiedDebt;
import io.github.jonasfortes12.core.model.ContextRequest;
import io.github.jonasfortes12.core.model.ContextStatus;
import io.github.jonasfortes12.core.model.ItemStatus;
import io.github.jonasfortes12.core.model.Provenance;
import io.github.jonasfortes12.core.model.SatdCandidate;
import io.github.jonasfortes12.core.model.SourceProvenance;

class JiraSourceConnectorTest {

    private static final String PERMITTED =
            "{\"permissions\":{\"BROWSE_PROJECTS\":{\"havePermission\":true}}}";

    private final JiraSourceConnector connector = new JiraSourceConnector();

    @Test
    void providerIdIsJira() {
        assertEquals("jira", connector.providerId());
    }

    @Test
    void verifyReturnsOkWhenBrowsePermitted() throws Exception {
        try (FakeJiraServer server = new FakeJiraServer(200, PERMITTED)) {
            assertTrue(connector.verifyAccess(anonymous(server)).passed());
        }
    }

    @Test
    void verifyAuthRequiredMessageNamesHostOnly() throws Exception {
        try (FakeJiraServer server = new FakeJiraServer(401, "{}")) {
            ContextAccessCheck check = connector.verifyAccess(anonymous(server));

            assertEquals(Outcome.AUTH_REQUIRED, check.outcome());
            assertEquals("Jira at localhost requires authentication; provide email and apiToken", check.message());
        }
    }

    @Test
    void verifyRejectedCredentialsMessageOmitsSecrets() throws Exception {
        try (FakeJiraServer server = new FakeJiraServer(401, "{}")) {
            ContextAccessCheck check = connector.verifyAccess(authenticated(server));

            assertEquals(Outcome.CREDENTIALS_REJECTED, check.outcome());
            assertEquals("Jira at localhost rejected the supplied credentials", check.message());
            assertFalse(check.message().contains("dev@x.com"));
            assertFalse(check.message().contains("s3cr3t"));
        }
    }

    @Test
    void verifyNoProjectAccessMessage() throws Exception {
        try (FakeJiraServer server = new FakeJiraServer(200,
                "{\"permissions\":{\"BROWSE_PROJECTS\":{\"havePermission\":false}}}")) {
            assertEquals("Jira at localhost accepted the credentials but they cannot browse any project",
                    connector.verifyAccess(authenticated(server)).message());
        }
    }

    @Test
    void verifyNotASourceMessage() throws Exception {
        try (FakeJiraServer server = new FakeJiraServer(404, "<html>")) {
            assertEquals("localhost does not look like a Jira site",
                    connector.verifyAccess(anonymous(server)).message());
        }
    }

    @Test
    void verifyUnreachableMessage() throws Exception {
        try (FakeJiraServer server = new FakeJiraServer(503, "{}")) {
            ContextAccessCheck check = connector.verifyAccess(anonymous(server));

            assertEquals(Outcome.UNREACHABLE, check.outcome());
            assertEquals("could not reach Jira at localhost to verify access", check.message());
        }
    }

    @Test
    void enricherForFetchesIssuesThroughTheSource() throws Exception {
        try (FakeJiraServer server = new FakeJiraServer(200,
                "{\"key\":\"ABC-1\",\"fields\":{\"summary\":\"s\",\"status\":{\"name\":\"Done\"}}}")) {
            var result = connector.enricherFor(anonymous(server))
                    .enrich(List.of(classifiedDebt("TODO ABC-1 fix this")), new ContextRequest(true));

            assertEquals(ContextStatus.MATCHED, result.enrichments().get(0).contextStatus());
            assertEquals("/rest/api/2/issue/ABC-1", server.requestPath());
        }
    }

    @Test
    void rejectsForeignProvider() {
        ContextSource github = new ContextSource("github", "https://github.com", Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> connector.verifyAccess(github));
        assertThrows(IllegalArgumentException.class, () -> connector.enricherFor(github));
    }

    private static ContextSource anonymous(FakeJiraServer server) {
        return new ContextSource("jira", server.endpoint(), Optional.empty());
    }

    private static ContextSource authenticated(FakeJiraServer server) {
        return new ContextSource("jira", server.endpoint(), ContextCredentials.optionalOf("dev@x.com", "s3cr3t"));
    }

    private static ClassifiedDebt classifiedDebt(String comment) {
        SatdCandidate candidate = new SatdCandidate("candidate-1", "src/Example.java", "example", 10, comment,
                "void example() {}", new SourceProvenance("https://example.test/repository", "main", "src/Example.java"));
        return new ClassifiedDebt(candidate, true, "DESIGN", 0.9, new Provenance("test", "fixture", "1"),
                ItemStatus.CLASSIFIED, List.of());
    }
}
