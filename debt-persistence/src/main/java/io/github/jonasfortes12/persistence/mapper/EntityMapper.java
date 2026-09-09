package io.github.jonasfortes12.persistence.mapper;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import io.github.jonasfortes12.core.model.ClassifiedDebt;
import io.github.jonasfortes12.core.model.EnrichedSatdDebt;
import io.github.jonasfortes12.core.model.ExternalReference;
import io.github.jonasfortes12.core.model.ExternalTaskSpec;
import io.github.jonasfortes12.core.model.GeneratedTest;
import io.github.jonasfortes12.core.model.PipelineError;
import io.github.jonasfortes12.core.model.Provenance;
import io.github.jonasfortes12.core.model.SatdCandidate;
import io.github.jonasfortes12.core.util.UrlSanitizer;
import io.github.jonasfortes12.persistence.entity.DebtContextEntity;
import io.github.jonasfortes12.persistence.entity.ExternalReferenceEntity;
import io.github.jonasfortes12.persistence.entity.GeneratedTestEntity;
import io.github.jonasfortes12.persistence.entity.PipelineErrorEntity;
import io.github.jonasfortes12.persistence.entity.PipelineRunEntity;
import io.github.jonasfortes12.persistence.entity.TechnicalDebtEntity;

/** Maps immutable domain records onto JPA entities. Pure functions: no Spring, no I/O. */
public final class EntityMapper {

    private EntityMapper() {
    }

    public static TechnicalDebtEntity toEntity(SatdCandidate candidate, PipelineRunEntity run) {
        TechnicalDebtEntity entity = new TechnicalDebtEntity();
        entity.setRun(run);
        entity.setCandidateId(candidate.candidateId());
        entity.setFilePath(candidate.filePath());
        entity.setMethodName(candidate.methodName());
        entity.setLineNumber(candidate.lineNumber());
        entity.setComment(candidate.comment());
        entity.setMethodSourceCode(candidate.methodSourceCode());
        entity.setSourceRepositoryUrl(UrlSanitizer.sanitize(candidate.sourceProvenance().repositoryUrl()));
        entity.setSourceRevision(candidate.sourceProvenance().revision());
        entity.setSourceRelativeFilePath(candidate.sourceProvenance().relativeFilePath());
        entity.setExtractedAt(Instant.now());
        return entity;
    }

    public static void applyClassification(TechnicalDebtEntity entity, ClassifiedDebt classified) {
        entity.setSatd(classified.satd());
        entity.setDebtType(classified.debtType());
        entity.setConfidence(classified.confidence());
        Provenance provenance = classified.provenance();
        entity.setClassifierProvider(provenance.provider());
        entity.setClassifierStrategy(provenance.strategy());
        entity.setClassifierVersion(provenance.version());
        entity.setItemStatus(classified.status());
        entity.setClassifiedAt(Instant.now());
    }

    public static DebtContextEntity toContextEntity(
            EnrichedSatdDebt enriched, TechnicalDebtEntity debt) {
        DebtContextEntity entity = new DebtContextEntity();
        entity.setTechnicalDebt(debt);
        entity.setContextStatus(enriched.contextStatus());
        entity.setEnrichedAt(Instant.now());

        ExternalTaskSpec task = enriched.externalTask();
        if (task != null) {
            entity.setProvider(task.provider());
            entity.setTaskKey(task.key());
            entity.setSummary(task.summary());
            entity.setDescription(task.description());
            entity.setUrl(task.url());
            entity.setAcceptanceCriteria(new ArrayList<>(task.acceptanceCriteria()));
            entity.setLabels(new ArrayList<>(task.labels()));
        }

        List<ExternalReferenceEntity> references = new ArrayList<>();
        for (ExternalReference reference : enriched.references()) {
            ExternalReferenceEntity referenceEntity = new ExternalReferenceEntity();
            referenceEntity.setDebtContext(entity);
            referenceEntity.setReferenceValue(reference.value());
            referenceEntity.setSource(reference.source());
            references.add(referenceEntity);
        }
        entity.setReferences(references);
        return entity;
    }

    public static GeneratedTestEntity toTestEntity(GeneratedTest test, TechnicalDebtEntity debt) {
        GeneratedTestEntity entity = new GeneratedTestEntity();
        entity.setTechnicalDebt(debt);
        entity.setSourceCode(test.sourceCode());
        entity.setFramework(test.framework());
        entity.setProvider(test.provider());
        entity.setModel(test.model());
        entity.setPromptVersion(test.promptVersion());
        entity.setStatus(test.status());
        entity.setValidationStatus(test.validationStatus());
        entity.setGeneratedAt(Instant.now());
        return entity;
    }

    public static PipelineErrorEntity toErrorEntity(
            PipelineError error, PipelineRunEntity run, TechnicalDebtEntity debt) {
        PipelineErrorEntity entity = new PipelineErrorEntity();
        entity.setRun(run);
        entity.setTechnicalDebt(debt);
        entity.setStage(error.stage());
        entity.setCode(error.code());
        entity.setMessage(error.message());
        entity.setCandidateId(error.candidateId());
        entity.setRecoverable(error.recoverable());
        entity.setRecordedAt(Instant.now());
        return entity;
    }
}
