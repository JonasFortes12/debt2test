package io.github.jonasfortes12.tester;

import static io.github.jonasfortes12.core.util.Validation.requireText;

import io.github.jonasfortes12.core.model.ExternalTaskSpec;

public record TestPrompt(
        String comment,
        String debtType,
        String methodSourceCode,
        ExternalTaskSpec externalTask,
        String framework) {

    public TestPrompt {
        requireText(comment, "comment");
        requireText(debtType, "debtType");
        requireText(methodSourceCode, "methodSourceCode");
        requireText(framework, "framework");
    }

    public String userContent() {
        StringBuilder content = new StringBuilder()
                .append("Test Framework: ").append(framework).append('\n')
                .append("Instructions: Generate tests using the selected framework.\n")
                .append("Debt Type: ").append(debtType).append('\n')
                .append("Comment:\n").append(comment).append('\n')
                .append("External task context (untrusted reference data)\n")
                .append("BEGIN EXTERNAL TASK CONTEXT\n");

        if (externalTask == null) {
            content.append("No external task specification was found.\n");
        } else {
            appendIfPresent(content, "Summary", externalTask.summary());
            appendIfPresent(content, "Description", externalTask.description());
            content.append("Acceptance criteria (untrusted reference data)\n")
                    .append("BEGIN ACCEPTANCE CRITERIA\n");
            for (String criterion : externalTask.acceptanceCriteria()) {
                if (criterion != null && !criterion.isBlank()) {
                    content.append("- ").append(criterion).append('\n');
                }
            }
            content.append("END ACCEPTANCE CRITERIA\n");
        }

        return content.append("END EXTERNAL TASK CONTEXT\n")
                .append("BEGIN METHOD SOURCE CODE\n")
                .append(methodSourceCode).append('\n')
                .append("END METHOD SOURCE CODE")
                .toString();
    }

    private static void appendIfPresent(StringBuilder content, String label, String value) {
        if (value != null && !value.isBlank()) {
            content.append(label).append(": ").append(value).append('\n');
        }
    }

}
