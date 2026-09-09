package io.github.jonasfortes12.persistence.adapter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import io.github.jonasfortes12.core.model.ClassifiedDebt;
import io.github.jonasfortes12.core.model.EnrichedSatdDebt;
import io.github.jonasfortes12.core.model.GeneratedTest;
import io.github.jonasfortes12.core.model.ItemStatus;
import io.github.jonasfortes12.core.model.PipelineError;
import io.github.jonasfortes12.core.model.PipelineRequest;
import io.github.jonasfortes12.core.model.PipelineResult;
import io.github.jonasfortes12.core.model.RepositoryWorkspace;
import io.github.jonasfortes12.core.model.RunStatus;
import io.github.jonasfortes12.core.model.SatdCandidate;
import io.github.jonasfortes12.core.port.PipelineRunStore;
import io.github.jonasfortes12.core.util.UrlSanitizer;
import io.github.jonasfortes12.persistence.entity.PipelineErrorEntity;
import io.github.jonasfortes12.persistence.entity.PipelineRunEntity;
import io.github.jonasfortes12.persistence.entity.TechnicalDebtEntity;
import io.github.jonasfortes12.persistence.mapper.EntityMapper;
import io.github.jonasfortes12.persistence.repository.CandidateIdProjection;
import io.github.jonasfortes12.persistence.repository.DebtContextRepository;
import io.github.jonasfortes12.persistence.repository.GeneratedTestRepository;
import io.github.jonasfortes12.persistence.repository.PipelineErrorRepository;
import io.github.jonasfortes12.persistence.repository.PipelineRunRepository;
import io.github.jonasfortes12.persistence.repository.TechnicalDebtRepository;

/**
 * Stores pipeline runs in the database, one transaction per lifecycle event.
 *
 * <p>The pipeline as a whole is not transactional: a run may take minutes, and holding a
 * connection open across LLM calls would leak a resource for no benefit. The cost is that a
 * crashed run leaves its row in RUNNING, which a future reconciliation sweep can pick up.
 *
 * <p>Every hook is idempotent, keyed by the (run_id, candidate_id) unique constraint, so
 * replaying a stage never duplicates rows.
 */
@Component
public class DatabasePipelineRunStore implements PipelineRunStore {

    private final PipelineRunRepository runs;
    private final TechnicalDebtRepository debts;
    private final DebtContextRepository contexts;
    private final GeneratedTestRepository tests;
    private final PipelineErrorRepository errors;

    public DatabasePipelineRunStore(
            PipelineRunRepository runs,
            TechnicalDebtRepository debts,
            DebtContextRepository contexts,
            GeneratedTestRepository tests,
            PipelineErrorRepository errors) {
        this.runs = runs;
        this.debts = debts;
        this.contexts = contexts;
        this.tests = tests;
        this.errors = errors;
    }

    @Override
    @Transactional
    public void runStarted(String executionId, PipelineRequest request) {
        PipelineRunEntity run = new PipelineRunEntity();
        run.setId(UUID.fromString(executionId));
        run.setRepositoryUrl(UrlSanitizer.sanitize(request.repository().repositoryUrl()));
        run.setRequestedRevision(request.repository().revision());
        run.setStatus(RunStatus.RUNNING);
        run.setStartedAt(Instant.now());
        run.setTriggeredBy("cli");
        run.setContextEnabled(request.context().enabled());
        run.setHeuristicFallbackAllowed(request.classification().allowHeuristicFallback());
        run.setTestFramework(request.testGeneration().framework());
        run.setPromptVersion(request.testGeneration().promptVersion());
        runs.save(run);
    }

    @Override
    @Transactional
    public void runIdResolved(String executionId, String runId) {
        runs.findById(UUID.fromString(executionId)).ifPresent(run -> run.setRunId(runId));
    }

    @Override
    @Transactional
    public void workspaceReady(String executionId, RepositoryWorkspace workspace) {
        runs.findById(UUID.fromString(executionId))
                .ifPresent(run -> run.setResolvedRevision(workspace.revision()));
    }

