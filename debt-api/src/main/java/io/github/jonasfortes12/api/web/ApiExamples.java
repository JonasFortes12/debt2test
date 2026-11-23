package io.github.jonasfortes12.api.web;

/**
 * Example payloads shown in the Swagger docs. Keep them in step with the DTOs
 * they illustrate.
 */
public final class ApiExamples {

        public static final String EXECUTION_ID = "3f2b8c1e-9a4d-4e7b-8f6a-2c1d5e9b7a40";

        public static final String CREATE_RUN = """
                        {
                          "repositoryUrl": "https://github.com/JonasFortes12/mock-debt-project",
                          "revision": "main",
                          "context": {
                            "provider": "jira",
                            "baseUrl": "https://your-company.atlassian.net",
                            "email": "dev@your-company.com",
                            "apiToken": "<api-token>"
                          },
                          "allowHeuristicFallback": true,
                          "testFramework": "JUnit 5"
                        }
                        """;

        public static final String CREATE_RUN_PUBLIC_JIRA = """
                        {
                          "repositoryUrl": "https://github.com/JonasFortes12/mock-debt-project",
                          "context": { "provider": "jira", "baseUrl": "https://public-jira.example.org" }
                        }
                        """;

        public static final String CREATE_RUN_WITHOUT_CONTEXT = """
                        { "repositoryUrl": "https://github.com/JonasFortes12/mock-debt-project", "revision": "main" }
                        """;

        public static final String RUN_ACCEPTED = """
                        { "executionId": "3f2b8c1e-9a4d-4e7b-8f6a-2c1d5e9b7a40" }
                        """;

        public static final String RUN_STATUS_RUNNING = """
                        {
                          "executionId": "3f2b8c1e-9a4d-4e7b-8f6a-2c1d5e9b7a40",
                          "runId": "9c1f0e6b2d7a4c3e8f5b1a0d6e2c7b4a9f3e8d1c0b5a6f7e2d9c4b3a1f0e8d7c",
                          "status": "RUNNING",
                          "startedAt": "2026-11-03T14:00:05Z",
                          "finishedAt": null,
                          "candidateCount": 0,
                          "satdCount": 0,
                          "generatedTestCount": 0,
                          "reportPaths": []
                        }
                        """;

        public static final String RUN_REPORT = """
                        {
                          "summary": {
                            "executionId": "3f2b8c1e-9a4d-4e7b-8f6a-2c1d5e9b7a40",
                            "runId": "9c1f0e6b2d7a4c3e8f5b1a0d6e2c7b4a9f3e8d1c0b5a6f7e2d9c4b3a1f0e8d7c",
                            "status": "COMPLETED_WITH_ERRORS",
                            "startedAt": "2026-11-03T14:00:05Z",
                            "finishedAt": "2026-11-03T14:03:41Z",
                            "candidateCount": 2,
                            "satdCount": 2,
                            "generatedTestCount": 1,
                            "reportPaths": ["output/debt-report.json", "output/debt-test-report.json", "output/debt-test-report.md"]
                          },
                          "items": [
                            {
                              "candidateId": "9c1f0e6b2d7a4c3e8f5b1a0d6e2c7b4a9f3e8d1c0b5a6f7e2d9c4b3a1f0e8d7c:src/main/java/com/example/OrderService.java:42:5:save:OrderService:save(Order)",
                              "filePath": "src/main/java/com/example/OrderService.java",
                              "methodName": "save",
                              "lineNumber": 42,
                              "comment": "// TODO: validate the order total before saving",
                              "satd": true,
                              "debtType": "DESIGN",
                              "confidence": 0.91,
                              "contextStatus": "NOT_FOUND",
                              "taskKey": null,
                              "taskSummary": null,
                              "taskUrl": null,
                              "generatedTestSourceCode": "class OrderServiceTest {\\n    @Test\\n    void saveRejectsNegativeTotals() { }\\n}",
                              "testProvider": "openai",
                              "testModel": "gpt-4o-mini",
                              "testStatus": "GENERATED",
                              "errors": [],
                              "errorMessages": []
                            },
                            {
                              "candidateId": "9c1f0e6b2d7a4c3e8f5b1a0d6e2c7b4a9f3e8d1c0b5a6f7e2d9c4b3a1f0e8d7c:src/main/java/com/example/PriceCalculator.java:17:5:apply:PriceCalculator:apply(Discount)",
                              "filePath": "src/main/java/com/example/PriceCalculator.java",
                              "methodName": "apply",
                              "lineNumber": 17,
                              "comment": "// FIXME: rounding is wrong for negative discounts",
                              "satd": true,
                              "debtType": "DEFECT",
                              "confidence": 0.84,
                              "contextStatus": "NOT_FOUND",
                              "taskKey": null,
                              "taskSummary": null,
                              "taskUrl": null,
                              "generatedTestSourceCode": null,
                              "testProvider": "openai",
                              "testModel": "gpt-4o-mini",
                              "testStatus": "GENERATION_FAILED",
                              "errors": [
                                {
                                  "stage": "generation",
                                  "code": "LLM_HTTP_RATE_LIMITED",
                                  "message": "LLM test generation failed.",
                                  "candidateId": "9c1f0e6b2d7a4c3e8f5b1a0d6e2c7b4a9f3e8d1c0b5a6f7e2d9c4b3a1f0e8d7c:src/main/java/com/example/PriceCalculator.java:17:5:apply:PriceCalculator:apply(Discount)",
                                  "recoverable": true
                                }
                              ],
                              "errorMessages": ["LLM test generation failed."]
                            }
                          ],
                          "errors": []
                        }
                        """;

