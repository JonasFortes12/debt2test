-- "run_id" meant two unrelated things: pipeline_run.run_id is a content-derived
-- reproducibility fingerprint (see V2), while pipeline_error.run_id and
-- technical_debt.run_id are foreign keys to pipeline_run.id, the row's UUID primary
-- key. Renaming both removes the accidental collision.

ALTER TABLE pipeline_run RENAME COLUMN run_id TO params_run_id;
ALTER INDEX idx_pipeline_run_run_id RENAME TO idx_pipeline_run_params_run_id;

ALTER TABLE technical_debt RENAME COLUMN run_id TO pipeline_run_id;
ALTER INDEX idx_technical_debt_run RENAME TO idx_technical_debt_pipeline_run_id;
ALTER INDEX idx_technical_debt_run_satd RENAME TO idx_technical_debt_pipeline_run_id_satd;

ALTER TABLE pipeline_error RENAME COLUMN run_id TO pipeline_run_id;
ALTER INDEX idx_pipeline_error_run RENAME TO idx_pipeline_error_pipeline_run_id;
