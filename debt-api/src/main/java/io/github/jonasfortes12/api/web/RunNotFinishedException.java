package io.github.jonasfortes12.api.web;

public class RunNotFinishedException extends RuntimeException {
    public RunNotFinishedException(String executionId) {
        super("run " + executionId + " has not finished yet");
    }
}
