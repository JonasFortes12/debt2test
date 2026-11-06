package io.github.jonasfortes12.context.provider.jira;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;

import org.junit.jupiter.api.Test;

import io.github.jonasfortes12.core.context.ContextAccessCheck.Outcome;


class JiraClientTest {

    @Test
    void constructorRejectsNullArguments() {
        assertThrows(NullPointerException.class, () -> new JiraClient(null, "e@x.com", "token"));
        assertThrows(NullPointerException.class, () -> new JiraClient("http://x", null, "token"));
        assertThrows(NullPointerException.class, () -> new JiraClient("http://x", "e@x.com", null));
    }

    @Test
    void getIssueParsesFieldsAndSendsBasicAuth() throws Exception {
        FakeJiraServer server = new FakeJiraServer(200, """
                {"key":"DEBT2TEST-1","fields":{"summary":"Fix login authentication bug",
                "description":"Users cannot login with SSO","status":{"name":"To Do"}}}""");
        try {
            JiraClient client = new JiraClient(server.endpoint(), "e@x.com", "api-token");

            JiraIssue issue = client.getIssue("DEBT2TEST-1");

            assertEquals("DEBT2TEST-1", issue.key());
            assertEquals("Fix login authentication bug", issue.summary());
            assertEquals("Users cannot login with SSO", issue.description());
            assertEquals("To Do", issue.status());
            assertEquals(server.endpoint() + "/browse/DEBT2TEST-1", issue.url());
            assertEquals("/rest/api/2/issue/DEBT2TEST-1", server.requestPath());

            String expectedAuth = "Basic " + Base64.getEncoder()
                    .encodeToString("e@x.com:api-token".getBytes(StandardCharsets.UTF_8));
            assertEquals(expectedAuth, server.header("Authorization"));
        } finally {
            server.close();
        }
    }

    @Test
    void getIssueTreatsMissingDescriptionAsNull() throws Exception {
        FakeJiraServer server = new FakeJiraServer(200,
                "{\"key\":\"OPS-1\",\"fields\":{\"summary\":\"s\",\"status\":{\"name\":\"Done\"}}}");
        try {
            JiraIssue issue = new JiraClient(server.endpoint(), "e@x.com", "token").getIssue("OPS-1");
            assertNull(issue.description());
        } finally {
            server.close();
        }
    }

    @Test
    void getIssueReturnsNullOnNotFound() throws Exception {
        FakeJiraServer server = new FakeJiraServer(404, "{\"errorMessages\":[\"Issue does not exist\"]}");
        try {
            assertNull(new JiraClient(server.endpoint(), "e@x.com", "token").getIssue("OPS-404"));
        } finally {
            server.close();
        }
    }

    @Test
    void getIssueThrowsUnauthorizedOnAuthFailure() throws Exception {
        assertFailure(401, "UNAUTHORIZED", false);
        assertFailure(403, "UNAUTHORIZED", false);
    }

    @Test
    void getIssueThrowsRateLimitedAndMarksRecoverable() throws Exception {
        assertFailure(429, "RATE_LIMITED", true);
    }

    @Test
    void getIssueThrowsServerErrorAndMarksRecoverable() throws Exception {
        assertFailure(503, "JIRA_HTTP_SERVER_ERROR", true);
    }

    @Test
    void getIssueThrowsResponseInvalidOnMalformedBody() throws Exception {
        FakeJiraServer server = new FakeJiraServer(200, "not json");
        try {
            JiraFetchException failure = assertThrows(JiraFetchException.class,
                    () -> new JiraClient(server.endpoint(), "e@x.com", "token").getIssue("OPS-1"));
            assertEquals("JIRA_RESPONSE_INVALID", failure.code());
            assertFalse(failure.recoverable());
        } finally {
            server.close();
        }
    }

    private void assertFailure(int status, String expectedCode, boolean expectedRecoverable) throws Exception {
        FakeJiraServer server = new FakeJiraServer(status, "{}");
        try {
            JiraFetchException failure = assertThrows(JiraFetchException.class,
                    () -> new JiraClient(server.endpoint(), "e@x.com", "token").getIssue("OPS-1"));
            assertEquals(expectedCode, failure.code());
            assertEquals(expectedRecoverable, failure.recoverable());
        } finally {
            server.close();
        }
    }

    @Test
    void checkAccessIsOkWhenBrowsePermitted() throws Exception {
        try (FakeJiraServer server = new FakeJiraServer(200, permissionBody(true))) {
            assertEquals(Outcome.OK, new JiraClient(server.endpoint()).checkAccess());
            assertEquals("/rest/api/2/mypermissions", server.requestPath());
            assertEquals("permissions=BROWSE_PROJECTS", server.query());
            assertNull(server.header("Authorization"));
        }
    }

    @Test
    void anonymousWithoutBrowsePermissionNeedsAuth() throws Exception {
        assertAnonymous(200, permissionBody(false), Outcome.AUTH_REQUIRED);
    }

