# Debt2Test study scripts

Reproducible collection and analysis for the four research questions:

| | Question | Answered by |
|---|---|---|
| QP1 | How often is SATD linked to an issue tracker? | automatic (`4_analyze.py`) |
| QP2 | How detailed is the SATD documentation (N0–N3, plus a rule-based "tracked" flag)? | two annotators |
| QP3 | Do issue-tracker context tests beat tests without it? | blind pairwise preference, two annotators |
| QP4 | Do the generated tests capture the SATD context (rubric 0–2)? | two annotators |

Four independent steps; repeat one without redoing the earlier ones (the pipeline run is the expensive one).

```
config/repos.yaml  → 1_run.py → data/run_manifest.csv
                     2_collect.py → data/raw/*.csv + SHA256SUMS      (read-only SQL)
                     3_annotation_export.py → annotation/*.xlsx (+ ab_mapping.csv, auto_labels.csv, sample_manifest.json)
                     4_analyze.py → results/qp1..qp4/{table.csv,table.tex,figure.png,provenance.json}
```

## Setup

```bash
make setup                 # uv sync: creates .venv from pyproject.toml / uv.lock (needs uv)
```

Environment (process env or a `.env` here or at the repo root; **never** commit values):

| Variable | Used by | Meaning |
|---|---|---|
| `DEBT_STUDY_API_URL` | 1_run | API base URL, e.g. `http://localhost:8182` |
| `DEBT_STUDY_API_KEY` | 1_run | same value as the server's `DEBT_STUDY_API_KEY` |
| `DEBT_STUDY_DB_URL` | 2_collect | `postgresql://<read-only user>:<pw>@host:port/db` |
| names given in `repos.yaml` | 1_run | Jira email / token per group-B repo (omit for a public Jira) |

Edit `config/repos.yaml`: repositories with **full 40-character commit SHAs**, group A or B, model (informational:
the server's `DEBT_TESTER_*` decides the real model, and `2_collect.py` records the model the database saw),
`repetitions`, `seed`, `sample.qp34_size` / `sample.qp2_size` / `sample.overlap`, annotators.

## Run

```bash
make run        # 1: start runs, resumable; safe to re-run after a crash
make collect    # 2: DB → data/raw (refuses to overwrite; --force archives the old batch)
make annotate   # 3: sample + workbooks (refuses to overwrite filled workbooks; --force)
# annotators fill annotation/annotator_<name>.xlsx (see annotation/GUIDE.md)
make analyze    # 4: results; questions without annotations yet are skipped, QP1 never waits for them
make test       # needs Docker (throwaway PostgreSQL with the real migrations)
```

Every script also takes `--workdir DIR` (default: this folder) and `--config FILE`.

## Why the numbers are defensible

* **Resumable runs.** The manifest is append-only (`repo, sha, condition, rep, attempt, run_id, status, model`).
  `COMPLETED` / `COMPLETED_WITH_ERRORS` are done and never repeated; `FAILED`, `CANCELLED`, lost or rejected runs
  are retried as a new attempt up to `max_attempts`; a run left in flight by a crash is looked up by `run_id`
  before anything is resubmitted.
* **Immutable raw data.** `data/raw/*.csv` are read-only and listed in `SHA256SUMS`. `4_analyze.py` and
  `3_annotation_export.py` refuse to start if a file changed. Every transformation lives in `4_analyze.py`.
* **Traceability.** Every raw row carries `run_id` and the resolved commit `sha`; `item_uid` (hash of repository,
  SHA, file, line, method) pairs the same item across conditions. Each `results/qpN/provenance.json` lists the
  runs used and the script's git commit.

## Method notes

* Conditions: `no_context` (no context in the request) and `with_context` (Jira). Group A runs only `no_context`.
* QP1 uses repetition 1 of `no_context`: share of SATD items with an issue reference (Wilson 95 % CI), plus the share
  of referenced items that resolved to an issue (`MATCHED`, group B). References are extracted even when context is
  skipped, so QP1 does not depend on Jira access.
* Repetitions 2+ only feed `results/qp3/stability.csv` (generation rate, issue-key mentions); human annotation
  covers repetition 1.
* QP3 sample: group-B items `MATCHED` in `with_context` with a generated test in both conditions. QP2 sample:
  SATD items of `no_context` runs (groups A and B), no `MATCHED` filter. Both stratified by debt type, seeded.
* Blindness: annotators never see the condition or the automatic pre-labels. `annotation/ab_mapping.csv` (A/B →
  condition) and `annotation/auto_labels.csv` are used only by `4_analyze.py`. QP4 scores both tests of an item; the
  analysis reports `with_context` and `no_context` scores separately.
* Annotator conflicts are excluded from distributions and written to `disagreements.csv` for adjudication.
  Weighted kappa (linear weights) uses the overlap items.

## Known limits

* Only the Jira connector exists; repositories on GitHub Issues never reach `MATCHED` (thesis limitation).
* Generated tests are not compiled or executed (`validation_status` stays `NOT_RUN`); QP3 relies on human preference.
* The model and temperature are server configuration; the manifest records what `repos.yaml` declares.
* The reproduction package is `data/raw/` plus the manifest; there is no `pg_dump`, since the database also holds
  participant data (emails) that must not be published.

## Re-running a run

`2_collect.py` refuses to write when a run's stored items differ from the candidate count the run reported (a
persistence error during the run, e.g. a schema limit). Fix the cause, then redo the study runs: the manifest is
append-only and treats `COMPLETED_WITH_ERRORS` as done, so keep the bad manifest as evidence
(`mv data/run_manifest.csv data/run_manifest.<date>.bad.csv`) and run `1_run.py` again for a fresh one.
