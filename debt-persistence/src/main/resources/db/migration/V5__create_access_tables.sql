-- Access codes and consent records.
-- These tables hold access/consent data whose policy (quota, expiry, single use) is owned by
-- debt-api. The pipeline never reads them. The only link is access_code.execution_id, which
-- points from access to pipeline_run, never the other way.

CREATE TABLE participant (
    id                 UUID         PRIMARY KEY,
    name               VARCHAR(200) NOT NULL,
    email              VARCHAR(320) NOT NULL,
    run_quota_override INTEGER,
    created_at         TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uq_participant_email UNIQUE (email),
    CONSTRAINT ck_participant_run_quota_override CHECK (run_quota_override IS NULL OR run_quota_override >= 0)
);

CREATE TABLE access_code (
    id                   UUID        PRIMARY KEY,
    participant_id       UUID        NOT NULL REFERENCES participant (id),
    code_hash            CHAR(64)    NOT NULL,
    terms_version        VARCHAR(32) NOT NULL,
    consented_at         TIMESTAMPTZ NOT NULL,
    consent_ip           VARCHAR(45) NOT NULL,
    issued_at            TIMESTAMPTZ NOT NULL,
    expires_at           TIMESTAMPTZ NOT NULL,
    revoked_at           TIMESTAMPTZ,
    used_at              TIMESTAMPTZ,
    execution_id         UUID        REFERENCES pipeline_run (id),
    results_access_until TIMESTAMPTZ,
    CONSTRAINT uq_access_code_code_hash UNIQUE (code_hash),
    CONSTRAINT ck_access_code_run_requires_use CHECK (execution_id IS NULL OR used_at IS NOT NULL)
);

CREATE INDEX idx_access_code_participant_id ON access_code (participant_id);
