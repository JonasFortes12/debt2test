package io.github.jonasfortes12.orchestrator.reporting;

import java.util.List;

/** External report projection for generated tests and their SATD inputs. */
public record TestReportItem(
        String filePath,
        String methodName,
        int lineNumber,
        String comment,
        String methodSourceCode,
        boolean isSatd,
        String debtType,
        Double confidence,
        String generatedTestCode,
        String status,
        String candidateId,
        String contextStatus,
        List<DebtReportItem.IssueReference> issueReferences,
        DebtReportItem.ExternalTask externalTask,
        DebtReportItem.ClassificationProvenance classificationProvenance,
        List<DebtReportItem.ReportError> errors,
        String provider,
        String model,
        String promptVersion,
        String validationStatus,
        String runId,
        String repositoryUrl,
        String revision) {
}
