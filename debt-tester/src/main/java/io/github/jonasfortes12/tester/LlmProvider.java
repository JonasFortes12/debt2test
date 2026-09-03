package io.github.jonasfortes12.tester;

public interface LlmProvider {
    String generateTest(TestPrompt prompt) throws Exception;
}
