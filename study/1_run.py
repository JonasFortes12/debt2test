"""Step 1: start the study's runs through the API and keep run_manifest.csv. Resumable."""
import os
import sys
import time
from datetime import datetime, timezone

import requests

from common import (DONE, TERMINAL, append_manifest, conditions_of, consolidated, load_config,
                    manifest_path, parse_args, read_manifest)


class Api:
    def __init__(self, base_url, key):
        self.base = base_url.rstrip("/") + "/api/pipeline/runs"
        self.http = requests.Session()
        self.http.headers["X-Study-Key"] = key

    def submit(self, body):
        return self.http.post(self.base, json=body, timeout=120)

    def status(self, run_id):
        """The run's status, or None when the API does not know the run."""
        response = self.http.get(f"{self.base}/{run_id}", timeout=60)
        if response.status_code == 404:
            return None
        response.raise_for_status()
        return response.json()["status"]


def require_env(name):
    value = os.environ.get(name)
    if not value:
        sys.exit(f"environment variable {name} is not set")
    return value


def context_body(repo):
    jira = repo["jira"]
    body = {"provider": "jira", "baseUrl": jira["base_url"]}
    if jira.get("email_env"):
        body["email"] = require_env(jira["email_env"])
        body["apiToken"] = require_env(jira["token_env"])
    return body


def request_body(cfg, repo, condition):
    body = {"repositoryUrl": repo["url"], "revision": repo["sha"],
            "allowHeuristicFallback": cfg["study"]["allow_heuristic_fallback"],
            "testFramework": cfg["study"]["test_framework"]}
    if condition == "with_context":
        body["context"] = context_body(repo)
    return body


def validate(cfg):
    for repo in cfg["repos"]:
        if repo["group"] == "B":
            if "jira" not in repo:
                sys.exit(f"repo {repo['id']}: group B needs a jira block")
            context_body(repo)  # fails fast on missing credentials
        if len(repo["sha"]) != 40:
            sys.exit(f"repo {repo['id']}: sha must be a full 40-character commit SHA")


def now():
    return datetime.now(timezone.utc).isoformat(timespec="seconds")


def wait_for_terminal(api, run_id, cfg):
    """Polls until the run is terminal; None if the API forgot it."""
    deadline = time.monotonic() + cfg["study"]["run_timeout_minutes"] * 60
    while True:
        status = api.status(run_id)
        if status is None or status in TERMINAL:
            return status
        if time.monotonic() > deadline:
            sys.exit(f"run {run_id} still {status} after the timeout; re-run 1_run.py to resume")
        time.sleep(cfg["study"]["poll_seconds"])


class Runner:
    def __init__(self, cfg, api, manifest):
        self.cfg, self.api, self.manifest = cfg, api, manifest

    def record(self, repo, condition, rep, attempt, run_id, status):
        append_manifest(self.manifest, {
            "repo_id": repo["id"], "repo_url": repo["url"], "sha": repo["sha"], "group": repo["group"],
            "condition": condition, "rep": rep, "attempt": attempt, "run_id": run_id, "status": status,
            "model": f"{self.cfg['model']['provider']}/{self.cfg['model']['name']}", "recorded_at": now()})

    def start(self, repo, condition, rep, attempt):
        response = self.api.submit(request_body(self.cfg, repo, condition))
        if 400 <= response.status_code < 500:
            sys.exit(f"API refused {repo['id']}/{condition}: {response.status_code} {response.text}")
        if response.status_code != 202:
            print(f"  rejected ({response.status_code}); will retry on the next attempt")
            return self.record(repo, condition, rep, attempt, "", "REJECTED")
        run_id = response.json()["executionId"]
        self.record(repo, condition, rep, attempt, run_id, "QUEUED")
        self.record(repo, condition, rep, attempt, run_id, wait_for_terminal(self.api, run_id, self.cfg) or "LOST")

    def process(self, repo, condition, rep, last):
        if last is None:
            return self.start(repo, condition, rep, 1)
        status = last["status"]
        if status in DONE:
            return print("  done, skipped")
        if status not in TERMINAL:  # the script died while this run was in flight
            status = wait_for_terminal(self.api, last["run_id"], self.cfg) or "LOST"
            self.record(repo, condition, rep, int(last["attempt"]), last["run_id"], status)
            if status in DONE:
                return print(f"  recovered: {status}")
        if int(last["attempt"]) >= self.cfg["study"]["max_attempts"]:
            return print(f"  {status}, attempts exhausted")
        self.start(repo, condition, rep, int(last["attempt"]) + 1)


def main():
    args = parse_args(__doc__)
    cfg = load_config(args)
    validate(cfg)
    manifest = manifest_path(args.workdir)
    api = Api(require_env(cfg["api"]["base_url_env"]), require_env(cfg["api"]["key_env"]))
    runner = Runner(cfg, api, manifest)
    for repo in cfg["repos"]:
        for condition in conditions_of(repo):
            for rep in range(1, cfg["study"]["repetitions"] + 1):
                print(f"{repo['id']} {condition} rep {rep}")
                state = consolidated(read_manifest(manifest))
                runner.process(repo, condition, rep, state.get((repo["id"], condition, rep)))


if __name__ == "__main__":
    main()
