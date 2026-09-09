package io.github.jonasfortes12.persistence.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import io.github.jonasfortes12.persistence.entity.GeneratedTestEntity;

public interface GeneratedTestRepository extends JpaRepository<GeneratedTestEntity, Long> {

    Optional<GeneratedTestEntity> findByTechnicalDebt_Id(Long technicalDebtId);

    List<GeneratedTestEntity> findByTechnicalDebt_Run_Id(UUID runId);
}
