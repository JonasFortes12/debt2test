package io.github.jonasfortes12.persistence.repository;

/** Candidate ID to primary key, loaded once per stage hook to drive idempotent upserts. */
public interface CandidateIdProjection {
    String getCandidateId();

    Long getId();
}
