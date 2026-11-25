"""Seeds the real schema with a small, hand-countable study: 2 repos, 3 runs, 18 SATD items."""
import uuid

import psycopg

from common import append_manifest
from conftest import REPOS, SHA_A, SHA_B

COMMENTS = ["TODO", "TODO fix", "FIXME: validate the order total before saving it", "HACK workaround for bug",
            "TODO: remove after the migration described in PROJ-7", "refactor this method, it does too much"]


def seed(pg_url, workdir):
    """Returns {(repo_id, condition): run_id}. Items 0-1 of repoA and 0-4 of repoB carry an issue reference."""
    runs = {}
    with psycopg.connect(pg_url) as conn:
        for repo, condition, sha in [(REPOS[0], "no_context", SHA_A), (REPOS[1], "no_context", SHA_B),
                                     (REPOS[1], "with_context", SHA_B)]:
            run_id = str(uuid.uuid4())
            runs[(repo["id"], condition)] = run_id
            conn.execute(
                """INSERT INTO pipeline_run (id, repository_url, requested_revision, resolved_revision, status,
                   started_at, finished_at, triggered_by, context_enabled, heuristic_fallback_allowed,
                   test_framework, candidate_count, satd_count, generated_test_count)
                   VALUES (%s, %s, %s, %s, 'COMPLETED', now(), now(), 'study', %s, false, 'JUnit 5', 6, 6, 6)""",
                (run_id, repo["url"], sha, sha, condition == "with_context"))
            for index, comment in enumerate(COMMENTS):
                insert_item(conn, run_id, repo, condition, index, comment)
    for (repo_id, condition), run_id in runs.items():
        repo = next(r for r in REPOS if r["id"] == repo_id)
        append_manifest(workdir / "data" / "run_manifest.csv", {
            "repo_id": repo_id, "repo_url": repo["url"], "sha": repo["sha"], "group": repo["group"],
            "condition": condition, "rep": 1, "attempt": 1, "run_id": run_id, "status": "COMPLETED",
            "model": "openai/m", "recorded_at": "t"})
    return runs


def insert_item(conn, run_id, repo, condition, index, comment):
    debt_type = "DESIGN" if index % 2 == 0 else "TEST"
    has_reference = index < (2 if repo["group"] == "A" else 5)
    td_id = conn.execute(
        """INSERT INTO technical_debt (pipeline_run_id, candidate_id, file_path, method_name, line_number, comment,
           method_source_code, source_repository_url, source_relative_file_path, satd, debt_type, confidence,
           item_status, extracted_at) VALUES (%s, %s, %s, %s, %s, %s, 'void m() {}', %s, %s, true, %s, 0.9,
           'GENERATED', now()) RETURNING id""",
        (run_id, f"{run_id}:{index}", f"src/F{index}.java", f"m{index}", 10 + index, comment, repo["url"],
         f"src/F{index}.java", debt_type)).fetchone()[0]
    matched = condition == "with_context" and has_reference
    status = "MATCHED" if matched else ("SKIPPED" if condition == "no_context" else "NOT_FOUND")
    dc_id = conn.execute(
        """INSERT INTO debt_context (technical_debt_id, context_status, provider, task_key, summary, description,
           url, enriched_at) VALUES (%s, %s, %s, %s, %s, %s, %s, now()) RETURNING id""",
        (td_id, status, "jira" if matched else None, f"PROJ-{index}" if matched else None,
         f"summary {index}" if matched else None, f"description {index}" if matched else None,
         f"https://jira.example.test/browse/PROJ-{index}" if matched else None)).fetchone()[0]
    if has_reference:
        conn.execute("INSERT INTO external_reference (debt_context_id, reference_value, source) VALUES (%s, %s, 'comment')",
                     (dc_id, f"PROJ-{index}"))
    conn.execute(
        """INSERT INTO generated_test (technical_debt_id, source_code, framework, provider, model, prompt_version,
           status, validation_status, generated_at) VALUES (%s, %s, 'JUnit 5', 'openai', 'm', 'v1', 'GENERATED',
           'NOT_RUN', now())""", (td_id, f"// {condition} test {index}\n@Test void t{index}() {{}}"))
