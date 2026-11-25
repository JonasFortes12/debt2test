-- candidateId is <runId>:<path>:<line>:<column>:<method>:<class>:<signature>; real repositories (e.g. Jenkins)
-- produce ids over 255 characters, which rolled back the whole candidate batch of a run.
ALTER TABLE technical_debt ALTER COLUMN candidate_id TYPE TEXT;
ALTER TABLE pipeline_error ALTER COLUMN candidate_id TYPE TEXT;
