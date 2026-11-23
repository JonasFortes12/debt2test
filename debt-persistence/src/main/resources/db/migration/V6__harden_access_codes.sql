-- Closes races found in review of V5's access tables.
-- last_mailed_at: the per-email cooldown now covers every email sent (codes and "quota reached"
-- notices alike), not only issued codes.
-- The partial unique index guarantees at most one usable (unused, unrevoked) code per participant,
-- so parallel requests can never hand out extra codes beyond the run quota.

ALTER TABLE participant ADD COLUMN last_mailed_at TIMESTAMPTZ;

CREATE UNIQUE INDEX uq_access_code_one_active_per_participant
    ON access_code (participant_id)
    WHERE used_at IS NULL AND revoked_at IS NULL;
