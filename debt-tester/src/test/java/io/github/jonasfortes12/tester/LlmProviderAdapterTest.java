package io.github.jonasfortes12.tester;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import io.github.jonasfortes12.core.model.ExternalTaskSpec;

public class LlmProviderAdapterTest {

    @Test
    void openAiSendsPromptAndNormalizesUppercaseFence() throws Exception {
        CapturingServer server = new CapturingServer(200,
                "{\"choices\":[{\"message\":{\"content\":\"```JAVA\\n@Test void openAi() {}\\n```\"}}]}");
        try {
            LlmProvider provider = new OpenAiProvider(config("openai", "openai-secret", server.endpoint()));

            assertEquals("@Test void openAi() {}", provider.generateTest(prompt()));
            JsonObject request = server.requestJson();
            assertEquals("gpt-test", request.get("model").getAsString());
            JsonArray messages = request.getAsJsonArray("messages");
            assertEquals(2, messages.size());
            assertTrue(messages.get(0).getAsJsonObject().get("content").getAsString().contains("JUnit 5"));
            assertTrue(messages.get(1).getAsJsonObject().get("content").getAsString().contains("Summary: Summary"));
            assertEquals("Bearer openai-secret", server.header("Authorization"));
            assertFalse(server.requestUri().toString().contains("openai-secret"));
        } finally {
            server.close();
        }
    }

    @Test
    void openAiNormalizesUnfencedText() throws Exception {
        assertResponse(new OpenAiProviderFactory(),
                "{\"choices\":[{\"message\":{\"content\":\"  @Test void plain() {}  \"}}]}",
                "@Test void plain() {}");
    }

    @Test
    void anthropicSendsPromptAndNormalizesMixedCaseFence() throws Exception {
        CapturingServer server = new CapturingServer(200,
                "{\"content\":[{\"text\":\"```jAvA\\n@Test void anthropic() {}\\n```\"}]}");
        try {
            LlmProvider provider = new AnthropicProvider(
                    new LlmConfig("anthropic", "anthropic-secret", "claude-test", server.endpoint()));

            assertEquals("@Test void anthropic() {}", provider.generateTest(prompt()));
            JsonObject request = server.requestJson();
            assertEquals("claude-test", request.get("model").getAsString());
            assertEquals(2048, request.get("max_tokens").getAsInt());
            assertTrue(request.get("system").getAsString().contains("JUnit 5"));
            assertTrue(request.getAsJsonArray("messages").get(0).getAsJsonObject()
                    .get("content").getAsString().contains("Description: Description"));
            assertEquals("anthropic-secret", server.header("x-api-key"));
            assertEquals("2023-06-01", server.header("anthropic-version"));
            assertFalse(server.requestUri().toString().contains("anthropic-secret"));
        } finally {
            server.close();
        }
    }

    @Test
    void anthropicNormalizesUnfencedText() throws Exception {
        assertResponse(new AnthropicProviderFactory(),
                "{\"content\":[{\"text\":\"  @Test void plain() {}  \"}]}",
                "@Test void plain() {}");
    }

    @Test
    void geminiUsesCompatibleShapeHeaderAuthAndUnfencedResponse() throws Exception {
        CapturingServer server = new CapturingServer(200,
                "{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"  @Test void gemini() {}  \"}]}}]}");
        try {
            LlmProvider provider = new GeminiProvider(config("gemini", "gemini-secret", server.endpoint()));

            assertEquals("@Test void gemini() {}", provider.generateTest(prompt()));
            URI requestUri = server.requestUri();
            assertFalse(requestUri.toString().contains("gemini-secret"));
            assertNull(requestUri.getRawQuery());
            assertEquals("gemini-secret", server.header("x-goog-api-key"));
            assertEquals("/models/gpt-test:generateContent", requestUri.getPath());
            JsonObject request = server.requestJson();
            assertFalse(request.has("system_instruction"));
            JsonObject systemInstruction = request.getAsJsonObject("systemInstruction");
            assertNotNull(systemInstruction);
            assertTrue(systemInstruction.get("parts").isJsonArray());
            assertTrue(systemInstruction.getAsJsonArray("parts").get(0).getAsJsonObject()
                    .get("text").getAsString().contains("JUnit 5"));
            assertTrue(request.getAsJsonArray("contents").get(0).getAsJsonObject()
                    .get("parts").isJsonArray());
            assertTrue(request.getAsJsonArray("contents").get(0).getAsJsonObject()
                    .getAsJsonArray("parts").get(0).getAsJsonObject()
                    .get("text").getAsString().contains("Acceptance criteria"));
        } finally {
            server.close();
        }
    }

