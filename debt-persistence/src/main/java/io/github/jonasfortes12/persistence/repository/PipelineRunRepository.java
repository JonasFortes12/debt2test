package io.github.jonasfortes12.persistence.repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import io.github.jonasfortes12.core.model.RunStatus;
import io.github.jonasfortes12.persistence.entity.PipelineRunEntity;

public interface PipelineRunRepository extends JpaRepository<PipelineRunEntity, UUID> {

    Optional<PipelineRunEntity> findByRunId(String runId);

    Page<PipelineRunEntity> findByStatus(RunStatus status, Pageable pageable);

    Page<PipelineRunEntity> findByStartedAtAfter(Instant threshold, Pageable pageable);

    Page<PipelineRunEntity> findByStatusAndStartedAtAfter(
            RunStatus status, Instant threshold, Pageable pageable);

    Page<PipelineRunEntity> findByRepositoryUrl(String repositoryUrl, Pageable pageable);
}
