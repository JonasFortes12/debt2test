package io.github.jonasfortes12.tester;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class OpenAiProvider implements LlmProvider {
    private final LlmConfig config;
    private final HttpClient httpClient;

    public OpenAiProvider(LlmConfig config) {
        this.config = config;
        this.httpClient = HttpClient.newHttpClient();
    }

    @Override
    public String generateTest(String comment, String debtType, String methodSourceCode) throws Exception {
        String url = config.getEndpoint().isEmpty() ? "https://api.openai.com/v1/chat/completions" : config.getEndpoint();
        
        JsonObject payload = new JsonObject();
        payload.addProperty("model", config.getModel());
        
        JsonArray messages = new JsonArray();
        JsonObject sysMsg = new JsonObject();
        sysMsg.addProperty("role", "system");
        sysMsg.addProperty("content", "You are an expert Java test engineer. Your task is to analyze the provided Java method, self-admitted technical debt comment, and debt type, then generate a comprehensive JUnit test case to pay off the technical debt. Output only Java test code inside markdown code blocks.");
        messages.add(sysMsg);

        JsonObject userMsg = new JsonObject();
        userMsg.addProperty("role", "user");
        userMsg.addProperty("content", String.format("Debt Type: %s\nComment: %s\nMethod Source Code:\n%s", debtType, comment, methodSourceCode));
        messages.add(userMsg);
        
        payload.add("messages", messages);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + config.getApiKey())
                .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new RuntimeException("OpenAI API error: " + response.statusCode() + " - " + response.body());
        }

        JsonObject jsonResp = JsonParser.parseString(response.body()).getAsJsonObject();
        String content = jsonResp.getAsJsonArray("choices")
                .get(0).getAsJsonObject()
                .getAsJsonObject("message")
                .get("content").getAsString();

        return extractCodeBlock(content);
    }

    private String extractCodeBlock(String response) {
        if (response.contains("```java")) {
            int start = response.indexOf("```java") + 7;
            int end = response.indexOf("```", start);
            if (end != -1) {
                return response.substring(start, end).trim();
            }
        } else if (response.contains("```")) {
            int start = response.indexOf("```") + 3;
            int end = response.indexOf("```", start);
            if (end != -1) {
                return response.substring(start, end).trim();
            }
        }
        return response.trim();
    }
}
