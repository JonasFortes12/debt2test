package io.github.jonasfortes12.core.error;

import io.github.jonasfortes12.core.model.PipelineError;

public final class PipelineException extends RuntimeException {
    private final PipelineError error;

    public PipelineException(PipelineError error) {
        this(error, null);
    }

    public PipelineException(PipelineError error, Throwable cause) {
        super(requireError(error).message(), cause);
        this.error = error;
    }

    public PipelineError error() {
        return error;
    }

    private static PipelineError requireError(PipelineError error) {
        if (error == null) {
            throw new IllegalArgumentException("error is required");
        }
        return error;
    }
}
