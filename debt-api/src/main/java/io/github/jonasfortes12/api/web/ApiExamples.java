package io.github.jonasfortes12.api.web;

/**
 * Example payloads shown in the Swagger docs. Keep them in step with the DTOs
 * they illustrate.
 */
final class ApiExamples {

        static final String EXECUTION_ID = "3f2b8c1e-9a4d-4e7b-8f6a-2c1d5e9b7a40";

        static final String CREATE_RUN = """
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

        static final String CREATE_RUN_PUBLIC_JIRA = """
                        {
                          "repositoryUrl": "https://github.com/JonasFortes12/mock-debt-project",
                          "context": { "provider": "jira", "baseUrl": "https://public-jira.example.org" }
                        }
                        """;

        static final String CREATE_RUN_WITHOUT_CONTEXT = """
                        { "repositoryUrl": "https://github.com/JonasFortes12/mock-debt-project", "revision": "main" }
                        """;

        static final String RUN_ACCEPTED = """
                        { "executionId": "3f2b8c1e-9a4d-4e7b-8f6a-2c1d5e9b7a40" }
                        """;

        static final String RUN_STATUS_RUNNING = """
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

        static final String RUN_REPORT = """
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

        static final String CANCEL_ACCEPTED = """
                        { "executionId": "3f2b8c1e-9a4d-4e7b-8f6a-2c1d5e9b7a40", "outcome": "ACCEPTED" }
                        """;

        static final String INVALID_BODY = """
                        {
                          "message": "invalid request body",
                          "fieldErrors": [ { "field": "repositoryUrl", "message": "must not be blank" } ]
                        }
                        """;

        static final String MALFORMED_BODY = """
                        { "message": "malformed JSON request body", "fieldErrors": [] }
                        """;

        static final String UNSUPPORTED_CONTEXT_PROVIDER = """
                        {
                          "message": "unsupported context provider 'github'; supported: jira",
                          "fieldErrors": [ { "field": "context.provider", "message": "must be one of: jira" } ]
                        }
                        """;

        static final String CONTEXT_AUTH_REQUIRED = """
                        {
                          "message": "Jira at your-company.atlassian.net requires authentication; provide email and apiToken",
                          "fieldErrors": []
                        }
                        """;

        static final String CONTEXT_CREDENTIALS_REJECTED = """
                        { "message": "Jira at your-company.atlassian.net rejected the supplied credentials", "fieldErrors": [] }
                        """;

        static final String CONTEXT_UNREACHABLE = """
                        { "message": "could not reach Jira at your-company.atlassian.net to verify access", "fieldErrors": [] }
                        """;

        static final String PERSISTENCE_UNAVAILABLE = """
                        { "message": "run could not be started: persistence is unavailable", "fieldErrors": [] }
                        """;

        static final String RUN_NOT_FOUND = """
                        { "message": "no run for execution 3f2b8c1e-9a4d-4e7b-8f6a-2c1d5e9b7a40", "fieldErrors": [] }
                        """;

        static final String RUN_NOT_FINISHED = """
                        { "message": "run 3f2b8c1e-9a4d-4e7b-8f6a-2c1d5e9b7a40 has not finished yet", "fieldErrors": [] }
                        """;

        static final String RUN_ALREADY_FINISHED = """
                        { "message": "run 3f2b8c1e-9a4d-4e7b-8f6a-2c1d5e9b7a40 has already finished", "fieldErrors": [] }
                        """;

        private ApiExamples() {
        }
}
