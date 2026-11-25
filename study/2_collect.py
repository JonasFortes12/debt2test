"""Step 2: read the finished runs from the database (read-only) into data/raw/*.csv. Raw data is immutable."""
import csv
import os
import shutil
import stat
import sys
from datetime import datetime, timezone

import psycopg

from common import (DONE, CHECKSUM_FILE, consolidated, item_uid, load_config, manifest_path, parse_args,
                    read_manifest, write_checksums)

RUNS_SQL = """SELECT id::text AS run_id, repository_url, requested_revision, resolved_revision, status,
       started_at, finished_at, triggered_by, context_enabled, candidate_count, satd_count,
       generated_test_count
  FROM pipeline_run WHERE id = ANY(%s::uuid[]) ORDER BY started_at"""

ITEMS_SQL = """SELECT td.pipeline_run_id::text AS run_id, td.candidate_id, td.file_path, td.method_name,
       td.line_number, td.comment, td.method_source_code, td.satd, td.debt_type, td.confidence,
       td.item_status, dc.context_status, dc.task_key, dc.summary AS task_summary,
       dc.description AS task_description, dc.url AS task_url, gt.status AS test_status,
       gt.source_code AS test_source, gt.provider AS test_provider, gt.model AS test_model,
       gt.validation_status AS test_validation_status
  FROM technical_debt td
  LEFT JOIN debt_context dc ON dc.technical_debt_id = td.id
  LEFT JOIN generated_test gt ON gt.technical_debt_id = td.id
 WHERE td.pipeline_run_id = ANY(%s::uuid[]) ORDER BY td.pipeline_run_id, td.candidate_id"""

REFERENCES_SQL = """SELECT td.pipeline_run_id::text AS run_id, td.candidate_id, er.reference_value, er.source
  FROM external_reference er
  JOIN debt_context dc ON dc.id = er.debt_context_id
  JOIN technical_debt td ON td.id = dc.technical_debt_id
 WHERE td.pipeline_run_id = ANY(%s::uuid[]) ORDER BY td.pipeline_run_id, td.candidate_id, er.id"""

ERRORS_SQL = """SELECT pipeline_run_id::text AS run_id, candidate_id, stage, code, message, recoverable
  FROM pipeline_error WHERE pipeline_run_id = ANY(%s::uuid[]) ORDER BY pipeline_run_id, id"""

MANIFEST_COLUMNS = ["repo_id", "sha", "group", "condition", "rep", "attempt"]


def fetch(conn, sql, run_ids):
    with conn.cursor() as cursor:
        cursor.execute(sql, (run_ids,))
        columns = [column.name for column in cursor.description]
        return [dict(zip(columns, row)) for row in cursor.fetchall()]


def write_csv(path, rows, columns):
    with path.open("w", newline="") as handle:
        writer = csv.DictWriter(handle, columns)
        writer.writeheader()
        writer.writerows(rows)
    path.chmod(stat.S_IRUSR | stat.S_IRGRP | stat.S_IROTH)


def incomplete_runs(runs, items):
    """Runs whose stored items differ from the candidate count the run itself reported (silent persistence loss)."""
    stored = {}
    for item in items:
        stored[item["run_id"]] = stored.get(item["run_id"], 0) + 1
    return [f"{run['run_id']} ({run['repo_id']}/{run['condition']}): reported {run['candidate_count']}, "
            f"stored {stored.get(run['run_id'], 0)}"
            for run in runs if stored.get(run["run_id"], 0) != run["candidate_count"]]


def archive_previous(raw):
    stamp = datetime.now(timezone.utc).strftime("%Y%m%dT%H%M%SZ")
    target = raw / "_archive" / stamp
    target.mkdir(parents=True)
    for entry in raw.iterdir():
        if entry.name != "_archive":
            shutil.move(str(entry), target / entry.name)


def main():
    args = parse_args(__doc__, lambda p: p.add_argument(
        "--force", action="store_true", help="archive the existing raw batch and collect again"))
    cfg = load_config(args)
    raw = args.workdir / "data" / "raw"
    batch_exists = (raw / CHECKSUM_FILE).exists()
    if batch_exists and not args.force:
        sys.exit(f"{raw} already holds a collected batch; use --force to archive it and collect again")

    manifest = {key: row for key, row in consolidated(read_manifest(manifest_path(args.workdir))).items()
                if row["status"] in DONE}
    if not manifest:
        sys.exit("no finished runs in the manifest: run 1_run.py first")
    by_run = {row["run_id"]: row for row in manifest.values()}
    run_ids = sorted(by_run)

    url = os.environ.get(cfg["db"]["url_env"])
    if not url:
        sys.exit(f"environment variable {cfg['db']['url_env']} is not set")
    with psycopg.connect(url) as conn:
        conn.read_only = True
        runs = fetch(conn, RUNS_SQL, run_ids)
        items = fetch(conn, ITEMS_SQL, run_ids)
        references = fetch(conn, REFERENCES_SQL, run_ids)
        errors = fetch(conn, ERRORS_SQL, run_ids)

    for run in runs:
        run.update({column: by_run[run["run_id"]][column] for column in MANIFEST_COLUMNS})
    missing = incomplete_runs(runs, items)
    if missing:
        sys.exit("the database holds fewer items than these runs reported (a persistence error during the run). "
                 "Nothing was written. Re-run them (see README 'Re-running a run').\n  " + "\n  ".join(missing))

    if batch_exists:  # only after validation, so a failed re-collection leaves the old batch in place
        archive_previous(raw)
    raw.mkdir(parents=True, exist_ok=True)
    sha_by_run = {run["run_id"]: run["resolved_revision"] or run["sha"] for run in runs}
    for item in items:
        run = by_run[item["run_id"]]
        item.update({column: run[column] for column in MANIFEST_COLUMNS})
        item["sha"] = sha_by_run[item["run_id"]]
        item["item_uid"] = item_uid(run["repo_url"], item["sha"], item["file_path"], item["line_number"],
                                    item["method_name"])
    for run in runs:
        run["sha"] = sha_by_run[run["run_id"]]

    write_csv(raw / "runs.csv", runs, list(runs[0]) if runs else ["run_id"])
    item_columns = (list(items[0]) if items else
                    ["run_id", "candidate_id", "item_uid"] + MANIFEST_COLUMNS)
    write_csv(raw / "items.csv", items, item_columns)
    write_csv(raw / "references.csv", references, ["run_id", "candidate_id", "reference_value", "source"])
    write_csv(raw / "errors.csv", errors,
              ["run_id", "candidate_id", "stage", "code", "message", "recoverable"])
    write_checksums(raw, ["runs.csv", "items.csv", "references.csv", "errors.csv"])
    print(f"collected {len(runs)} runs, {len(items)} items into {raw}")


if __name__ == "__main__":
    main()
