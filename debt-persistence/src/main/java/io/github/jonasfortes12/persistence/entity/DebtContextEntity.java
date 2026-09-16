package io.github.jonasfortes12.persistence.entity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import io.github.jonasfortes12.core.model.ContextStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

/** Enrichment outcome for one debt. Task columns are populated only when MATCHED. */
@Entity
@Table(name = "debt_context")
public class DebtContextEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "technical_debt_id", nullable = false, unique = true)
    private TechnicalDebtEntity technicalDebt;

    @Enumerated(EnumType.STRING)
    @Column(name = "context_status", nullable = false, length = 32)
    private ContextStatus contextStatus;

    @Column(name = "provider", length = 64)
    private String provider;

    @Column(name = "task_key", length = 128)
    private String taskKey;

    @Column(name = "summary", columnDefinition = "text")
    private String summary;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Column(name = "url", columnDefinition = "text")
    private String url;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "acceptance_criteria", nullable = false, columnDefinition = "jsonb")
    private List<String> acceptanceCriteria = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "labels", nullable = false, columnDefinition = "jsonb")
    private List<String> labels = new ArrayList<>();

    @Column(name = "enriched_at", nullable = false)
    private Instant enrichedAt;

    @OneToMany(mappedBy = "debtContext", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ExternalReferenceEntity> references = new ArrayList<>();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public TechnicalDebtEntity getTechnicalDebt() { return technicalDebt; }
    public void setTechnicalDebt(TechnicalDebtEntity technicalDebt) { this.technicalDebt = technicalDebt; }
    public ContextStatus getContextStatus() { return contextStatus; }
    public void setContextStatus(ContextStatus contextStatus) { this.contextStatus = contextStatus; }
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public String getTaskKey() { return taskKey; }
    public void setTaskKey(String taskKey) { this.taskKey = taskKey; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public List<String> getAcceptanceCriteria() { return acceptanceCriteria; }
    public void setAcceptanceCriteria(List<String> v) { this.acceptanceCriteria = v; }
    public List<String> getLabels() { return labels; }
    public void setLabels(List<String> labels) { this.labels = labels; }
    public Instant getEnrichedAt() { return enrichedAt; }
    public void setEnrichedAt(Instant enrichedAt) { this.enrichedAt = enrichedAt; }
    public List<ExternalReferenceEntity> getReferences() { return references; }
    public void setReferences(List<ExternalReferenceEntity> references) { this.references = references; }
}