    @Test
    void geminiNormalizesMixedCaseFence() throws Exception {
        assertResponse(new GeminiProviderFactory(),
                "{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"```JaVa\\n@Test void plain() {}\\n```\"}]}}]}",
                "@Test void plain() {}");
    }

    @Test
    void allAdaptersMapAuthenticationClientAndServerFailuresWithoutRawBodies() throws Exception {
        for (ProviderFactory factory : List.of(
                new OpenAiProviderFactory(), new AnthropicProviderFactory(), new GeminiProviderFactory())) {
            for (int status : List.of(400, 401, 403)) {
                LlmProviderException failure = invokeFailure(factory, status, "RAW_PROVIDER_PAYLOAD");
                assertEquals(expectedCode(status), failure.code(), factory.name() + " " + status);
                assertFalse(failure.recoverable(), factory.name() + " " + status);
                assertFalse(failure.getMessage().contains("RAW_PROVIDER_PAYLOAD"));
            }
            for (int status : List.of(429, 500, 503)) {
                LlmProviderException failure = invokeFailure(factory, status, "RAW_PROVIDER_PAYLOAD");
                assertEquals(expectedCode(status), failure.code(), factory.name() + " " + status);
                assertTrue(failure.recoverable(), factory.name() + " " + status);
                assertFalse(failure.getMessage().contains("RAW_PROVIDER_PAYLOAD"));
            }
        }
    }

    @Test
    void malformedResponsesAreTypedNonrecoverableAndSanitized() throws Exception {
        for (ProviderFactory factory : List.of(
                new OpenAiProviderFactory(), new AnthropicProviderFactory(), new GeminiProviderFactory())) {
            LlmProviderException failure = invokeFailure(factory, 200, "RAW_PROVIDER_PAYLOAD");
            assertEquals("LLM_RESPONSE_INVALID", failure.code(), factory.name());
            assertFalse(failure.recoverable(), factory.name());
            assertFalse(failure.getMessage().contains("RAW_PROVIDER_PAYLOAD"));
        }
    }

    @Test
    void invalidUrisAreRequestFailuresNotResponseFailures() throws Exception {
        for (ProviderFactory factory : List.of(
                new OpenAiProviderFactory(), new AnthropicProviderFactory(), new GeminiProviderFactory())) {
            LlmProviderException failure = assertThrows(LlmProviderException.class,
                    () -> factory.create(config(factory.name(), "secret", "://invalid"))
                            .generateTest(prompt()));

            assertEquals("LLM_REQUEST_INVALID", failure.code(), factory.name());
            assertFalse(failure.recoverable(), factory.name());
            assertFalse(failure.getMessage().contains("secret"));
        }
    }

    @Test
    void invalidHeadersAreTypedRequestFailures() throws Exception {
        for (ProviderFactory factory : List.of(
                new OpenAiProviderFactory(), new AnthropicProviderFactory(), new GeminiProviderFactory())) {
            LlmProviderException failure = assertThrows(LlmProviderException.class,
                    () -> factory.create(config(factory.name(), "bad\nkey", "http://localhost"))
                            .generateTest(prompt()));

            assertEquals("LLM_REQUEST_INVALID", failure.code(), factory.name());
            assertFalse(failure.recoverable(), factory.name());
        }
    }

