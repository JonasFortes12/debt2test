package io.github.jonasfortes12.api.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateRunRequest(
        @NotBlank String repositoryUrl,
        String revision,
        boolean contextEnabled,
        boolean allowHeuristicFallback,
        @NotBlank String testFramework) {

    public CreateRunRequest {
        if (testFramework == null) {
            testFramework = "JUnit 5";
        }
    }
}