    @Test
    void authenticatedWithoutBrowsePermissionHasNoProjectAccess() throws Exception {
        assertAuthenticated(200, permissionBody(false), Outcome.NO_PROJECT_ACCESS);
    }

    @Test
    void anonymous401NeedsAuth() throws Exception {
        assertAnonymous(401, "{}", Outcome.AUTH_REQUIRED);
        assertAnonymous(403, "{}", Outcome.AUTH_REQUIRED);
    }

    @Test
    void authenticated401IsRejected() throws Exception {
        assertAuthenticated(401, "{}", Outcome.CREDENTIALS_REJECTED);
        assertAuthenticated(403, "{}", Outcome.CREDENTIALS_REJECTED);
    }

    @Test
    void anonymousRedirectNeedsAuth() throws Exception {
        assertAnonymous(302, "", Outcome.AUTH_REQUIRED);
    }

    @Test
    void authenticatedRedirectIsNotASource() throws Exception {
        assertAuthenticated(302, "", Outcome.NOT_A_SOURCE);
    }

    @Test
    void notFoundIsNotASource() throws Exception {
        assertAnonymous(404, "<html>", Outcome.NOT_A_SOURCE);
        assertAuthenticated(404, "<html>", Outcome.NOT_A_SOURCE);
    }

    @Test
    void rateLimitedIsUnreachable() throws Exception {
        assertAnonymous(429, "{}", Outcome.UNREACHABLE);
    }

    @Test
    void serverErrorIsUnreachable() throws Exception {
        assertAnonymous(503, "{}", Outcome.UNREACHABLE);
    }

    @Test
    void malformedBodyIsUnreachable() throws Exception {
        assertAnonymous(200, "not json", Outcome.UNREACHABLE);
        assertAnonymous(200, "{\"permissions\":{}}", Outcome.UNREACHABLE);
    }

    @Test
    void closedPortIsUnreachable() throws Exception {
        String endpoint;
        try (FakeJiraServer server = new FakeJiraServer(200, "{}")) {
            endpoint = server.endpoint();
        }
        assertEquals(Outcome.UNREACHABLE, new JiraClient(endpoint).checkAccess());
    }

    @Test
    void anonymousGetIssueSendsNoAuthorizationHeader() throws Exception {
        try (FakeJiraServer server = new FakeJiraServer(200,
                "{\"key\":\"OPS-1\",\"fields\":{\"summary\":\"s\",\"status\":{\"name\":\"Done\"}}}")) {
            new JiraClient(server.endpoint()).getIssue("OPS-1");
            assertNull(server.header("Authorization"));
        }
    }

    @Test
    void authenticatedCheckSendsBasicAuth() throws Exception {
        try (FakeJiraServer server = new FakeJiraServer(200, permissionBody(true))) {
            new JiraClient(server.endpoint(), "e@x.com", "api-token").checkAccess();
            String expectedAuth = "Basic " + Base64.getEncoder()
                    .encodeToString("e@x.com:api-token".getBytes(StandardCharsets.UTF_8));
            assertEquals(expectedAuth, server.header("Authorization"));
        }
    }

    @Test
    void stalledBodyIsUnreachableWithinTheCheckTimeout() throws Exception {
        try (ServerSocket server = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
            Thread stalling = Thread.ofVirtual().start(() -> sendHeadersThenStall(server));
            long started = System.nanoTime();

            Outcome outcome = new JiraClient("http://localhost:" + server.getLocalPort()).checkAccess();

            long elapsedSeconds = Duration.ofNanos(System.nanoTime() - started).toSeconds();
            assertEquals(Outcome.UNREACHABLE, outcome);
            assertTrue(elapsedSeconds < 8, "check must give up after its timeout, took " + elapsedSeconds + " s");
            stalling.interrupt();
        }
    }

    /** Answers 200 with a promised 100-byte body, then never sends it. */
    private static void sendHeadersThenStall(ServerSocket server) {
        try (Socket client = server.accept()) {
            client.getOutputStream().write(
                    "HTTP/1.1 200 OK\r\nContent-Length: 100\r\n\r\n".getBytes(StandardCharsets.US_ASCII));
            client.getOutputStream().flush();
            Thread.sleep(Duration.ofSeconds(30));
        } catch (IOException | InterruptedException ignored) {
            // test finished or server closed
        }
    }

    private static String permissionBody(boolean havePermission) {
        return "{\"permissions\":{\"BROWSE_PROJECTS\":{\"key\":\"BROWSE_PROJECTS\",\"havePermission\":"
                + havePermission + "}}}";
    }

    private static void assertAnonymous(int status, String body, Outcome expected) throws Exception {
        try (FakeJiraServer server = new FakeJiraServer(status, body)) {
            assertEquals(expected, new JiraClient(server.endpoint()).checkAccess());
        }
    }

    private static void assertAuthenticated(int status, String body, Outcome expected) throws Exception {
        try (FakeJiraServer server = new FakeJiraServer(status, body)) {
            assertEquals(expected, new JiraClient(server.endpoint(), "e@x.com", "token").checkAccess());
        }
    }
}
