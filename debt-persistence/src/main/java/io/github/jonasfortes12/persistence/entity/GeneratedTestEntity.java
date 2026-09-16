package io.github.jonasfortes12.persistence.entity;

import java.time.Instant;

import io.github.jonasfortes12.core.model.ItemStatus;
import io.github.jonasfortes12.core.model.ValidationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

/** The LLM-generated test for one debt, with the provenance needed to reproduce it. */
@Entity
@Table(name = "generated_test")
public class GeneratedTestEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "technical_debt_id", nullable = false, unique = true)
    private TechnicalDebtEntity technicalDebt;

    /** Null only when generation failed. */
    @Column(name = "source_code", columnDefinition = "text")
    private String sourceCode;

    @Column(name = "framework", nullable = false, length = 64)
    private String framework;

    @Column(name = "provider", nullable = false, length = 64)
    private String provider;

    @Column(name = "model", nullable = false, length = 128)
    private String model;

    @Column(name = "prompt_version", nullable = false, length = 64)
    private String promptVersion;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private ItemStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "validation_status", nullable = false, length = 32)
    private ValidationStatus validationStatus;

    @Column(name = "generated_at", nullable = false)
    private Instant generatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public TechnicalDebtEntity getTechnicalDebt() { return technicalDebt; }
    public void setTechnicalDebt(TechnicalDebtEntity technicalDebt) { this.technicalDebt = technicalDebt; }
    public String getSourceCode() { return sourceCode; }
    public void setSourceCode(String sourceCode) { this.sourceCode = sourceCode; }
    public String getFramework() { return framework; }
    public void setFramework(String framework) { this.framework = framework; }
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public String getPromptVersion() { return promptVersion; }
    public void setPromptVersion(String promptVersion) { this.promptVersion = promptVersion; }
    public ItemStatus getStatus() { return status; }
    public void setStatus(ItemStatus status) { this.status = status; }
    public ValidationStatus getValidationStatus() { return validationStatus; }
    public void setValidationStatus(ValidationStatus v) { this.validationStatus = v; }
    public Instant getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(Instant generatedAt) { this.generatedAt = generatedAt; }
}
