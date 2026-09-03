package io.github.jonasfortes12.core.model;

import static io.github.jonasfortes12.core.util.Validation.requireText;

import java.util.List;
import java.util.Objects;

public record ExternalTaskSpec(
        String provider,
        String key,
        String summary,
        String description,
        List<String> acceptanceCriteria,
        List<String> labels,
        String url) {

    public ExternalTaskSpec {
        requireText(provider, "provider");
        requireText(key, "key");
        acceptanceCriteria = List.copyOf(Objects.requireNonNull(
                acceptanceCriteria, "acceptanceCriteria must not be null"));
        labels = List.copyOf(Objects.requireNonNull(labels, "labels must not be null"));
    }

}
