package io.github.jonasfortes12.tester;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class OpenAiProvider implements LlmProvider {
    private final LlmConfig config;
    private final HttpClient httpClient;

    public OpenAiProvider(LlmConfig config) {
        this.config = config;
        this.httpClient = LlmProviderSupport.newHttpClient();
    }

    @Override
    public String generateTest(TestPrompt prompt) throws Exception {
        String url = config.getEndpoint().isEmpty() ? "https://api.openai.com/v1/chat/completions"
                : config.getEndpoint();

        JsonObject payload = new JsonObject();
        payload.addProperty("model", config.getModel());

        JsonArray messages = new JsonArray();
        JsonObject sysMsg = new JsonObject();
        sysMsg.addProperty("role", "system");
        sysMsg.addProperty("content", LlmProviderSupport.systemInstruction(prompt));
        messages.add(sysMsg);

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
                    .header("Authorization", "Bearer " + config.getApiKey())
                    .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                    .build();
        } catch (IllegalArgumentException invalidRequest) {
            throw LlmProviderSupport.invalidRequest();
        }

        HttpResponse<String> response = LlmProviderSupport.send(httpClient, request);
        LlmProviderSupport.requireSuccessful(response);

        try {
            JsonObject jsonResp = LlmProviderSupport.parseResponseObject(response.body());
            String content = jsonResp.getAsJsonArray("choices")
                    .get(0).getAsJsonObject()
                    .getAsJsonObject("message")
                    .get("content").getAsString();

            return LlmProviderSupport.extractCodeBlock(content);
        } catch (LlmProviderException failure) {
            throw failure;
        } catch (RuntimeException malformedResponse) {
            throw LlmProviderSupport.malformedResponse();
        }
    }

}
