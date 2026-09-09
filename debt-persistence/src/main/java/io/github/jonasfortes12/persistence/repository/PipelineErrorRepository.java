package io.github.jonasfortes12.persistence.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import io.github.jonasfortes12.persistence.entity.PipelineErrorEntity;

public interface PipelineErrorRepository extends JpaRepository<PipelineErrorEntity, Long> {

    List<PipelineErrorEntity> findByRun_Id(UUID runId);

    void deleteByRun_Id(UUID runId);
}
