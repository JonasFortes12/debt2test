package io.github.jonasfortes12.tester;

public interface LlmProvider {
    String generateTest(String comment, String debtType, String methodSourceCode) throws Exception;
}
