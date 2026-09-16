package io.github.jonasfortes12.persistence.mapper;

import java.util.ArrayList;
import java.util.List;

import io.github.jonasfortes12.core.model.ClassifiedDebt;
import io.github.jonasfortes12.core.model.ContextStatus;
import io.github.jonasfortes12.core.model.EnrichedSatdDebt;
import io.github.jonasfortes12.core.model.ExternalReference;
import io.github.jonasfortes12.core.model.ExternalTaskSpec;
import io.github.jonasfortes12.core.model.GeneratedTest;
import io.github.jonasfortes12.core.model.PipelineError;
import io.github.jonasfortes12.core.model.Provenance;
import io.github.jonasfortes12.core.model.SatdCandidate;
import io.github.jonasfortes12.core.model.SourceProvenance;
import io.github.jonasfortes12.persistence.entity.DebtContextEntity;
import io.github.jonasfortes12.persistence.entity.ExternalReferenceEntity;
import io.github.jonasfortes12.persistence.entity.GeneratedTestEntity;
import io.github.jonasfortes12.persistence.entity.PipelineErrorEntity;
import io.github.jonasfortes12.persistence.entity.TechnicalDebtEntity;

/** Rebuilds domain records from stored entities. Stored URLs are already sanitized. */
public final class DomainMapper {

    private DomainMapper() {
    }

    public static SatdCandidate toCandidate(TechnicalDebtEntity entity) {
        return new SatdCandidate(
                entity.getCandidateId(),
                entity.getFilePath(),
                entity.getMethodName(),
                entity.getLineNumber(),
                entity.getComment(),
                entity.getMethodSourceCode(),
                new SourceProvenance(
                        entity.getSourceRepositoryUrl(),
                        entity.getSourceRevision(),
                        entity.getSourceRelativeFilePath()));
    }

    public static ClassifiedDebt toClassifiedDebt(TechnicalDebtEntity entity, SatdCandidate candidate) {
        return new ClassifiedDebt(
                candidate,
                Boolean.TRUE.equals(entity.getSatd()),
                entity.getDebtType(),
                entity.getConfidence(),
                new Provenance(
                        entity.getClassifierProvider(),
                        entity.getClassifierStrategy(),
                        entity.getClassifierVersion()),
                entity.getItemStatus(),
                List.of());
    }

    public static EnrichedSatdDebt toEnrichedDebt(DebtContextEntity entity, ClassifiedDebt classified) {
        ExternalTaskSpec task = entity.getContextStatus() == ContextStatus.MATCHED
                ? new ExternalTaskSpec(
                        entity.getProvider(),
                        entity.getTaskKey(),
                        entity.getSummary(),
                        entity.getDescription(),
                        List.copyOf(entity.getAcceptanceCriteria()),
                        List.copyOf(entity.getLabels()),
                        entity.getUrl())
                : null;

        List<ExternalReference> references = new ArrayList<>();
        for (ExternalReferenceEntity reference : entity.getReferences()) {
            references.add(new ExternalReference(reference.getReferenceValue(), reference.getSource()));
        }

        return new EnrichedSatdDebt(classified, task, references, entity.getContextStatus(), List.of());
    }

    public static GeneratedTest toGeneratedTest(GeneratedTestEntity entity, String candidateId) {
        return new GeneratedTest(
                candidateId,
                entity.getSourceCode(),
                entity.getFramework(),
                entity.getProvider(),
                entity.getModel(),
                entity.getPromptVersion(),
                entity.getStatus(),
                entity.getValidationStatus(),
                entity.getStatus() == io.github.jonasfortes12.core.model.ItemStatus.GENERATION_FAILED
                        ? List.of(new PipelineError(
                                "generation", "GENERATION_FAILED", "generation failed", candidateId, true))
                        : List.of());
    }

    public static PipelineError toPipelineError(PipelineErrorEntity entity) {
        return new PipelineError(
                entity.getStage(),
                entity.getCode(),
                entity.getMessage(),
                entity.getCandidateId(),
                entity.isRecoverable());
    }
}
