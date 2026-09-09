package io.github.jonasfortes12.persistence.entity;

import java.time.Instant;

import io.github.jonasfortes12.core.model.ItemStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/** One extracted SATD candidate, stored whether or not it was classified as SATD. */
@Entity
@Table(
    name = "technical_debt",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_technical_debt_candidate",
        columnNames = {"run_id", "candidate_id"}))
public class TechnicalDebtEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "run_id", nullable = false)
    private PipelineRunEntity run;

    @Column(name = "candidate_id", nullable = false)
    private String candidateId;

    @Column(name = "file_path", nullable = false, columnDefinition = "text")
    private String filePath;

    @Column(name = "method_name", nullable = false, columnDefinition = "text")
    private String methodName;

    @Column(name = "line_number", nullable = false)
    private int lineNumber;

    @Column(name = "comment", nullable = false, columnDefinition = "text")
    private String comment;

    @Column(name = "method_source_code", nullable = false, columnDefinition = "text")
    private String methodSourceCode;

    @Column(name = "source_repository_url", nullable = false, columnDefinition = "text")
    private String sourceRepositoryUrl;

    @Column(name = "source_revision")
    private String sourceRevision;

    @Column(name = "source_relative_file_path", nullable = false, columnDefinition = "text")
    private String sourceRelativeFilePath;

    /** Null until the classification hook runs. */
    @Column(name = "satd")
    private Boolean satd;

    @Column(name = "debt_type", length = 128)
    private String debtType;

    @Column(name = "confidence")
    private Double confidence;

    @Column(name = "classifier_provider", length = 64)
    private String classifierProvider;

    @Column(name = "classifier_strategy", length = 64)
    private String classifierStrategy;

    @Column(name = "classifier_version", length = 64)
    private String classifierVersion;

    @Enumerated(EnumType.STRING)
    @Column(name = "item_status", length = 32)
    private ItemStatus itemStatus;

    @Column(name = "extracted_at", nullable = false)
    private Instant extractedAt;

    @Column(name = "classified_at")
    private Instant classifiedAt;

    @OneToOne(fetch = FetchType.LAZY, mappedBy = "technicalDebt")
    private DebtContextEntity context;

    @OneToOne(fetch = FetchType.LAZY, mappedBy = "technicalDebt")
    private GeneratedTestEntity generatedTest;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public PipelineRunEntity getRun() { return run; }
    public void setRun(PipelineRunEntity run) { this.run = run; }
    public String getCandidateId() { return candidateId; }
    public void setCandidateId(String candidateId) { this.candidateId = candidateId; }
    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
    public String getMethodName() { return methodName; }
    public void setMethodName(String methodName) { this.methodName = methodName; }
    public int getLineNumber() { return lineNumber; }
    public void setLineNumber(int lineNumber) { this.lineNumber = lineNumber; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
    public String getMethodSourceCode() { return methodSourceCode; }
    public void setMethodSourceCode(String methodSourceCode) { this.methodSourceCode = methodSourceCode; }
    public String getSourceRepositoryUrl() { return sourceRepositoryUrl; }
    public void setSourceRepositoryUrl(String v) { this.sourceRepositoryUrl = v; }
    public String getSourceRevision() { return sourceRevision; }
    public void setSourceRevision(String sourceRevision) { this.sourceRevision = sourceRevision; }
    public String getSourceRelativeFilePath() { return sourceRelativeFilePath; }
    public void setSourceRelativeFilePath(String v) { this.sourceRelativeFilePath = v; }
    public Boolean getSatd() { return satd; }
    public void setSatd(Boolean satd) { this.satd = satd; }
    public String getDebtType() { return debtType; }
    public void setDebtType(String debtType) { this.debtType = debtType; }
    public Double getConfidence() { return confidence; }
    public void setConfidence(Double confidence) { this.confidence = confidence; }
    public String getClassifierProvider() { return classifierProvider; }
    public void setClassifierProvider(String v) { this.classifierProvider = v; }
    public String getClassifierStrategy() { return classifierStrategy; }
    public void setClassifierStrategy(String v) { this.classifierStrategy = v; }
    public String getClassifierVersion() { return classifierVersion; }
    public void setClassifierVersion(String v) { this.classifierVersion = v; }
    public ItemStatus getItemStatus() { return itemStatus; }
    public void setItemStatus(ItemStatus itemStatus) { this.itemStatus = itemStatus; }
    public Instant getExtractedAt() { return extractedAt; }
    public void setExtractedAt(Instant extractedAt) { this.extractedAt = extractedAt; }
    public Instant getClassifiedAt() { return classifiedAt; }
    public void setClassifiedAt(Instant classifiedAt) { this.classifiedAt = classifiedAt; }
    public DebtContextEntity getContext() { return context; }
    public void setContext(DebtContextEntity context) { this.context = context; }
    public GeneratedTestEntity getGeneratedTest() { return generatedTest; }
    public void setGeneratedTest(GeneratedTestEntity generatedTest) { this.generatedTest = generatedTest; }
}
