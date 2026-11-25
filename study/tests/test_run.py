"""1_run.py through its command line, against a fake HTTP API."""
import csv

from common import MANIFEST_FIELDS, append_manifest
from conftest import REPOS, SHA_A, run_step, write_config


def env(api):
    return {"STUDY_API_URL": api.url, "STUDY_API_KEY": "k3y"}


def manifest(workdir):
    with (workdir / "data" / "run_manifest.csv").open(newline="") as handle:
        return list(csv.DictReader(handle))


def test_resume_skips_finished_runs_and_retries_failed_ones(tmp_path, fake_api):
    write_config(tmp_path, REPOS)
    fake_api.outcomes = ["COMPLETED", "FAILED", "COMPLETED"]  # A/no_context, B/no_context, B/with_context
    first = run_step("1_run.py", tmp_path, env=env(fake_api))
    assert first.returncode == 0, first.stderr
    assert len(fake_api.posts) == 3
    before = manifest(tmp_path)

    run_step("1_run.py", tmp_path, env=env(fake_api))

    assert len(fake_api.posts) == 4  # only the FAILED unit ran again
    after = manifest(tmp_path)
    assert after[:len(before)] == before  # append-only
    assert [r["attempt"] for r in after if r["repo_id"] == "repoB" and r["condition"] == "no_context"][-1] == "2"

    run_step("1_run.py", tmp_path, env=env(fake_api))
    assert len(fake_api.posts) == 4  # everything is done now


def test_attempts_are_capped_by_max_attempts(tmp_path, fake_api):
    write_config(tmp_path, REPOS[:1], max_attempts=2)
    fake_api.outcomes = ["FAILED", "FAILED"]
    run_step("1_run.py", tmp_path, env=env(fake_api))
    run_step("1_run.py", tmp_path, env=env(fake_api))
    run_step("1_run.py", tmp_path, env=env(fake_api))
    assert len(fake_api.posts) == 2


def test_in_flight_run_is_recovered_without_a_new_submission(tmp_path, fake_api):
    write_config(tmp_path, REPOS[:1])
    run_id = "00000000-0000-0000-0000-00000000aaaa"
    fake_api.runs[run_id] = "COMPLETED"
    append_manifest(tmp_path / "data" / "run_manifest.csv", {
        "repo_id": "repoA", "repo_url": REPOS[0]["url"], "sha": SHA_A, "group": "A", "condition": "no_context",
        "rep": 1, "attempt": 1, "run_id": run_id, "status": "QUEUED", "model": "openai/m", "recorded_at": "t"})

    run_step("1_run.py", tmp_path, env=env(fake_api))

    assert fake_api.posts == []
    assert manifest(tmp_path)[-1]["status"] == "COMPLETED"


def test_run_unknown_to_the_api_is_marked_lost_and_retried(tmp_path, fake_api):
    write_config(tmp_path, REPOS[:1])
    append_manifest(tmp_path / "data" / "run_manifest.csv", {
        "repo_id": "repoA", "repo_url": REPOS[0]["url"], "sha": SHA_A, "group": "A", "condition": "no_context",
        "rep": 1, "attempt": 1, "run_id": "ffffffff-0000-0000-0000-000000000000", "status": "RUNNING",
        "model": "openai/m", "recorded_at": "t"})

    run_step("1_run.py", tmp_path, env=env(fake_api))

    statuses = [(r["attempt"], r["status"]) for r in manifest(tmp_path)]
    assert ("1", "LOST") in statuses and ("2", "COMPLETED") in statuses
    assert len(fake_api.posts) == 1


def test_request_carries_the_study_key_and_context_only_for_the_with_context_condition(tmp_path, fake_api):
    write_config(tmp_path, REPOS[1:])
    run_step("1_run.py", tmp_path, env=env(fake_api))

    no_context, with_context = fake_api.posts
    assert fake_api.headers == ["k3y", "k3y"]
    assert "context" not in no_context
    assert with_context["context"] == {"provider": "jira", "baseUrl": "https://jira.example.test"}
    assert no_context["revision"] == "b" * 40
    assert list(manifest(tmp_path)[0]) == MANIFEST_FIELDS


def test_group_b_repo_without_jira_block_is_rejected_before_any_run(tmp_path, fake_api):
    broken = [{**REPOS[1]}]
    del broken[0]["jira"]
    write_config(tmp_path, broken)
    result = run_step("1_run.py", tmp_path, env=env(fake_api))
    assert result.returncode != 0 and fake_api.posts == []
