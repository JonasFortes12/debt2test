package io.github.jonasfortes12.core.model;

import static io.github.jonasfortes12.core.util.Validation.requireText;

public record TestGenerationOptions(String framework) {

    public TestGenerationOptions {
        requireText(framework, "framework");
    }

}
