package io.github.jonasfortes12.core.model;

import static io.github.jonasfortes12.core.util.Validation.requireText;

public record TestGenerationOptions(String framework) {

    /**
     * The framework used when a caller names none; the only one the prompts are
     * tested with.
     */
    public static final String DEFAULT_FRAMEWORK = "JUnit 5";

    public TestGenerationOptions {
        requireText(framework, "framework");
    }

}
