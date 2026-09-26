package io.github.jonasfortes12.api.web;

public class RunNotFoundException extends RuntimeException {
    public RunNotFoundException(String executionId) {
        super("no run for execution " + executionId);
    }
}
