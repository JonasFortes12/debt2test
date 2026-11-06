package io.github.jonasfortes12.api.dto;

import java.util.List;

import io.github.jonasfortes12.core.model.ContextStatus;
import io.github.jonasfortes12.core.model.ItemStatus;
import io.github.jonasfortes12.core.model.RunReportItem;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "One candidate comment and how far it got through the pipeline. Fields of a "
                + "stage the item never reached are null.")
public record RunReportItemResponse(
                @Schema(description = "Identifier of the candidate within this run: "
                                + "<runId>:<path>:<line>:<column>:<method>:<class>:<signature>.") String candidateId,

                @Schema(description = "Source file, relative to the repository root.", example = "src/main/java/com/example/OrderService.java") String filePath,

                @Schema(description = "Method the comment belongs to.", example = "save") String methodName,

                @Schema(description = "1-based line where the method declaration begins.", example = "42") int lineNumber,

                @Schema(description = "The extracted comment text.", example = "// TODO: validate the order total") String comment,

                @Schema(description = "Whether the binary classifier judged the comment SATD. false with debtType "
                                + "UNCLASSIFIED when classification was unavailable for the item; null when the item was "
                                + "never classified.", nullable = true) Boolean satd,

                @Schema(description = "Debt category from the multi-class model; NONE when the comment is not "
                                + "SATD; UNCLASSIFIED when classification was unavailable for the item.", nullable = true, allowableValues = {
                                                "DESIGN", "TEST", "DOCUMENTATION", "IMPLEMENTATION", "DEFECT", "NONE",
                                                "UNCLASSIFIED" }) String debtType,

                @Schema(description = "Classifier confidence in [0, 1]; null when unavailable.", nullable = true, minimum = "0", maximum = "1", example = "0.91") Double confidence,

                @Schema(description = "MATCHED: a linked issue was found (task fields are set). NOT_FOUND: no "
                                + "issue found. FAILED: the lookup failed (see errors). "
                                + "SKIPPED: the run had no context source. Null when the item never reached the "
                                + "context stage.", nullable = true) ContextStatus contextStatus,

                @Schema(description = "Key of the linked issue; set only when contextStatus is MATCHED.", nullable = true, example = "DEBT-12") String taskKey,

                @Schema(description = "Title of the linked issue; set only when contextStatus is MATCHED.", nullable = true) String taskSummary,

                @Schema(description = "URL of the linked issue; set only when contextStatus is MATCHED.", nullable = true) String taskUrl,

                @Schema(description = "Generated test source code; null when generation did not run or failed.", nullable = true) String generatedTestSourceCode,

                @Schema(description = "LLM provider used (openai, anthropic, gemini). \"mock\" means the server "
                                + "has no API key and the code is a placeholder stub.", nullable = true, example = "openai") String testProvider,

                @Schema(description = "LLM model used; \"none\" for mock tests.", nullable = true, example = "gpt-4o-mini") String testModel,

                @Schema(description = "Generation outcome. Only GENERATED and GENERATION_FAILED occur here; null "
                                + "when generation did not run for the item.", nullable = true) ItemStatus testStatus,

                @Schema(description = "Structured errors recorded for this item.") List<PipelineErrorResponse> errors,

                @Schema(description = "Deprecated: the message of each entry in "
                                + "errors. Use errors instead.", deprecated = true) @Deprecated List<String> errorMessages) {

        public static RunReportItemResponse from(RunReportItem item) {
                return new RunReportItemResponse(
                                item.candidateId(), item.filePath(), item.methodName(), item.lineNumber(),
                                item.comment(),
                                item.satd(), item.debtType(), item.confidence(), item.contextStatus(), item.taskKey(),
                                item.taskSummary(), item.taskUrl(), item.generatedTestSourceCode(), item.testProvider(),
                                item.testModel(), item.testStatus(),
                                item.errors().stream().map(PipelineErrorResponse::from).toList(), item.errorMessages());
        }
}
