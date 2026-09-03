package io.github.jonasfortes12.core.model;

import static io.github.jonasfortes12.core.util.Validation.requireText;

public record ExternalReference(String value, String source) {

    public ExternalReference {
        requireText(value, "value");
        requireText(source, "source");
    }

}
