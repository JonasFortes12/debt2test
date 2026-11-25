"""2_collect, 3_annotation_export and 4_analyze through their command lines, on a real PostgreSQL."""
import importlib.util
import json
import math
import stat

import pandas as pd
import pytest
from openpyxl import load_workbook
from scipy.stats import binomtest

from conftest import REPOS, SHA_B, STUDY, run_step, write_config
from seed import seed


@pytest.fixture
def collected(tmp_path, pg_url):
    write_config(tmp_path, REPOS)
    runs = seed(pg_url, tmp_path)
    result = run_step("2_collect.py", tmp_path, env={"STUDY_DB_URL": pg_url})
    assert result.returncode == 0, result.stderr
    return tmp_path, runs, pg_url


def items_of(workdir):
    return pd.read_csv(workdir / "data" / "raw" / "items.csv", dtype=str, keep_default_na=False)


def test_collect_writes_traceable_read_only_raw_data(collected):
    workdir, runs, _ = collected
    raw = workdir / "data" / "raw"
    items = items_of(workdir)

    assert len(items) == 18
    assert set(items["run_id"]) == set(runs.values())
    assert set(items[items["repo_id"] == "repoB"]["sha"]) == {SHA_B}
    both = items[items["repo_id"] == "repoB"].groupby("item_uid")["condition"].nunique()
    assert (both == 2).all()  # the same item has the same uid in both conditions
    for name in ("runs.csv", "items.csv", "references.csv", "errors.csv"):
        assert not (raw / name).stat().st_mode & stat.S_IWUSR
    assert (raw / "SHA256SUMS").read_text().count("\n") == 4


def test_collect_refuses_to_overwrite_and_force_archives(collected):
    workdir, _, pg_url = collected
    env = {"STUDY_DB_URL": pg_url}

    assert run_step("2_collect.py", workdir, env=env).returncode != 0
    assert run_step("2_collect.py", workdir, "--force", env=env).returncode == 0
    assert len(list((workdir / "data" / "raw" / "_archive").iterdir())) == 1


def test_collect_fails_loudly_when_a_run_lost_items_in_the_database(tmp_path, pg_url):
    import psycopg
    write_config(tmp_path, REPOS)
    runs = seed(pg_url, tmp_path)
    with psycopg.connect(pg_url) as conn:
        conn.execute("UPDATE pipeline_run SET candidate_count = 99 WHERE id = %s", (runs[("repoA", "no_context")],))

    result = run_step("2_collect.py", tmp_path, env={"STUDY_DB_URL": pg_url})

    assert result.returncode != 0 and "reported 99, stored 6" in result.stderr
    assert not (tmp_path / "data" / "raw" / "SHA256SUMS").exists()


def test_analyze_aborts_when_raw_data_was_edited(collected):
    workdir, _, _ = collected
    target = workdir / "data" / "raw" / "items.csv"
    target.chmod(0o644)
    target.write_text(target.read_text() + "\n")

    result = run_step("4_analyze.py", workdir)

    assert result.returncode != 0 and "raw data changed" in result.stderr


def test_qp1_counts_references_and_traces_runs(collected):
    workdir, runs, _ = collected
    assert run_step("4_analyze.py", workdir).returncode == 0

    table = pd.read_csv(workdir / "results" / "qp1" / "table.csv").set_index("group")
    assert (table.loc["A", "satd_items"], table.loc["A", "with_issue_reference"]) == (6, 2)
    assert (table.loc["B", "satd_items"], table.loc["B", "with_issue_reference"]) == (6, 5)
    assert (table.loc["all", "satd_items"], table.loc["all", "with_issue_reference"]) == (12, 7)
    assert table.loc["B (with_context)", "proportion"] == 1.0  # every referenced item resolved to an issue
    provenance = json.loads((workdir / "results" / "qp1" / "provenance.json").read_text())
    assert {r["run_id"] for r in provenance["runs"]} == set(runs.values())
    assert all(len(r["sha"]) == 40 for r in provenance["runs"])
    for name in ("table.tex", "figure.png"):
        assert (workdir / "results" / "qp1" / name).stat().st_size > 0


def export(workdir, *extra):
    return run_step("3_annotation_export.py", workdir, *extra)


def sheet_rows(path, sheet):
    rows = list(load_workbook(path)[sheet].iter_rows(values_only=True))
    return [dict(zip(rows[0], row)) for row in rows[1:]]


