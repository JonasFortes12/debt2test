package io.github.jonasfortes12.api.config;

import java.util.List;
import java.util.Map;

import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;

/**
 * API-level Swagger metadata: the usage guide a front-end developer reads
 * first.
 */
@Configuration
public class OpenApiConfig {

        /**
         * Version of the HTTP contract, not of the Maven build. Bump it on a breaking
         * API change.
         */
        static final String API_VERSION = "1.0.0";

        private static final String USAGE_GUIDE = """
                        Runs the Debt2Test pipeline on a Java repository: clones it, finds self-admitted technical \
                        debt (SATD) comments, classifies them with the DebtHunter models, optionally looks up \
                        issue-tracker context, and generates candidate tests with an LLM.

                        ### How to use it
                        1. `POST /api/pipeline/runs` starts a run and answers `202` with its `executionId`.\n
                        2. Poll `GET /api/pipeline/runs/{executionId}` every few seconds until `status` is terminal \
                        (`COMPLETED`, `COMPLETED_WITH_ERRORS`, `FAILED` or `CANCELLED`).\n
                        3. `GET /api/pipeline/runs/{executionId}/report` returns the summary and one item per \
                        extracted candidate.\n

                        ### Run lifecycle
                        `QUEUED` → `RUNNING` → `COMPLETED` | `COMPLETED_WITH_ERRORS` | `FAILED`, or `CANCELLED` \
                        from `QUEUED`/`RUNNING`. Runs execute one at a time on a single worker, so a new run stays \
                        `QUEUED` while another is running.

                        """;

        @Bean
        public OpenAPI debtApiOpenApi() {
                return new OpenAPI().info(new Info()
                                .title("Debt2Test API")
                                .version(API_VERSION)
                                .description(USAGE_GUIDE));
        }

        /**
         * Documents the generic 500 once, for every operation, instead of repeating it
         * per endpoint.
         */
        @Bean
        public OpenApiCustomizer internalServerErrorResponse() {
                return openApi -> openApi.getPaths().values().forEach(path -> path.readOperations().forEach(
                                operation -> operation.getResponses().addApiResponse("500", new ApiResponse()
                                                .description("Unexpected server failure. The message never reveals the cause; "
                                                                + "it is logged on the server.")
                                                .content(new Content().addMediaType("application/json", new MediaType()
                                                                .schema(new Schema<>()
                                                                                .$ref("#/components/schemas/ApiError"))
                                                                .example(Map.of("message", "internal server error",
                                                                                "fieldErrors", List.of())))))));
        }
}
