package io.github.jonasfortes12.api.config;

import java.util.List;
import java.util.Map;

import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.github.jonasfortes12.api.access.web.AccessSecurity;
import io.github.jonasfortes12.api.access.web.StudyKey;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.security.SecurityScheme;

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
        static final String API_VERSION = "3.0.0";

        private static final String USAGE_GUIDE = """
                        Runs the Debt2Test pipeline on a Java repository: clones it, finds self-admitted technical \
                        debt (SATD) comments, classifies them with the DebtHunter models, optionally looks up \
                        issue-tracker context, and generates candidate tests with an LLM.

                        ### Access codes
                        Every `/api/pipeline/**` call needs a single-run access code, sent as \
                        `Authorization: Bearer <code>` (the **Authorize** button above). To get one: read \
                        `GET /api/access/terms`, then `POST /api/access/requests` with name, email and the accepted \
                        terms version; the code arrives by email and is valid for 72 hours. One code starts exactly \
                        one run; after that it reads, polls and cancels only that run, for 30 days. Each email can \
                        start a limited number of runs. `GET /api/access/code` tells what a code currently allows.

                        * `401`: no code, an unknown code, or an expired one.
                        * `403`: the code has already started its run.
                        * `404`: the run does not exist or was not started by this code.

                        ### Study service key
                        For research scripts only. When the server is configured with a study service key, \
                        sending it as `X-Study-Key` replaces the access code on every `/api/pipeline/**` call: no \
                        code is needed or spent, no participant quota is used, and runs started this way are \
                        recorded as triggered by `study`. The same key reads, polls and cancels any run. When the \
                        server has no key (the default) the header is ignored, and a wrong key is treated as no key.

                        ### How to use it
                        1. `POST /api/pipeline/runs` (with the access code) starts a run and answers `202` with its \
                        `executionId`.\n
                        2. Poll `GET /api/pipeline/runs/{executionId}` every few seconds until `status` is terminal \
                        (`COMPLETED`, `COMPLETED_WITH_ERRORS`, `FAILED` or `CANCELLED`).\n
                        3. `GET /api/pipeline/runs/{executionId}/report` returns the summary and one item per \
                        extracted candidate.\n

                        ### Run lifecycle
                        `QUEUED` → `RUNNING` → `COMPLETED` | `COMPLETED_WITH_ERRORS` | `FAILED`, or `CANCELLED` \
                        from `QUEUED`/`RUNNING`. Runs execute one at a time on a single worker, so a new run stays \
                        `QUEUED` while another is running.

                        ### Context sources
                        To link SATD comments to issues, send a `context` object naming the tracker \
                        (`provider`: `jira`), its `baseUrl`, and, for a private Jira, `email` and `apiToken` \
                        together. A public Jira needs no credentials. The server checks access before accepting \
                        the run: a private Jira without credentials or with rejected ones answers `400`, an \
                        unreachable one `502`. Credentials are used for that run only and never stored or \
                        returned. Without `context`, every item's `contextStatus` is `SKIPPED`.

                        """;

        @Bean
        public OpenAPI debtApiOpenApi() {
                return new OpenAPI()
                                .info(new Info()
                                                .title("Debt2Test API")
                                                .version(API_VERSION)
                                                .description(USAGE_GUIDE))
                                .components(new Components().addSecuritySchemes(AccessSecurity.SCHEME,
                                                new SecurityScheme()
                                                                .type(SecurityScheme.Type.HTTP)
                                                                .scheme("bearer")
                                                                .bearerFormat("D2T access code")
                                                                .description("Single-run access code from the email sent "
                                                                                + "by POST /api/access/requests, e.g. "
                                                                                + "D2T-XXXX-XXXX-XXXX-XXXX-XXXX-XXXX-XXXX."))
                                                .addSecuritySchemes(StudyKey.SCHEME,
                                                new SecurityScheme()
                                                                .type(SecurityScheme.Type.APIKEY)
                                                                .in(SecurityScheme.In.HEADER)
                                                                .name(StudyKey.HEADER)
                                                                .description("Optional service key for research scripts, "
                                                                                + "accepted instead of an access code when the server has "
                                                                                + "DEBT_STUDY_API_KEY set.")));
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

        /**
         * Documents the 401 once for every operation that requires an access code, as for the 500.
         */
        @Bean
        public OpenApiCustomizer accessCodeRequiredResponse() {
                return openApi -> openApi.getPaths().values().forEach(path -> path.readOperations().stream()
                                .filter(operation -> operation.getSecurity() != null && !operation.getSecurity().isEmpty())
                                .forEach(operation -> operation.getResponses().computeIfAbsent("401", status -> new ApiResponse()
                                                .description("No access code (or valid study service key) was sent, or the code is unknown or expired. "
                                                                + "Get a new one from POST /api/access/requests.")
                                                .content(new Content().addMediaType("application/json", new MediaType()
                                                                .schema(new Schema<>().$ref("#/components/schemas/ApiError"))
                                                                .example(Map.of("message", "missing, unknown or expired access code",
                                                                                "fieldErrors", List.of())))))));
        }
}
