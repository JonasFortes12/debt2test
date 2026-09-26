package io.github.jonasfortes12.api.dto;

import java.util.List;

import io.github.jonasfortes12.core.model.RunReportItem;

public record RunReportItemResponse(
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

    public static RunReportItemResponse from(RunReportItem item) {
        return new RunReportItemResponse(
                item.candidateId(), item.filePath(), item.methodName(), item.lineNumber(), item.comment(),
                item.satd(), item.debtType(), item.confidence(), item.contextStatus(), item.taskKey(),
                item.taskSummary(), item.taskUrl(), item.generatedTestSourceCode(), item.testProvider(),
                item.testModel(), item.testStatus(), item.errorMessages());
    }
}
