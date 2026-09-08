package io.github.jonasfortes12.context.provider.jira;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.HttpServer;

import io.github.jonasfortes12.context.provider.ProviderResolution;
import io.github.jonasfortes12.core.model.ExternalReference;
import io.github.jonasfortes12.core.model.ExternalTaskSpec;

class JiraContextProviderTest {

    @Test
    void constructorRejectsNullClient() {
        assertThrows(NullPointerException.class, () -> new JiraContextProvider(null));
    }

    @Test
    void providerIdIsJira() {
        assertEquals("jira", provider().providerId());
    }

    @Test
    void supportsMatchesJiraIssueKeysOnly() {
        JiraContextProvider provider = provider();

        assertTrue(provider.supports(new ExternalReference("DEBT2TEST-1", "comment")));
        assertTrue(provider.supports(new ExternalReference("OPS-12", "comment")));
        assertFalse(provider.supports(new ExternalReference("not-a-key", "comment")));
        assertFalse(provider.supports(new ExternalReference("https://trello.com/c/abc", "comment")));
        assertFalse(provider.supports(null));
    }

    @Test
    void fetchReturnsMatchedTaskWhenIssueFound() throws Exception {
        FakeJiraServer server = new FakeJiraServer(200, """
                {"key":"DEBT2TEST-1","fields":{"summary":"Fix login authentication bug",
                "description":"Users cannot login with SSO","status":{"name":"To Do"}}}""");
        try {
            JiraContextProvider provider = new JiraContextProvider(
                    new JiraClient(server.endpoint(), "e@x.com", "token"));

            ProviderResolution resolution = provider.fetch(new ExternalReference("DEBT2TEST-1", "comment"));

            assertNull(resolution.error());
            assertFalse(resolution.isNotFound());
            ExternalTaskSpec task = resolution.task();
            assertEquals("jira", task.provider());
            assertEquals("DEBT2TEST-1", task.key());
            assertEquals("Fix login authentication bug", task.summary());
            assertEquals("Users cannot login with SSO", task.description());
            assertEquals(List.of(), task.acceptanceCriteria());
            assertEquals(List.of(), task.labels());
            assertEquals(server.endpoint() + "/browse/DEBT2TEST-1", task.url());
        } finally {
            server.close();
        }
    }

    @Test
    void fetchReturnsNotFoundWhenIssueMissing() throws Exception {
        FakeJiraServer server = new FakeJiraServer(404, "{}");
        try {
            JiraContextProvider provider = new JiraContextProvider(
                    new JiraClient(server.endpoint(), "e@x.com", "token"));

            ProviderResolution resolution = provider.fetch(new ExternalReference("OPS-404", "comment"));

            assertTrue(resolution.isNotFound());
            assertNull(resolution.task());
            assertNull(resolution.error());
        } finally {
            server.close();
        }
    }

    @Test
    void fetchReturnsFailedWithUnauthorizedCode() throws Exception {
        FakeJiraServer server = new FakeJiraServer(401, "{}");
        try {
            JiraContextProvider provider = new JiraContextProvider(
                    new JiraClient(server.endpoint(), "e@x.com", "token"));

            ProviderResolution resolution = provider.fetch(new ExternalReference("OPS-1", "comment"));

            assertFalse(resolution.isNotFound());
            assertNull(resolution.task());
            assertEquals("UNAUTHORIZED", resolution.error().code());
            assertFalse(resolution.error().recoverable());
        } finally {
            server.close();
        }
    }

    @Test
    void fetchReturnsFailedWithRateLimitedCode() throws Exception {
        FakeJiraServer server = new FakeJiraServer(429, "{}");
        try {
            JiraContextProvider provider = new JiraContextProvider(
                    new JiraClient(server.endpoint(), "e@x.com", "token"));

            ProviderResolution resolution = provider.fetch(new ExternalReference("OPS-1", "comment"));

            assertEquals("RATE_LIMITED", resolution.error().code());
            assertTrue(resolution.error().recoverable());
        } finally {
            server.close();
        }
    }

    private JiraContextProvider provider() {
        return new JiraContextProvider(new JiraClient("http://localhost", "e@x.com", "token"));
    }

    private static final class FakeJiraServer implements AutoCloseable {
        private final HttpServer server;
        private final int status;
        private final String response;

        private FakeJiraServer(int status, String response) throws IOException {
            this.status = status;
            this.response = response;
            this.server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
            this.server.createContext("/", this::handle);
            this.server.start();
        }

        private void handle(com.sun.net.httpserver.HttpExchange exchange) throws IOException {
            byte[] body = response.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(status, body.length);
            try (OutputStream output = exchange.getResponseBody()) {
                output.write(body);
            }
        }

        private String endpoint() {
            return "http://localhost:" + server.getAddress().getPort();
        }

        @Override
        public void close() {
            server.stop(0);
        }
    }
}
