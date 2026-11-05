package io.github.jonasfortes12.api;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Guards the rule in CLAUDE.md that the Swagger docs are part of the API:
 * fails, listing every
 * offender, when an operation, parameter, response, or schema property in
 * {@code /v3/api-docs}
 * has no description, or when an error response does not declare the
 * {@code ApiError} body.
 * Never weaken or skip this test to make an API change pass; document the
 * change instead.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@TestPropertySource(properties = {
        "DEBT_PERSISTENCE_ENABLED=true",
        "DEBT_DB_URL=jdbc:postgresql://localhost:1/unreachable-test-db",
        "DEBT_DB_USERNAME=test",
        "DEBT_DB_PASSWORD=test"
})
class OpenApiDocumentationCompletenessTest {

    private static final String API_ERROR_REF = "#/components/schemas/ApiError";

    /**
     * Every operation the API exposes. Adding an endpoint means adding it here and
     * documenting it.
     */
    private static final List<String> EXPECTED_OPERATIONS = List.of(
            "GET /api/pipeline/runs/{executionId}",
            "GET /api/pipeline/runs/{executionId}/report",
            "POST /api/pipeline/runs",
            "POST /api/pipeline/runs/{executionId}/cancel");

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void everyDocumentedElementHasADescription() {
        JsonObject spec = JsonParser.parseString(
                restTemplate.getForEntity("/v3/api-docs", String.class).getBody()).getAsJsonObject();
        JsonObject schemas = spec.getAsJsonObject("components").getAsJsonObject("schemas");
        List<String> offenders = new ArrayList<>();
        assertEquals(EXPECTED_OPERATIONS, documentedOperations(spec),
                "the completeness check must see every endpoint; a vanished path would pass it vacuously");

        for (Map.Entry<String, JsonElement> path : spec.getAsJsonObject("paths").entrySet()) {
            for (Map.Entry<String, JsonElement> operation : path.getValue().getAsJsonObject().entrySet()) {
                checkOperation(path.getKey() + " " + operation.getKey().toUpperCase(),
                        operation.getValue().getAsJsonObject(), offenders);
            }
        }
        for (Map.Entry<String, JsonElement> schema : schemas.entrySet()) {
            checkSchemaProperties(schema.getKey(), schema.getValue().getAsJsonObject(), schemas, offenders);
        }

        assertEquals(List.of(), offenders);
    }

    @Test
    void createAndReadEndpointsCarryExamples() {
        JsonObject paths = JsonParser.parseString(
                restTemplate.getForEntity("/v3/api-docs", String.class).getBody())
                .getAsJsonObject().getAsJsonObject("paths");
        List<String> missing = new ArrayList<>();

        if (!hasExample(paths.getAsJsonObject("/api/pipeline/runs").getAsJsonObject("post")
                .getAsJsonObject("requestBody"))) {
            missing.add("POST /api/pipeline/runs request body");
        }
        for (String path : List.of("/api/pipeline/runs/{executionId}", "/api/pipeline/runs/{executionId}/report")) {
            JsonObject ok = paths.getAsJsonObject(path).getAsJsonObject("get")
                    .getAsJsonObject("responses").getAsJsonObject("200");
            if (!hasExample(ok)) {
                missing.add("GET " + path + " 200 response");
            }
        }

        assertEquals(List.of(), missing);
    }

    /**
     * OpenAPI 3.1 drops {@code nullable}; 3.0 keeps it, so client generators see
     * optional fields.
     */
    @Test
    void nullableFieldsAreMarkedNullable() {
        JsonObject schemas = JsonParser.parseString(
                restTemplate.getForEntity("/v3/api-docs", String.class).getBody())
                .getAsJsonObject().getAsJsonObject("components").getAsJsonObject("schemas");

        JsonObject finishedAt = schemas.getAsJsonObject("RunStatusResponse")
                .getAsJsonObject("properties").getAsJsonObject("finishedAt");

        assertEquals(true, finishedAt.has("nullable") && finishedAt.get("nullable").getAsBoolean());
    }

