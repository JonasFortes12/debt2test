-- run_id is a content-derived reproducibility fingerprint (repository + revision + model
-- digests + LLM identity), not a row identity: re-running the same configuration is expected
-- to produce the same run_id across multiple pipeline_run rows. The unique constraint blocked
-- that, so every repeat run of an identical configuration failed to persist. Row identity is
-- the existing UUID primary key; this only relaxes run_id back to a lookup key.

ALTER TABLE pipeline_run DROP CONSTRAINT uq_pipeline_run_run_id;

CREATE INDEX idx_pipeline_run_run_id ON pipeline_run (run_id);
