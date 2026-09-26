package io.github.jonasfortes12.core.model;

import java.util.List;
import java.util.Objects;

/** One candidate's row in a {@link RunReport}. Nullable fields reflect stages not yet reached. */
public record RunReportItem(
        String candidateId,
        String filePath,
        String methodName,
        int lineNumber,
        String comment,
        Boolean satd,
        String debtType,
        Double confidence,
        String contextStatus,
        String taskKey,
        String taskSummary,
        String taskUrl,
        String generatedTestSourceCode,
        String testProvider,
        String testModel,
        String testStatus,
        List<String> errorMessages) {

    public RunReportItem {
        errorMessages = List.copyOf(Objects.requireNonNull(errorMessages, "errorMessages must not be null"));
    }
}