    private static List<String> documentedOperations(JsonObject spec) {
        List<String> operations = new ArrayList<>();
        for (Map.Entry<String, JsonElement> path : spec.getAsJsonObject("paths").entrySet()) {
            for (String method : path.getValue().getAsJsonObject().keySet()) {
                operations.add(method.toUpperCase() + " " + path.getKey());
            }
        }
        operations.sort(null);
        return operations;
    }

    private static void checkOperation(String name, JsonObject operation, List<String> offenders) {
        requireText(operation, "summary", name, offenders);
        requireText(operation, "description", name, offenders);
        if (operation.has("parameters")) {
            for (JsonElement parameter : operation.getAsJsonArray("parameters")) {
                JsonObject parameterObject = parameter.getAsJsonObject();
                requireText(parameterObject, "description",
                        name + " parameter " + parameterObject.get("name").getAsString(), offenders);
            }
        }
        if (operation.has("requestBody")) {
            requireText(operation.getAsJsonObject("requestBody"), "description", name + " request body", offenders);
        }
        for (Map.Entry<String, JsonElement> response : operation.getAsJsonObject("responses").entrySet()) {
            String responseName = name + " response " + response.getKey();
            JsonObject responseObject = response.getValue().getAsJsonObject();
            requireText(responseObject, "description", responseName, offenders);
            boolean isError = response.getKey().startsWith("4") || response.getKey().startsWith("5");
            if (isError && !declaresApiError(responseObject)) {
                offenders.add(responseName + ": body schema is not ApiError");
            }
        }
    }

    private static void checkSchemaProperties(
            String schemaName, JsonObject schema, JsonObject schemas, List<String> offenders) {
        if (!schema.has("properties")) {
            return;
        }
        for (Map.Entry<String, JsonElement> property : schema.getAsJsonObject("properties").entrySet()) {
            JsonObject propertyObject = property.getValue().getAsJsonObject();
            if (hasText(propertyObject, "description") || referencedSchemaIsDescribed(propertyObject, schemas)) {
                continue;
            }
            offenders.add(schemaName + "." + property.getKey() + ": missing description");
        }
    }

    /**
     * OpenAPI 3.0 ignores siblings of {@code $ref}, so a reference is described by
     * its target.
     */
    private static boolean referencedSchemaIsDescribed(JsonObject property, JsonObject schemas) {
        if (!property.has("$ref")) {
            return false;
        }
        String target = property.get("$ref").getAsString().substring("#/components/schemas/".length());
        return schemas.has(target) && hasText(schemas.getAsJsonObject(target), "description");
    }

    private static boolean declaresApiError(JsonObject response) {
        if (!response.has("content")) {
            return false;
        }
        for (Map.Entry<String, JsonElement> mediaType : response.getAsJsonObject("content").entrySet()) {
            JsonObject schema = mediaType.getValue().getAsJsonObject().getAsJsonObject("schema");
            if (schema == null || !schema.has("$ref") || !API_ERROR_REF.equals(schema.get("$ref").getAsString())) {
                return false;
            }
        }
        return true;
    }

    private static boolean hasExample(JsonObject withContent) {
        if (withContent == null || !withContent.has("content")) {
            return false;
        }
        for (Map.Entry<String, JsonElement> mediaType : withContent.getAsJsonObject("content").entrySet()) {
            JsonObject media = mediaType.getValue().getAsJsonObject();
            if (media.has("example") || media.has("examples")) {
                return true;
            }
        }
        return false;
    }

    private static void requireText(JsonObject object, String field, String name, List<String> offenders) {
        if (!hasText(object, field)) {
            offenders.add(name + ": missing " + field);
        }
    }

    private static boolean hasText(JsonObject object, String field) {
        return object.has(field) && !object.get(field).getAsString().isBlank();
    }
}
