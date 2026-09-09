package io.github.jonasfortes12.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import io.github.jonasfortes12.persistence.entity.ExternalReferenceEntity;

public interface ExternalReferenceRepository extends JpaRepository<ExternalReferenceEntity, Long> {
}
