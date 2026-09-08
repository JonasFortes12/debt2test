package io.github.jonasfortes12.context.provider.jira;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

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

    private static final class FakeJiraServer implements AutoCloseable {
        private final HttpServer server;
        private final int status;
        private final String response;
        private volatile HttpExchange exchange;

        private FakeJiraServer(int status, String response) throws IOException {
            this.status = status;
            this.response = response;
            this.server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
            this.server.createContext("/", this::handle);
            this.server.start();
        }

        private void handle(HttpExchange exchange) throws IOException {
            this.exchange = exchange;
            byte[] body = response.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(status, body.length);
            try (OutputStream output = exchange.getResponseBody()) {
                output.write(body);
            }
        }

        private String endpoint() {
            return "http://localhost:" + server.getAddress().getPort();
        }

        private String requestPath() {
            return exchange.getRequestURI().getPath();
        }

        private String header(String name) {
            return exchange.getRequestHeaders().getFirst(name);
        }

        @Override
        public void close() {
            server.stop(0);
        }
    }
}