    @Test
    void connectionFailuresAreTypedRecoverableTransportFailures() throws Exception {
        for (ProviderFactory factory : List.of(
                new OpenAiProviderFactory(), new AnthropicProviderFactory(), new GeminiProviderFactory())) {
            CapturingServer server = new CapturingServer(200, "{}");
            String endpoint = server.endpoint();
            server.close();

            LlmProviderException failure = assertThrows(LlmProviderException.class,
                    () -> factory.create(config(factory.name(), "secret", endpoint))
                            .generateTest(prompt()));

            assertEquals("LLM_TRANSPORT_FAILED", failure.code(), factory.name());
            assertTrue(failure.recoverable(), factory.name());
            assertFalse(failure.getMessage().contains("secret"));
        }
    }

    private static String expectedCode(int status) {
        if (status == 400) {
            return "LLM_HTTP_BAD_REQUEST";
        }
        if (status == 401) {
            return "LLM_HTTP_UNAUTHORIZED";
        }
        if (status == 403) {
            return "LLM_HTTP_FORBIDDEN";
        }
        if (status == 429) {
            return "LLM_HTTP_RATE_LIMITED";
        }
        return "LLM_HTTP_SERVER_ERROR";
    }

    private static void assertResponse(
            ProviderFactory factory, String response, String expected) throws Exception {
        CapturingServer server = new CapturingServer(200, response);
        try {
            assertEquals(expected, factory.create(config(factory.name(), "secret", server.endpoint()))
                    .generateTest(prompt()));
        } finally {
            server.close();
        }
    }

    private static LlmProviderException invokeFailure(
            ProviderFactory factory, int status, String response) throws Exception {
        CapturingServer server = new CapturingServer(status, response);
        try {
            Exception failure = assertThrows(Exception.class,
                    () -> factory.create(config(factory.name(), "secret", server.endpoint()))
                            .generateTest(prompt()));
            assertTrue(failure instanceof LlmProviderException, factory.name());
            return (LlmProviderException) failure;
        } finally {
            server.close();
        }
    }

    private static TestPrompt prompt() {
        return new TestPrompt(
                "TODO: simplify",
                "DESIGN_DEBT",
                "void run() {}",
                new ExternalTaskSpec(
                        "jira", "TASK-1", "Summary", "Description",
                        List.of("Acceptance criterion"), List.of("label"), null),
                "JUnit 5");
    }

    private static LlmConfig config(String provider, String key, String endpoint) {
        return new LlmConfig(provider, key, "gpt-test", endpoint);
    }

    private interface ProviderFactory {
        String name();

        LlmProvider create(LlmConfig config);
    }

    private static final class OpenAiProviderFactory implements ProviderFactory {
        @Override
        public String name() {
            return "openai";
        }

        @Override
        public LlmProvider create(LlmConfig config) {
            return new OpenAiProvider(config);
        }
    }

    private static final class AnthropicProviderFactory implements ProviderFactory {
        @Override
        public String name() {
            return "anthropic";
        }

        @Override
        public LlmProvider create(LlmConfig config) {
            return new AnthropicProvider(config);
        }
    }

    private static final class GeminiProviderFactory implements ProviderFactory {
        @Override
        public String name() {
            return "gemini";
        }

        @Override
        public LlmProvider create(LlmConfig config) {
            return new GeminiProvider(config);
        }
    }

    private static final class CapturingServer implements AutoCloseable {
        private final HttpServer server;
        private final int status;
        private final String response;
        private volatile String requestBody;
        private volatile HttpExchange exchange;

        private CapturingServer(int status, String response) throws IOException {
            this.status = status;
            this.response = response;
            this.server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
            this.server.createContext("/", this::handle);
            this.server.start();
        }

        private void handle(HttpExchange exchange) throws IOException {
            this.exchange = exchange;
            this.requestBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            byte[] body = response.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(status, body.length);
            try (OutputStream output = exchange.getResponseBody()) {
                output.write(body);
            }
        }

        private String endpoint() {
            return "http://localhost:" + server.getAddress().getPort();
        }

        private JsonObject requestJson() {
            return JsonParser.parseString(requestBody).getAsJsonObject();
        }

        private String header(String name) {
            return exchange.getRequestHeaders().getFirst(name);
        }

        private URI requestUri() {
            return exchange.getRequestURI();
        }

        @Override
        public void close() {
            server.stop(0);
        }
    }
}