def test_annotation_sample_is_reproducible_and_blind(collected):
    workdir, _, _ = collected
    assert export(workdir).returncode == 0
    first = (workdir / "annotation" / "sample_manifest.json").read_text()

    assert export(workdir).returncode != 0  # would overwrite filled workbooks
    assert export(workdir, "--force").returncode == 0
    assert (workdir / "annotation" / "sample_manifest.json").read_text() == first

    sample = json.loads(first)
    assert len(sample["qp34"]["items"]) == 4 and len(sample["qp2"]["items"]) == 4
    assert len(sample["qp34"]["shared"]) == 2  # overlap 0.5 of 4
    mapping = pd.read_csv(workdir / "annotation" / "ab_mapping.csv")
    assert set(mapping["item_uid"]) == set(sample["qp34"]["items"])
    assert all({a, b} == {"with_context", "no_context"} for a, b in zip(mapping["A"], mapping["B"]))
    workbook = workdir / "annotation" / "annotator_researcher.xlsx"
    rows = sheet_rows(workbook, "QP3_QP4")
    assert rows and "condition" not in " ".join(rows[0]) and "auto" not in " ".join(rows[0])
    assert len(load_workbook(workbook)["QP2"].data_validations.dataValidation) == 1
    shared_in_both = set(r["item_uid"] for r in rows) & set(
        r["item_uid"] for r in sheet_rows(workdir / "annotation" / "annotator_advisor.xlsx", "QP3_QP4"))
    assert shared_in_both == set(sample["qp34"]["shared"])


def test_annotation_export_warns_and_degrades_when_the_pool_is_small(collected):
    workdir, _, _ = collected
    write_config(workdir, REPOS, seed=7)
    config = (workdir / "config" / "repos.yaml").read_text().replace("qp34_size: 4", "qp34_size: 100")
    (workdir / "config" / "repos.yaml").write_text(config)

    result = export(workdir)

    assert result.returncode == 0 and "WARNING" in result.stderr
    assert len(json.loads((workdir / "annotation" / "sample_manifest.json").read_text())["qp34"]["items"]) == 5  # 5 MATCHED items exist


def fill(workdir, choose):
    mapping = pd.read_csv(workdir / "annotation" / "ab_mapping.csv").set_index("item_uid")
    for name in ("researcher", "advisor"):
        path = workdir / "annotation" / f"annotator_{name}.xlsx"
        workbook = load_workbook(path)
        for row in workbook["QP2"].iter_rows(min_row=2):
            row[7].value = "N1"
        sheet = workbook["QP3_QP4"]
        header = [cell.value for cell in sheet[1]]
        for row in sheet.iter_rows(min_row=2):
            values = dict(zip(header, row))
            mapped = mapping.loc[values["item_uid"].value]
            winner_label = "A" if mapped["A"] == "with_context" else "B"
            values["preferred"].value = winner_label if choose == "context" else "tie"
            values["rubric_A"].value = 2 if mapped["A"] == "with_context" else 1
            values["rubric_B"].value = 2 if mapped["B"] == "with_context" else 1
        workbook.save(path)


def test_analysis_unblinds_annotations(collected):
    workdir, _, _ = collected
    assert export(workdir).returncode == 0
    fill(workdir, "context")

    assert run_step("4_analyze.py", workdir).returncode == 0

    qp3 = pd.read_csv(workdir / "results" / "qp3" / "table.csv").iloc[0]
    assert (qp3["with_context"], qp3["no_context"], qp3["tie"]) == (4, 0, 0)
    assert qp3["sign_test_p"] == pytest.approx(binomtest(4, 4, 0.5).pvalue)
    qp4 = pd.read_csv(workdir / "results" / "qp4" / "table.csv").set_index("condition")
    assert qp4.loc["with_context", "mean"] == 2 and qp4.loc["no_context", "mean"] == 1
    qp2 = pd.read_csv(workdir / "results" / "qp2" / "table.csv").set_index("level")
    assert qp2.loc["N1", "items"] == 4
    assert (workdir / "results" / "qp3" / "stability.csv").exists()


def test_analysis_skips_questions_without_annotations(collected):
    workdir, _, _ = collected
    result = run_step("4_analyze.py", workdir)
    assert result.returncode == 0 and "skipped" in result.stdout
    assert not (workdir / "results" / "qp3" / "table.csv").exists()


def load_analyze():
    spec = importlib.util.spec_from_file_location("analyze", STUDY / "4_analyze.py")
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def test_wilson_and_weighted_kappa_match_hand_calculations():
    analyze = load_analyze()
    low, high = analyze.wilson(5, 10)
    assert (round(low, 4), round(high, 4)) == (0.2366, 0.7634)
    assert analyze.weighted_kappa(["N0", "N1", "N2"], ["N0", "N1", "N2"], analyze.LEVELS) == pytest.approx(1.0)
    # one adjacent disagreement in three items: kappa_w = 1 - 0.1111/0.5185 ... computed by hand below
    first, second = ["N0", "N1", "N2"], ["N0", "N1", "N3"]
    observed_weighted = (1 / 3) * (1 / 3)
    marginals_a = {c: first.count(c) / 3 for c in analyze.LEVELS}
    marginals_b = {c: second.count(c) / 3 for c in analyze.LEVELS}
    expected_weighted = sum(abs(i - j) / 3 * marginals_a[a] * marginals_b[b]
                            for i, a in enumerate(analyze.LEVELS) for j, b in enumerate(analyze.LEVELS))
    assert analyze.weighted_kappa(first, second, analyze.LEVELS) == pytest.approx(
        1 - observed_weighted / expected_weighted)
    assert math.isnan(analyze.weighted_kappa([], [], analyze.LEVELS))
