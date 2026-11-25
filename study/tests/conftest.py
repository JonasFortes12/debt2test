"""Fixtures: a real PostgreSQL (throwaway docker container, real Flyway migrations) and a fake pipeline API."""
import json
import shutil
import subprocess
import sys
import threading
import time
from http.server import BaseHTTPRequestHandler, HTTPServer
from pathlib import Path

import psycopg
import pytest
import yaml

STUDY = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(STUDY))
MIGRATIONS = STUDY.parent / "debt-persistence" / "src" / "main" / "resources" / "db" / "migration"


def run_step(script, workdir, *extra, env=None):
    import os
    return subprocess.run([sys.executable, str(STUDY / script), "--workdir", str(workdir), *extra],
                          capture_output=True, text=True, env={**os.environ, **(env or {})})


@pytest.fixture(scope="session")
def pg_url():
    if not shutil.which("docker"):
        pytest.skip("docker is required for the real-PostgreSQL tests")
    name = f"study-test-pg-{int(time.time())}"
    subprocess.run(["docker", "run", "-d", "--rm", "--name", name, "-e", "POSTGRES_PASSWORD=pw",
                    "-e", "POSTGRES_DB=study", "-p", "127.0.0.1::5432", "postgres:16-alpine"],
                   check=True, capture_output=True)
    try:
        port = subprocess.run(["docker", "port", name, "5432/tcp"], check=True, capture_output=True,
                              text=True).stdout.split(":")[-1].strip()
        url = f"postgresql://postgres:pw@127.0.0.1:{port}/study"
        for _ in range(60):
            try:
                with psycopg.connect(url) as conn:
                    for migration in sorted(MIGRATIONS.glob("V*.sql"), key=lambda p: int(p.name[1:].split("__")[0])):
                        conn.execute(migration.read_text())
                break
            except psycopg.OperationalError:
                time.sleep(1)
        else:
            pytest.fail("test PostgreSQL did not start")
        yield url
    finally:
        subprocess.run(["docker", "rm", "-f", name], capture_output=True)


class FakeApi:
    """Answers like the pipeline API. `outcomes` is a queue of final statuses handed to new runs."""

    def __init__(self):
        self.runs, self.posts, self.outcomes, self.headers = {}, [], [], []
        api = self

        class Handler(BaseHTTPRequestHandler):
            def log_message(self, *args):
                pass

            def reply(self, code, payload):
                body = json.dumps(payload).encode()
                self.send_response(code)
                self.send_header("Content-Type", "application/json")
                self.send_header("Content-Length", str(len(body)))
                self.end_headers()
                self.wfile.write(body)

            def do_POST(self):
                api.headers.append(self.headers.get("X-Study-Key"))
                api.posts.append(json.loads(self.rfile.read(int(self.headers["Content-Length"]))))
                run_id = f"00000000-0000-0000-0000-{len(api.posts):012d}"
                api.runs[run_id] = api.outcomes.pop(0) if api.outcomes else "COMPLETED"
                self.reply(202, {"executionId": run_id})

            def do_GET(self):
                run_id = self.path.rsplit("/", 1)[-1]
                if run_id in api.runs:
                    self.reply(200, {"status": api.runs[run_id]})
                else:
                    self.reply(404, {"message": "not found"})

        self.server = HTTPServer(("127.0.0.1", 0), Handler)
        threading.Thread(target=self.server.serve_forever, daemon=True).start()
        self.url = f"http://127.0.0.1:{self.server.server_port}"


@pytest.fixture
def fake_api():
    api = FakeApi()
    yield api
    api.server.shutdown()


def write_config(workdir, repos, **study):
    config = {
        "api": {"base_url_env": "STUDY_API_URL", "key_env": "STUDY_API_KEY"},
        "db": {"url_env": "STUDY_DB_URL"},
        "study": {"repetitions": 1, "max_attempts": 2, "poll_seconds": 0, "run_timeout_minutes": 1, "seed": 7,
                  "allow_heuristic_fallback": False, "test_framework": "JUnit 5", **study},
        "model": {"provider": "openai", "name": "m", "temperature": 0},
        "sample": {"qp34_size": 4, "qp2_size": 4, "overlap": 0.5},
        "annotators": ["researcher", "advisor"],
        "repos": repos,
    }
    (workdir / "config").mkdir(parents=True, exist_ok=True)
    (workdir / "config" / "repos.yaml").write_text(yaml.safe_dump(config))
    return config


SHA_A = "a" * 40
SHA_B = "b" * 40
REPOS = [
    {"id": "repoA", "url": "https://example.test/a", "sha": SHA_A, "group": "A"},
    {"id": "repoB", "url": "https://example.test/b", "sha": SHA_B, "group": "B",
     "jira": {"base_url": "https://jira.example.test"}},
]
