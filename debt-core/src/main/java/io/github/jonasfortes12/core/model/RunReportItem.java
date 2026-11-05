package io.github.jonasfortes12.core.model;

import java.util.List;
import java.util.Objects;

/**
 * One candidate's row in a {@link RunReport}. Nullable fields reflect stages
 * not yet reached.
 */
public record RunReportItem(
        String candidateId,
        String filePath,
        String methodName,
        int lineNumber,
        String comment,
        Boolean satd,
        String debtType,
        Double confidence,
        ContextStatus contextStatus,
        String taskKey,
        String taskSummary,
        String taskUrl,
        String generatedTestSourceCode,
        String testProvider,
        String testModel,
        ItemStatus testStatus,
        List<String> errorMessages,
        List<PipelineError> errors) {

    public RunReportItem {
        errorMessages = List.copyOf(Objects.requireNonNull(errorMessages, "errorMessages must not be null"));
        errors = List.copyOf(Objects.requireNonNull(errors, "errors must not be null"));
    }
}
