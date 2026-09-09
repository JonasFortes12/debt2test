package io.github.jonasfortes12.persistence.entity;

import java.time.Instant;
import java.util.UUID;

import io.github.jonasfortes12.core.model.RunStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** A single pipeline run. Data container only: invariants live in the debt-core records. */
@Entity
@Table(name = "pipeline_run")
public class PipelineRunEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    /**
     * The domain run ID: a content fingerprint of the run's configuration, not a row identity.
     * Repeat runs of an identical configuration share the same value across multiple rows. Null
     * until resolved, which may be after workspace preparation.
     */
    @Column(name = "run_id")
    private String runId;

    @Column(name = "repository_url", nullable = false, columnDefinition = "text")
    private String repositoryUrl;

    @Column(name = "requested_revision")
    private String requestedRevision;

    @Column(name = "resolved_revision")
    private String resolvedRevision;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private RunStatus status;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    @Column(name = "triggered_by")
    private String triggeredBy;

    @Column(name = "context_enabled", nullable = false)
    private boolean contextEnabled;

    @Column(name = "heuristic_fallback_allowed", nullable = false)
    private boolean heuristicFallbackAllowed;

    @Column(name = "test_framework", length = 64)
    private String testFramework;

    @Column(name = "prompt_version", length = 64)
    private String promptVersion;

    @Column(name = "candidate_count", nullable = false)
    private int candidateCount;

    @Column(name = "satd_count", nullable = false)
    private int satdCount;

    @Column(name = "generated_test_count", nullable = false)
    private int generatedTestCount;

    /** Newline-delimited report artifact paths. */
    @Column(name = "report_paths", columnDefinition = "text")
    private String reportPaths;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getRunId() { return runId; }
    public void setRunId(String runId) { this.runId = runId; }
    public String getRepositoryUrl() { return repositoryUrl; }
    public void setRepositoryUrl(String repositoryUrl) { this.repositoryUrl = repositoryUrl; }
    public String getRequestedRevision() { return requestedRevision; }
    public void setRequestedRevision(String requestedRevision) { this.requestedRevision = requestedRevision; }
    public String getResolvedRevision() { return resolvedRevision; }
    public void setResolvedRevision(String resolvedRevision) { this.resolvedRevision = resolvedRevision; }
    public RunStatus getStatus() { return status; }
    public void setStatus(RunStatus status) { this.status = status; }
    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }
    public Instant getFinishedAt() { return finishedAt; }
    public void setFinishedAt(Instant finishedAt) { this.finishedAt = finishedAt; }
    public String getTriggeredBy() { return triggeredBy; }
    public void setTriggeredBy(String triggeredBy) { this.triggeredBy = triggeredBy; }
    public boolean isContextEnabled() { return contextEnabled; }
    public void setContextEnabled(boolean contextEnabled) { this.contextEnabled = contextEnabled; }
    public boolean isHeuristicFallbackAllowed() { return heuristicFallbackAllowed; }
    public void setHeuristicFallbackAllowed(boolean v) { this.heuristicFallbackAllowed = v; }
    public String getTestFramework() { return testFramework; }
    public void setTestFramework(String testFramework) { this.testFramework = testFramework; }
    public String getPromptVersion() { return promptVersion; }
    public void setPromptVersion(String promptVersion) { this.promptVersion = promptVersion; }
    public int getCandidateCount() { return candidateCount; }
    public void setCandidateCount(int candidateCount) { this.candidateCount = candidateCount; }
    public int getSatdCount() { return satdCount; }
    public void setSatdCount(int satdCount) { this.satdCount = satdCount; }
    public int getGeneratedTestCount() { return generatedTestCount; }
    public void setGeneratedTestCount(int generatedTestCount) { this.generatedTestCount = generatedTestCount; }
    public String getReportPaths() { return reportPaths; }
    public void setReportPaths(String reportPaths) { this.reportPaths = reportPaths; }
}
