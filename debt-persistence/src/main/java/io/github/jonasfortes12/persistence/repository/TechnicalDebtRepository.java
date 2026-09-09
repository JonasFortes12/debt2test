package io.github.jonasfortes12.persistence.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import io.github.jonasfortes12.persistence.entity.TechnicalDebtEntity;

public interface TechnicalDebtRepository extends JpaRepository<TechnicalDebtEntity, Long> {

    // Underscore form is explicit about traversing run.id rather than a runId property.
    Page<TechnicalDebtEntity> findByRun_Id(UUID runId, Pageable pageable);

    Page<TechnicalDebtEntity> findByRun_IdAndSatd(UUID runId, boolean satd, Pageable pageable);

    Page<TechnicalDebtEntity> findByRun_IdAndDebtType(UUID runId, String debtType, Pageable pageable);

    Optional<TechnicalDebtEntity> findByRun_IdAndCandidateId(UUID runId, String candidateId);

    long countByRun_IdAndSatd(UUID runId, boolean satd);

    @Query("select d.candidateId as candidateId, d.id as id "
         + "from TechnicalDebtEntity d where d.run.id = :runId")
    List<CandidateIdProjection> findIdIndexByRunId(@Param("runId") UUID runId);

    @Query("select d.debtType as debtType, count(d) as total "
         + "from TechnicalDebtEntity d where d.run.id = :runId and d.satd = true "
         + "group by d.debtType")
    List<DebtTypeCount> countByDebtType(@Param("runId") UUID runId);
}
