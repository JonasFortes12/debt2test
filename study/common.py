"""Shared helpers for the four study steps (see README.md). No step imports another step."""
import argparse
import csv
import hashlib
import json
import os
import subprocess
import sys
from pathlib import Path

import yaml

DONE = {"COMPLETED", "COMPLETED_WITH_ERRORS"}
# LOST: the API no longer knows the run. REJECTED: the API refused it transiently (5xx, unreachable).
TERMINAL = DONE | {"FAILED", "CANCELLED", "LOST", "REJECTED"}
MANIFEST_FIELDS = ["repo_id", "repo_url", "sha", "group", "condition", "rep", "attempt",
                   "run_id", "status", "model", "recorded_at"]
CHECKSUM_FILE = "SHA256SUMS"


def parse_args(description, extra=None):
    parser = argparse.ArgumentParser(description=description)
    parser.add_argument("--workdir", type=Path, default=Path(__file__).resolve().parent,
                        help="folder holding config/, data/, annotation/ and results/ (default: study/)")
    parser.add_argument("--config", type=Path, help="default: <workdir>/config/repos.yaml")
    if extra:
        extra(parser)
    args = parser.parse_args()
    args.config = args.config or args.workdir / "config" / "repos.yaml"
    load_dotenv(args.workdir / ".env")
    load_dotenv(Path(__file__).resolve().parent.parent / ".env")
    return args


def load_dotenv(path):
    """Minimal .env reader; real environment variables win."""
    if not path.is_file():
        return
    for line in path.read_text().splitlines():
        line = line.strip()
        if line and not line.startswith("#") and "=" in line:
            key, value = line.split("=", 1)
            os.environ.setdefault(key.strip(), value.strip().strip("'\""))


def load_config(args):
    return yaml.safe_load(args.config.read_text())


def conditions_of(repo):
    return ["no_context", "with_context"] if repo["group"] == "B" else ["no_context"]


def manifest_path(workdir):
    return workdir / "data" / "run_manifest.csv"


def read_manifest(path):
    if not path.is_file():
        return []
    with path.open(newline="") as handle:
        return list(csv.DictReader(handle))


def append_manifest(path, row):
    """The manifest is append-only: rows are never rewritten."""
    path.parent.mkdir(parents=True, exist_ok=True)
    is_new = not path.is_file()
    with path.open("a", newline="") as handle:
        writer = csv.DictWriter(handle, MANIFEST_FIELDS)
        if is_new:
            writer.writeheader()
        writer.writerow(row)
        handle.flush()
        os.fsync(handle.fileno())


def unit_key(row):
    return (row["repo_id"], row["condition"], int(row["rep"]))


def consolidated(rows):
    """Latest manifest row of every (repo, condition, rep): the current state of each unit."""
    return {unit_key(row): row for row in rows}


def item_uid(repo_url, sha, file_path, line_number, method_name):
    text = "|".join([repo_url, sha, file_path, str(line_number), method_name])
    return hashlib.sha256(text.encode()).hexdigest()[:16]


def file_sha256(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def write_checksums(directory, names):
    lines = [f"{file_sha256(directory / name)}  {name}" for name in sorted(names)]
    (directory / CHECKSUM_FILE).write_text("\n".join(lines) + "\n")


def verify_checksums(directory):
    """Aborts when any raw file differs from the checksum recorded at collection time."""
    sums = directory / CHECKSUM_FILE
    if not sums.is_file():
        sys.exit(f"{sums} missing: run 2_collect.py first")
    for line in sums.read_text().splitlines():
        digest, name = line.split("  ", 1)
        if not (directory / name).is_file() or file_sha256(directory / name) != digest:
            sys.exit(f"raw data changed: {name} does not match {CHECKSUM_FILE}. Raw data is immutable; "
                     "re-collect with 2_collect.py --force instead of editing it.")


def git_commit():
    here = Path(__file__).resolve().parent
    try:
        commit = subprocess.run(["git", "rev-parse", "HEAD"], cwd=here, capture_output=True, text=True,
                                check=True).stdout.strip()
        dirty = subprocess.run(["git", "status", "--porcelain", "--", str(here)], cwd=here,
                               capture_output=True, text=True, check=True).stdout.strip()
        return commit + ("+dirty" if dirty else "")
    except (OSError, subprocess.CalledProcessError):
        return "unknown"


def write_json(path, payload):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(payload, indent=2, sort_keys=True) + "\n")
