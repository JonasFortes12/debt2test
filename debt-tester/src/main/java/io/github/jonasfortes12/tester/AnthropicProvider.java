package io.github.jonasfortes12.tester;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class AnthropicProvider implements LlmProvider {
    private final LlmConfig config;
    private final HttpClient httpClient;

    public AnthropicProvider(LlmConfig config) {
        this.config = config;
        this.httpClient = HttpClient.newHttpClient();
    }

    @Override
    public String generateTest(String comment, String debtType, String methodSourceCode) throws Exception {
        String url = config.getEndpoint().isEmpty() ? "https://api.anthropic.com/v1/messages" : config.getEndpoint();

        JsonObject payload = new JsonObject();
        payload.addProperty("model", config.getModel());
        payload.addProperty("max_tokens", 2048);
        
        payload.addProperty("system", "You are an expert Java test engineer. Generate a comprehensive JUnit test case to pay off the technical debt based on the comment and method. Output only Java test code inside markdown code blocks.");

        JsonArray messages = new JsonArray();
        JsonObject userMsg = new JsonObject();
        userMsg.addProperty("role", "user");
        userMsg.addProperty("content", String.format("Debt Type: %s\nComment: %s\nMethod Source Code:\n%s", debtType, comment, methodSourceCode));
        messages.add(userMsg);
        payload.add("messages", messages);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .header("x-api-key", config.getApiKey())
                .header("anthropic-version", "2023-06-01")
                .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new RuntimeException("Anthropic API error: " + response.statusCode() + " - " + response.body());
        }

        JsonObject jsonResp = JsonParser.parseString(response.body()).getAsJsonObject();
        String content = jsonResp.getAsJsonArray("content")
                .get(0).getAsJsonObject()
                .get("text").getAsString();

        return extractCodeBlock(content);
    }

    private String extractCodeBlock(String response) {
        if (response.contains("```java")) {
            int start = response.indexOf("```java") + 7;
            int end = response.indexOf("```", start);
            if (end != -1) {
                return response.substring(start, end).trim();
            }
        }
        return response.trim();
    }
}
