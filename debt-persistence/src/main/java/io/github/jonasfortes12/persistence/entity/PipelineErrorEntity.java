package io.github.jonasfortes12.persistence.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** A run- or item-level pipeline error, kept queryable rather than buried in a JSON blob. */
@Entity
@Table(name = "pipeline_error")
public class PipelineErrorEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "run_id", nullable = false)
    private PipelineRunEntity run;

    /** Null for run-level errors. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "technical_debt_id")
    private TechnicalDebtEntity technicalDebt;

    @Column(name = "stage", nullable = false, length = 32)
    private String stage;

    @Column(name = "code", nullable = false, length = 128)
    private String code;

    @Column(name = "message", nullable = false, columnDefinition = "text")
    private String message;

    @Column(name = "candidate_id")
    private String candidateId;

    @Column(name = "recoverable", nullable = false)
    private boolean recoverable;

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public PipelineRunEntity getRun() { return run; }
    public void setRun(PipelineRunEntity run) { this.run = run; }
    public TechnicalDebtEntity getTechnicalDebt() { return technicalDebt; }
    public void setTechnicalDebt(TechnicalDebtEntity technicalDebt) { this.technicalDebt = technicalDebt; }
    public String getStage() { return stage; }
    public void setStage(String stage) { this.stage = stage; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getCandidateId() { return candidateId; }
    public void setCandidateId(String candidateId) { this.candidateId = candidateId; }
    public boolean isRecoverable() { return recoverable; }
    public void setRecoverable(boolean recoverable) { this.recoverable = recoverable; }
    public Instant getRecordedAt() { return recordedAt; }
    public void setRecordedAt(Instant recordedAt) { this.recordedAt = recordedAt; }
}