    @Override
    @Transactional
    public void candidatesExtracted(String executionId, List<SatdCandidate> candidates) {
        PipelineRunEntity run = requireRun(executionId);
        Map<String, Long> existing = idIndex(run.getId());
        List<TechnicalDebtEntity> inserts = new ArrayList<>();
        for (SatdCandidate candidate : candidates) {
            if (!existing.containsKey(candidate.candidateId())) {
                inserts.add(EntityMapper.toEntity(candidate, run));
            }
        }
        debts.saveAll(inserts);
    }

    @Override
    @Transactional
    public void candidatesClassified(String executionId, List<ClassifiedDebt> classifications) {
        PipelineRunEntity run = requireRun(executionId);
        Map<String, TechnicalDebtEntity> byCandidate = debtsByCandidateId(run.getId());
        for (ClassifiedDebt classified : classifications) {
            TechnicalDebtEntity debt = byCandidate.get(classified.candidateId());
            if (debt == null) {
                debt = debts.save(EntityMapper.toEntity(classified.candidate(), run));
            }
            EntityMapper.applyClassification(debt, classified);
        }
    }

    @Override
    @Transactional
    public void contextEnriched(String executionId, List<EnrichedSatdDebt> enrichments) {
        PipelineRunEntity run = requireRun(executionId);
        Map<String, TechnicalDebtEntity> byCandidate = debtsByCandidateId(run.getId());
        for (EnrichedSatdDebt enriched : enrichments) {
            TechnicalDebtEntity debt = byCandidate.get(enriched.candidateId());
            if (debt == null) {
                continue;
            }
            contexts.findByTechnicalDebt_Id(debt.getId()).ifPresent(contexts::delete);
            contexts.save(EntityMapper.toContextEntity(enriched, debt));
        }
    }

    @Override
    @Transactional
    public void testsGenerated(String executionId, List<GeneratedTest> generatedTests) {
        PipelineRunEntity run = requireRun(executionId);
        Map<String, TechnicalDebtEntity> byCandidate = debtsByCandidateId(run.getId());
        for (GeneratedTest test : generatedTests) {
            TechnicalDebtEntity debt = byCandidate.get(test.candidateId());
            if (debt == null) {
                continue;
            }
            tests.findByTechnicalDebt_Id(debt.getId()).ifPresent(tests::delete);
            tests.save(EntityMapper.toTestEntity(test, debt));
        }
    }

    @Override
    @Transactional
    public void runFinished(String executionId, PipelineResult result) {
        PipelineRunEntity run = requireRun(executionId);
        run.setStatus(result.status());
        run.setFinishedAt(Instant.now());
        run.setCandidateCount(result.items().size());
        run.setSatdCount((int) result.items().stream()
                .filter(item -> item.classification() != null && item.classification().satd())
                .count());
        run.setGeneratedTestCount((int) result.items().stream()
                .filter(item -> item.generatedTest() != null
                        && item.generatedTest().status() == ItemStatus.GENERATED)
                .count());

        errors.deleteByRun_Id(run.getId());
        Map<String, TechnicalDebtEntity> byCandidate = debtsByCandidateId(run.getId());
        List<PipelineErrorEntity> rows = new ArrayList<>();
        for (PipelineError error : result.errors()) {
            TechnicalDebtEntity debt = error.candidateId() == null
                    ? null
                    : byCandidate.get(error.candidateId());
            rows.add(EntityMapper.toErrorEntity(error, run, debt));
        }
        errors.saveAll(rows);
    }

    private PipelineRunEntity requireRun(String executionId) {
        return runs.findById(UUID.fromString(executionId))
                .orElseThrow(() -> new IllegalStateException("no run for execution " + executionId));
    }

    private Map<String, Long> idIndex(UUID runId) {
        Map<String, Long> index = new LinkedHashMap<>();
        for (CandidateIdProjection projection : debts.findIdIndexByRunId(runId)) {
            index.put(projection.getCandidateId(), projection.getId());
        }
        return index;
    }

    private Map<String, TechnicalDebtEntity> debtsByCandidateId(UUID runId) {
        Map<String, TechnicalDebtEntity> byCandidate = new LinkedHashMap<>();
        for (TechnicalDebtEntity debt : debts.findByRun_Id(runId, Pageable.unpaged()).getContent()) {
            byCandidate.put(debt.getCandidateId(), debt);
        }
        return byCandidate;
    }
}
