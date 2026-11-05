package io.github.jonasfortes12.persistence.repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import io.github.jonasfortes12.core.model.RunStatus;
import io.github.jonasfortes12.persistence.entity.PipelineRunEntity;
import jakarta.persistence.LockModeType;

public interface PipelineRunRepository extends JpaRepository<PipelineRunEntity, UUID> {

    /**
     * Reads the run with SELECT ... FOR UPDATE, so a status check and the write
     * that follows it
     * cannot interleave with a concurrent cancel or finish of the same run.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select run from PipelineRunEntity run where run.id = :id")
    Optional<PipelineRunEntity> findByIdForUpdate(@Param("id") UUID id);

    /**
     * params_run_id is a fingerprint, not a unique key: repeat runs of an identical
     * configuration
     * share it across rows. This returns the most recently started matching row,
     * which is always
     * the caller's own in-flight run.
     */
    Optional<PipelineRunEntity> findFirstByRunIdOrderByStartedAtDesc(String runId);

    Page<PipelineRunEntity> findByStatus(RunStatus status, Pageable pageable);

    Page<PipelineRunEntity> findByStartedAtAfter(Instant threshold, Pageable pageable);

    Page<PipelineRunEntity> findByStatusAndStartedAtAfter(
            RunStatus status, Instant threshold, Pageable pageable);

    Page<PipelineRunEntity> findByRepositoryUrl(String repositoryUrl, Pageable pageable);
}
