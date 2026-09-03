package io.github.jonasfortes12.tester;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class AnthropicProvider implements LlmProvider {
    private final LlmConfig config;
    private final HttpClient httpClient;

    public AnthropicProvider(LlmConfig config) {
        this.config = config;
        this.httpClient = LlmProviderSupport.newHttpClient();
    }

    @Override
    public String generateTest(TestPrompt prompt) throws Exception {
        String url = config.getEndpoint().isEmpty()
                ? "https://api.anthropic.com/v1/messages"
                : config.getEndpoint();

        JsonObject payload = new JsonObject();
        payload.addProperty("model", config.getModel());
        payload.addProperty("max_tokens", 2048);
        payload.addProperty("system", LlmProviderSupport.systemInstruction(prompt));

        JsonArray messages = new JsonArray();
        JsonObject userMsg = new JsonObject();
        userMsg.addProperty("role", "user");
        userMsg.addProperty("content", prompt.userContent());
        messages.add(userMsg);
        payload.add("messages", messages);

        HttpRequest request;
        try {
            request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(LlmProviderSupport.REQUEST_TIMEOUT)
                    .header("Content-Type", "application/json")
                    .header("x-api-key", config.getApiKey())
                    .header("anthropic-version", "2023-06-01")
                    .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                    .build();
        } catch (IllegalArgumentException invalidRequest) {
            throw LlmProviderSupport.invalidRequest();
        }

        HttpResponse<String> response = LlmProviderSupport.send(httpClient, request);
        LlmProviderSupport.requireSuccessful(response);

        try {
            JsonObject jsonResp = LlmProviderSupport.parseResponseObject(response.body());
            String content = jsonResp.getAsJsonArray("content")
                    .get(0).getAsJsonObject()
                    .get("text").getAsString();

            return LlmProviderSupport.extractCodeBlock(content);
        } catch (LlmProviderException failure) {
            throw failure;
        } catch (RuntimeException malformedResponse) {
            throw LlmProviderSupport.malformedResponse();
        }
    }
}
