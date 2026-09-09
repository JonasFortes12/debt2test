package io.github.jonasfortes12.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** An issue reference cited by a debt's context, e.g. a Jira key found in the comment. */
@Entity
@Table(name = "external_reference")
public class ExternalReferenceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "debt_context_id", nullable = false)
    private DebtContextEntity debtContext;

    @Column(name = "reference_value", nullable = false)
    private String referenceValue;

    @Column(name = "source", nullable = false, length = 64)
    private String source;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public DebtContextEntity getDebtContext() { return debtContext; }
    public void setDebtContext(DebtContextEntity debtContext) { this.debtContext = debtContext; }
    public String getReferenceValue() { return referenceValue; }
    public void setReferenceValue(String referenceValue) { this.referenceValue = referenceValue; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
}
