package io.github.jonasfortes12.context.provider;

import io.github.jonasfortes12.core.model.ExternalReference;

public interface ContextProvider {
    String providerId();

    boolean supports(ExternalReference reference);

    ProviderResolution fetch(ExternalReference reference);
}
