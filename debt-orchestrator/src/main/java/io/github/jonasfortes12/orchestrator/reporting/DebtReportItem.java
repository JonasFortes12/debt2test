package io.github.jonasfortes12.orchestrator.reporting;

import java.util.List;

/** External report projection for a classified SATD item. */
public record DebtReportItem(
        String filePath,
        String methodName,
        int lineNumber,
        String comment,
        String methodSourceCode,
        boolean isSatd,
        String debtType,
        Double confidence,
        String candidateId,
        String contextStatus,
        List<IssueReference> issueReferences,
        ExternalTask externalTask,
        ClassificationProvenance classificationProvenance,
        List<ReportError> errors,
        String runId,
        String repositoryUrl,
        String revision) {

    public record IssueReference(String value, String source) {
    }

    public record ExternalTask(
            String provider,
            String key,
            String summary,
            String description,
            List<String> acceptanceCriteria,
            List<String> labels,
            String url) {
    }

    public record ClassificationProvenance(String provider, String strategy, String version) {
    }

    /** Deliberately uses a generic message instead of provider error details. */
    public record ReportError(
            String stage, String code, String candidateId, boolean recoverable, String message) {
    }
}
