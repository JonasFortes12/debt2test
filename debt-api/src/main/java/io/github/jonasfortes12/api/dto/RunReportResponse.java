package io.github.jonasfortes12.api.dto;

import java.util.List;

import io.github.jonasfortes12.core.model.RunReport;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "A terminal run's summary, its items, and the errors that belong to no item.")
public record RunReportResponse(
                RunStatusResponse summary,

                @Schema(description = "One item per extracted candidate comment. Empty "
                                + "when the run failed before extraction.") List<RunReportItemResponse> items,

                @Schema(description = "Errors not attached to any item: failures of the run "
                                + "as a whole (candidateId null, e.g. a repository that could not be cloned; this is why a "
                                + "run is FAILED), and errors naming a candidate that has no item in this report.") List<PipelineErrorResponse> errors) {

        public static RunReportResponse from(RunReport report) {
                return new RunReportResponse(
                                RunStatusResponse.from(report.summary()),
                                report.items().stream().map(RunReportItemResponse::from).toList(),
                                report.errors().stream().map(PipelineErrorResponse::from).toList());
        }
}
