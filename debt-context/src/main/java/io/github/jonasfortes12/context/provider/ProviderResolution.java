package io.github.jonasfortes12.context.provider;

import io.github.jonasfortes12.core.model.ExternalTaskSpec;
import io.github.jonasfortes12.core.model.PipelineError;

public record ProviderResolution(ExternalTaskSpec task, PipelineError error, boolean unmatched) {

    public ProviderResolution {
        int outcomes = (task == null ? 0 : 1)
                + (error == null ? 0 : 1)
                + (unmatched ? 1 : 0);
        if (outcomes != 1) {
            throw new IllegalArgumentException("provider resolution must contain exactly one outcome");
        }
    }

    public ProviderResolution(ExternalTaskSpec task, PipelineError error) {
        this(task, error, false);
    }

    public static ProviderResolution matched(ExternalTaskSpec task) {
        return new ProviderResolution(task, null, false);
    }

    public static ProviderResolution notFound() {
        return new ProviderResolution(null, null, true);
    }

    public static ProviderResolution failed(PipelineError error) {
        return new ProviderResolution(null, error, false);
    }

    public boolean isNotFound() {
        return unmatched;
    }
}
