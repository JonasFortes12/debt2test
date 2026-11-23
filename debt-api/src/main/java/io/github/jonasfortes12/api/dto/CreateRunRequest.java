package io.github.jonasfortes12.api.dto;

import io.github.jonasfortes12.core.model.TestGenerationOptions;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "What to analyse and how. Unknown fields are ignored.")
public record CreateRunRequest(
                @Schema(description = "Git URL of the repository to clone (public HTTPS is the tested case). "
                                + "Only Java sources are analysed. Credentials embedded in the URL are removed from logs "
                                + "and reports, but sending them is discouraged.", requiredMode = Schema.RequiredMode.REQUIRED, example = "https://github.com/JonasFortes12/mock-debt-project") @NotBlank String repositoryUrl,

                @Schema(description = "Branch, tag or commit SHA to analyse. When omitted or blank, the "
                                + "repository's default branch HEAD is used. An unknown revision does not fail this "
                                + "request: the run ends FAILED with a run-level error.", nullable = true, example = "main") String revision,

                @Schema(description = "Issue tracker to read context from for each SATD item. Omit it (or send "
                                + "null) to skip context: every item's contextStatus is then SKIPPED. Replaces the "
                                + "contextEnabled flag removed in API 2.0.0; a contextEnabled field is now ignored.",
                                nullable = true) @Valid ContextSourceRequest context,

                @Schema(description = "If a DebtHunter Weka model cannot load or predict, classify with a "
                                + "keyword heuristic instead of recording a classification error on the item.", defaultValue = "false") boolean allowHeuristicFallback,

                @Schema(description = "Test framework named in the LLM prompt (\"Generate a comprehensive "
                                + "<framework> test case ...\"). Free text; JUnit 5 is the only tested value. "
                                + "Omitted or blank means JUnit 5.", nullable = true, defaultValue = TestGenerationOptions.DEFAULT_FRAMEWORK, example = TestGenerationOptions.DEFAULT_FRAMEWORK) String testFramework) {

        public CreateRunRequest {
                if (testFramework == null || testFramework.isBlank()) {
                        testFramework = TestGenerationOptions.DEFAULT_FRAMEWORK;
                }
        }
}