        public static final String CANCEL_ACCEPTED = """
                        { "executionId": "3f2b8c1e-9a4d-4e7b-8f6a-2c1d5e9b7a40", "outcome": "ACCEPTED" }
                        """;

        public static final String INVALID_BODY = """
                        {
                          "message": "invalid request body",
                          "fieldErrors": [ { "field": "repositoryUrl", "message": "must not be blank" } ]
                        }
                        """;

        public static final String MALFORMED_BODY = """
                        { "message": "malformed JSON request body", "fieldErrors": [] }
                        """;

        public static final String UNSUPPORTED_CONTEXT_PROVIDER = """
                        {
                          "message": "unsupported context provider 'github'; supported: jira",
                          "fieldErrors": [ { "field": "context.provider", "message": "must be one of: jira" } ]
                        }
                        """;

        public static final String CONTEXT_AUTH_REQUIRED = """
                        {
                          "message": "Jira at your-company.atlassian.net requires authentication; provide email and apiToken",
                          "fieldErrors": []
                        }
                        """;

        public static final String CONTEXT_CREDENTIALS_REJECTED = """
                        { "message": "Jira at your-company.atlassian.net rejected the supplied credentials", "fieldErrors": [] }
                        """;

        public static final String CONTEXT_UNREACHABLE = """
                        { "message": "could not reach Jira at your-company.atlassian.net to verify access", "fieldErrors": [] }
                        """;

        public static final String PERSISTENCE_UNAVAILABLE = """
                        { "message": "run could not be started: persistence is unavailable", "fieldErrors": [] }
                        """;

        public static final String RUN_NOT_FOUND = """
                        { "message": "no run for execution 3f2b8c1e-9a4d-4e7b-8f6a-2c1d5e9b7a40", "fieldErrors": [] }
                        """;

        public static final String RUN_NOT_FINISHED = """
                        { "message": "run 3f2b8c1e-9a4d-4e7b-8f6a-2c1d5e9b7a40 has not finished yet", "fieldErrors": [] }
                        """;

        public static final String RUN_ALREADY_FINISHED = """
                        { "message": "run 3f2b8c1e-9a4d-4e7b-8f6a-2c1d5e9b7a40 has already finished", "fieldErrors": [] }
                        """;

        private ApiExamples() {
        }

        public static final String ACCESS_CODE_REQUEST = """
                        { "name": "Ana Souza", "email": "ana@example.com", "acceptedTermsVersion": "v1" }
                        """;

        public static final String ACCESS_REQUEST_ACCEPTED = """
                        { "message": "If the address can receive email, a message has been sent to it." }
                        """;

        public static final String TERMS = """
                        { "version": "v1", "text": "# Debt2Test terms of use (v1)\\n..." }
                        """;

        public static final String ACCESS_CODE_STATUS = """
                        {
                          "state": "USED",
                          "expiresAt": "2026-12-06T12:00:00Z",
                          "executionId": "3f2b8c1e-9a4d-4e7b-8f6a-2c1d5e9b7a40"
                        }
                        """;

        public static final String ACCESS_CODE_REQUIRED = """
                        { "message": "missing, unknown or expired access code", "fieldErrors": [] }
                        """;

        public static final String ACCESS_CODE_USED = """
                        { "message": "access code has already been used for a run", "fieldErrors": [] }
                        """;

        public static final String TERMS_NOT_CURRENT = """
                        {
                          "message": "terms version 'v0' is not current; current is 'v1'",
                          "fieldErrors": [ { "field": "acceptedTermsVersion", "message": "must be 'v1'" } ]
                        }
                        """;

        public static final String DATABASE_UNAVAILABLE = """
                        { "message": "the database is unavailable; try again later", "fieldErrors": [] }
                        """;

        public static final String TOO_MANY_ACCESS_REQUESTS = """
                        { "message": "too many access-code requests from this address; try again later", "fieldErrors": [] }
                        """;
}
