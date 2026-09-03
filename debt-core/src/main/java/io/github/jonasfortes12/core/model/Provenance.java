package io.github.jonasfortes12.core.model;

import static io.github.jonasfortes12.core.util.Validation.requireText;

public record Provenance(String provider, String strategy, String version) {

    public Provenance {
        requireText(provider, "provider");
        requireText(strategy, "strategy");
        requireText(version, "version");
    }

}
