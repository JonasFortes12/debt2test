package io.github.jonasfortes12.tester;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

final class LlmProviderSupport {

    // Fixed bounds keep provider calls from hanging indefinitely without adding runtime config surface.
    static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);

    private static final Pattern CODE_FENCE = Pattern.compile(
            "```[ \\t]*(?:[a-z][a-z0-9_+.-]*)?[ \\t]*\\R(.*?)```",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    private LlmProviderSupport() {
    }

    static HttpClient newHttpClient() {
        return HttpClient.newBuilder()
                .connectTimeout(CONNECT_TIMEOUT)
                .build();
    }

    static HttpResponse<String> send(HttpClient client, HttpRequest request) throws LlmProviderException {
        try {
            return client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new LlmProviderException("LLM_REQUEST_INTERRUPTED", false);
        } catch (IOException ioFailure) {
            throw new LlmProviderException("LLM_TRANSPORT_FAILED", true);
        }
    }

    static LlmProviderException invalidRequest() {
        return new LlmProviderException("LLM_REQUEST_INVALID", false);
    }

    static void requireSuccessful(HttpResponse<String> response) throws LlmProviderException {
        int status = response.statusCode();
        if (status != 200) {
            throw httpFailure(status);
        }
    }

    static JsonObject parseResponseObject(String body) throws LlmProviderException {
        try {
            JsonElement parsed = JsonParser.parseString(body);
            if (!parsed.isJsonObject()) {
                throw malformedResponse();
            }
            return parsed.getAsJsonObject();
        } catch (LlmProviderException failure) {
            throw failure;
        } catch (RuntimeException malformed) {
            throw malformedResponse();
        }
    }

    static String extractCodeBlock(String response) throws LlmProviderException {
        if (response == null || response.isBlank()) {
            throw malformedResponse();
        }

        String trimmed = response.trim();
        Matcher fenced = CODE_FENCE.matcher(trimmed);
        if (fenced.find()) {
            String code = fenced.group(1).trim();
            if (code.isBlank()) {
                throw malformedResponse();
            }
            return code;
        }
        if (trimmed.contains("```")) {
            throw malformedResponse();
        }
        return trimmed;
    }

    static String systemInstruction(TestPrompt prompt) {
        return "You are an expert Java test engineer. Generate a comprehensive "
                + prompt.framework()
                + " test case to pay off the technical debt based on the comment and method. "
                + "Output only " + prompt.framework() + " test code inside markdown code blocks.";
    }

    static LlmProviderException malformedResponse() {
        return new LlmProviderException("LLM_RESPONSE_INVALID", false);
    }

    private static LlmProviderException httpFailure(int status) {
        String code;
        if (status == 400) {
            code = "LLM_HTTP_BAD_REQUEST";
        } else if (status == 401) {
            code = "LLM_HTTP_UNAUTHORIZED";
        } else if (status == 403) {
            code = "LLM_HTTP_FORBIDDEN";
        } else if (status == 429) {
            code = "LLM_HTTP_RATE_LIMITED";
        } else if (status >= 500 && status <= 599) {
            code = "LLM_HTTP_SERVER_ERROR";
        } else {
            code = "LLM_HTTP_FAILED";
        }
        boolean recoverable = status == 408 || status == 429 || status >= 500 && status <= 599;
        return new LlmProviderException(code, recoverable);
    }
}
