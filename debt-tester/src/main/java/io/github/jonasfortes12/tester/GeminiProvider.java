package io.github.jonasfortes12.tester;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class GeminiProvider implements LlmProvider {
    private final LlmConfig config;
    private final HttpClient httpClient;

    public GeminiProvider(LlmConfig config) {
        this.config = config;
        this.httpClient = HttpClient.newHttpClient();
    }

    @Override
    public String generateTest(String comment, String debtType, String methodSourceCode) throws Exception {
        String baseUrl = config.getEndpoint().isEmpty() ? "https://generativelanguage.googleapis.com/v1beta" : config.getEndpoint();
        String url = String.format("%s/models/%s:generateContent?key=%s", baseUrl, config.getModel(), config.getApiKey());

        JsonObject payload = new JsonObject();
        
        // System instruction
        JsonObject systemInstruction = new JsonObject();
        JsonObject sysParts = new JsonObject();
        sysParts.addProperty("text", "You are an expert Java test engineer. Your task is to analyze the provided Java method, self-admitted technical debt comment, and debt type, then generate a comprehensive JUnit test case to pay off the technical debt. Output only Java test code inside markdown code blocks.");
        systemInstruction.add("parts", sysParts);
        payload.add("system_instruction", systemInstruction);

        // Contents
        JsonArray contents = new JsonArray();
        JsonObject userContent = new JsonObject();
        userContent.addProperty("role", "user");
        
        JsonArray parts = new JsonArray();
        JsonObject part = new JsonObject();
        part.addProperty("text", String.format("Debt Type: %s\nComment: %s\nMethod Source Code:\n%s", debtType, comment, methodSourceCode));
        parts.add(part);
        
        userContent.add("parts", parts);
        contents.add(userContent);
        payload.add("contents", contents);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new RuntimeException("Gemini API error: " + response.statusCode() + " - " + response.body());
        }

        JsonObject jsonResp = JsonParser.parseString(response.body()).getAsJsonObject();
        String content = jsonResp.getAsJsonArray("candidates")
                .get(0).getAsJsonObject()
                .getAsJsonObject("content")
                .getAsJsonArray("parts")
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
