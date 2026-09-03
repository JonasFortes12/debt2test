package io.github.jonasfortes12.orchestrator;

public enum ExitCode {
    SUCCESS(0, "Pipeline executed successfully"),
    FATAL_ERROR(1, "Unrecoverable pipeline error"),
    INVALID_ARGUMENTS(2, "Invalid command-line arguments");

    private final int code;
    private final String description;

    ExitCode(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int code() {
        return this.code;
    }

    public String description() {
        return this.description;
    }
}
