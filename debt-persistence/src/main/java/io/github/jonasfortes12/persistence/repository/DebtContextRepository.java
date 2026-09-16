package io.github.jonasfortes12.persistence.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import io.github.jonasfortes12.persistence.entity.DebtContextEntity;

public interface DebtContextRepository extends JpaRepository<DebtContextEntity, Long> {

    Optional<DebtContextEntity> findByTechnicalDebt_Id(Long technicalDebtId);

    List<DebtContextEntity> findByTechnicalDebt_Run_Id(UUID runId);
}
