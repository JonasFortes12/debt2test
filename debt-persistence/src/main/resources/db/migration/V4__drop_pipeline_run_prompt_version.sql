-- The prompt version is no longer a run request option: debt-tester owns it as a constant
-- next to the prompt text it labels (TestGeneratorService.PROMPT_VERSION). Provenance stays
-- on each generated_test row, which is where the version actually applies.

ALTER TABLE pipeline_run DROP COLUMN prompt_version;
