package io.github.jonasfortes12.api.web;

public class RunAlreadyFinishedException extends RuntimeException {
    public RunAlreadyFinishedException(String executionId) {
        super("run " + executionId + " has already finished");
    }
}
