package io.github.jonasfortes12.tester;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class GeminiProvider implements LlmProvider {
    private final LlmConfig config;
    private final HttpClient httpClient;

    public GeminiProvider(LlmConfig config) {
        this.config = config;
        this.httpClient = LlmProviderSupport.newHttpClient();
    }

    @Override
    public String generateTest(TestPrompt prompt) throws Exception {
        String baseUrl = config.getEndpoint().isEmpty()
                ? "https://generativelanguage.googleapis.com/v1beta"
                : config.getEndpoint();
        String url = String.format("%s/models/%s:generateContent", baseUrl, config.getModel());

        JsonObject payload = new JsonObject();

        JsonObject systemInstruction = new JsonObject();
        JsonArray systemParts = new JsonArray();
        JsonObject systemPart = new JsonObject();
        systemPart.addProperty("text", LlmProviderSupport.systemInstruction(prompt));
        systemParts.add(systemPart);
        systemInstruction.add("parts", systemParts);
        payload.add("systemInstruction", systemInstruction);

        JsonArray contents = new JsonArray();
        JsonObject userContent = new JsonObject();
        userContent.addProperty("role", "user");

        JsonArray parts = new JsonArray();
        JsonObject part = new JsonObject();
        part.addProperty("text", prompt.userContent());
        parts.add(part);

        userContent.add("parts", parts);
        contents.add(userContent);
        payload.add("contents", contents);

        HttpRequest request;
        try {
            request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(LlmProviderSupport.REQUEST_TIMEOUT)
                    .header("Content-Type", "application/json")
                    .header("x-goog-api-key", config.getApiKey())
                    .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                    .build();
        } catch (IllegalArgumentException invalidRequest) {
            throw LlmProviderSupport.invalidRequest();
        }

        HttpResponse<String> response = LlmProviderSupport.send(httpClient, request);
        LlmProviderSupport.requireSuccessful(response);

        try {
            JsonObject jsonResp = LlmProviderSupport.parseResponseObject(response.body());
            String content = jsonResp.getAsJsonArray("candidates")
                    .get(0).getAsJsonObject()
                    .getAsJsonObject("content")
                    .getAsJsonArray("parts")
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
