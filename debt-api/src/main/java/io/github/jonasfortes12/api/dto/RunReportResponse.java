package io.github.jonasfortes12.api.dto;

import java.util.List;

import io.github.jonasfortes12.core.model.RunReport;

public record RunReportResponse(RunStatusResponse summary, List<RunReportItemResponse> items) {

    public static RunReportResponse from(RunReport report) {
        return new RunReportResponse(
                RunStatusResponse.from(report.summary()),
                report.items().stream().map(RunReportItemResponse::from).toList());
    }
}
