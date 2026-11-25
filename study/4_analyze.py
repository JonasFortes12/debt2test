"""Step 4: raw data + completed annotations -> results/qp1..qp4 (CSV, LaTeX, PNG, provenance.json)."""
import math
import sys
from datetime import datetime, timezone

import matplotlib
import numpy as np
import pandas as pd
from openpyxl import load_workbook
from scipy.stats import binomtest

from common import DONE, consolidated, git_commit, load_config, manifest_path, parse_args, read_manifest, \
    verify_checksums, write_json

matplotlib.use("Agg")
import matplotlib.pyplot as plt  # noqa: E402

LEVELS = ["N0", "N1", "N2", "N3"]


def wilson(successes, n, z=1.96):
    if n == 0:
        return (float("nan"), float("nan"))
    p = successes / n
    denominator = 1 + z * z / n
    centre = (p + z * z / (2 * n)) / denominator
    half = z * math.sqrt(p * (1 - p) / n + z * z / (4 * n * n)) / denominator
    return (centre - half, centre + half)


def weighted_kappa(first, second, categories):
    """Cohen's kappa with linear weights over ordered categories; nan when undefined."""
    k = len(categories)
    index = {category: i for i, category in enumerate(categories)}
    observed = np.zeros((k, k))
    for a, b in zip(first, second):
        observed[index[a], index[b]] += 1
    n = observed.sum()
    if n == 0 or k < 2:
        return float("nan")
    observed /= n
    expected = np.outer(observed.sum(axis=1), observed.sum(axis=0))
    weights = np.array([[abs(i - j) / (k - 1) for j in range(k)] for i in range(k)])
    denominator = (weights * expected).sum()
    return float("nan") if denominator == 0 else 1 - (weights * observed).sum() / denominator


class Context:
    def __init__(self, args):
        self.args = args
        self.cfg = load_config(args)
        self.raw = args.workdir / "data" / "raw"
        verify_checksums(self.raw)
        self.manifest = [row for row in consolidated(read_manifest(manifest_path(args.workdir))).values()
                         if row["status"] in DONE]
        self.items = pd.read_csv(self.raw / "items.csv", dtype=str, keep_default_na=False)
        self.items["rep"] = self.items["rep"].astype(int)
        self.satd = self.items[self.items["satd"] == "True"]
        self.references = pd.read_csv(self.raw / "references.csv", dtype=str, keep_default_na=False)
        self.referenced = set(zip(self.references["run_id"], self.references["candidate_id"]))

    def out(self, name):
        path = self.args.workdir / "results" / name
        path.mkdir(parents=True, exist_ok=True)
        return path

    def save(self, name, table, figure=None, runs=None):
        folder = self.out(name)
        table.to_csv(folder / "table.csv", index=False)
        table.to_latex(folder / "table.tex", index=False, float_format="%.3f", na_rep="--")
        if figure is not None:
            figure.savefig(folder / "figure.png", dpi=150, bbox_inches="tight")
            plt.close(figure)
        used = [row for row in self.manifest if runs is None or runs(row)]
        write_json(folder / "provenance.json", {
            "question": name, "script_commit": git_commit(),
            "analyzed_at": datetime.now(timezone.utc).isoformat(timespec="seconds"),
            "runs": [{"run_id": r["run_id"], "repo_id": r["repo_id"], "sha": r["sha"],
                      "condition": r["condition"], "rep": int(r["rep"])} for r in used]})
        print(f"{name}: {folder}")

    def annotations(self, sheet, columns):
        """Long table (annotator, item_uid, <columns>) from every workbook; blank cells are not annotations."""
        frames = []
        for name in self.cfg["annotators"]:
            path = self.args.workdir / "annotation" / f"annotator_{name}.xlsx"
            if not path.exists():
                continue
            rows = list(load_workbook(path, data_only=True)[sheet].iter_rows(values_only=True))
            header = list(rows[0])
            frame = pd.DataFrame(rows[1:], columns=header)[["item_uid"] + columns]
            frame.insert(0, "annotator", name)
            frames.append(frame)
        return pd.concat(frames) if frames else pd.DataFrame(columns=["annotator", "item_uid"] + columns)


def consolidate(long, value_column):
    """One value per item when annotators agree or only one answered; conflicts are returned apart."""
    answered = long.dropna(subset=[value_column])
    answered = answered[answered[value_column].astype(str).str.strip() != ""]
    agreed, conflicts, pairs = {}, [], []
    for uid, group in answered.groupby("item_uid"):
        values = list(group[value_column])
        if len(set(values)) == 1:
            agreed[uid] = values[0]
        else:
            conflicts.append({"item_uid": uid, **{r.annotator: r[value_column] for _, r in group.iterrows()}})
        if len(values) == 2:
            pairs.append(tuple(values))
    return agreed, pd.DataFrame(conflicts), pairs


def qp1(ctx):
    sample = ctx.satd[(ctx.satd["condition"] == "no_context") & (ctx.satd["rep"] == 1)].copy()
    sample["has_reference"] = [(r, c) in ctx.referenced for r, c in zip(sample["run_id"], sample["candidate_id"])]
    rows = []
    for label, frame in [("A", sample[sample["group"] == "A"]), ("B", sample[sample["group"] == "B"]), ("all", sample)]:
        n, k = len(frame), int(frame["has_reference"].sum())
        low, high = wilson(k, n)
        rows.append({"group": label, "satd_items": n, "with_issue_reference": k,
                     "proportion": k / n if n else float("nan"), "ci95_low": low, "ci95_high": high})
    table = pd.DataFrame(rows)
    matched = ctx.satd[(ctx.satd["condition"] == "with_context") & (ctx.satd["rep"] == 1)].copy()
    matched["has_reference"] = [(r, c) in ctx.referenced for r, c in zip(matched["run_id"], matched["candidate_id"])]
    referenced = matched[matched["has_reference"]]
    resolved = int((referenced["context_status"] == "MATCHED").sum())
    low, high = wilson(resolved, len(referenced))
    resolution = pd.DataFrame([{"group": "B (with_context)", "satd_items": len(referenced),
                                "with_issue_reference": resolved,
                                "proportion": resolved / len(referenced) if len(referenced) else float("nan"),
                                "ci95_low": low, "ci95_high": high}])
    table = pd.concat([table, resolution], ignore_index=True)
    figure, axis = plt.subplots(figsize=(5, 3.2))
    axis.bar(table["group"], table["proportion"].fillna(0),
             yerr=[(table["proportion"] - table["ci95_low"]).fillna(0), (table["ci95_high"] - table["proportion"]).fillna(0)])
    axis.set_ylabel("proportion of SATD items")
    axis.set_title("QP1: SATD with an issue reference (last bar: resolved to an issue)", fontsize=8)
    ctx.save("qp1", table, figure, lambda r: r["condition"] in ("no_context", "with_context") and int(r["rep"]) == 1)


def qp2(ctx):
    long = ctx.annotations("QP2", ["content_level"])
    agreed, conflicts, pairs = consolidate(long, "content_level")
    if not agreed:
        return print("qp2: no completed annotations yet; skipped")
    counts = pd.Series(agreed).value_counts().reindex(LEVELS, fill_value=0)
    table = pd.DataFrame({"level": LEVELS, "items": counts.values, "share": counts.values / counts.sum()})
    auto = pd.read_csv(ctx.args.workdir / "annotation" / "auto_labels.csv")
    tracked = auto[auto["item_uid"].isin(agreed)]["auto_tracked"].mean()
    table = pd.concat([table, pd.DataFrame([
        {"level": "weighted_kappa (overlap)", "items": len(pairs),
         "share": weighted_kappa([a for a, _ in pairs], [b for _, b in pairs], LEVELS)},
        {"level": "tracked (N4 flag, rule)", "items": len(auto[auto["item_uid"].isin(agreed)]), "share": tracked}])],
        ignore_index=True)
    figure, axis = plt.subplots(figsize=(4.5, 3))
    axis.bar(LEVELS, counts.values)
    axis.set_ylabel("items")
    axis.set_title("QP2: SATD documentation level", fontsize=9)
    ctx.save("qp2", table, figure, lambda r: r["condition"] == "no_context" and int(r["rep"]) == 1)
    if len(conflicts):
        conflicts.to_csv(ctx.out("qp2") / "disagreements.csv", index=False)


def unblinded(ctx, sheet, columns):
    path = ctx.args.workdir / "annotation" / "ab_mapping.csv"
    mapping = pd.read_csv(path).set_index("item_uid") if path.exists() else pd.DataFrame()
    return ctx.annotations(sheet, columns), mapping


def qp3(ctx):
    long, mapping = unblinded(ctx, "QP3_QP4", ["preferred"])
    agreed, conflicts, _ = consolidate(long, "preferred")
    if not agreed:
        return print("qp3: no completed annotations yet; skipped")
    winners = {"with_context": 0, "no_context": 0, "tie": 0}
    for uid, choice in agreed.items():
        winners["tie" if choice == "tie" else mapping.loc[uid, choice]] += 1
    decided = winners["with_context"] + winners["no_context"]
    p_value = binomtest(winners["with_context"], decided, 0.5).pvalue if decided else float("nan")
    table = pd.DataFrame([{**winners, "decided": decided, "sign_test_p": p_value}])
    figure, axis = plt.subplots(figsize=(4.5, 3))
    axis.bar(list(winners), list(winners.values()))
    axis.set_ylabel("items")
    axis.set_title(f"QP3: blind preference (sign test p={p_value:.3f})", fontsize=9)
    ctx.save("qp3", table, figure, lambda r: int(r["rep"]) == 1 and r["group"] == "B")
    if len(conflicts):
        conflicts.to_csv(ctx.out("qp3") / "disagreements.csv", index=False)
    stability(ctx)


def stability(ctx):
    """Automatic checks over all repetitions (human annotation covers repetition 1 only)."""
    frame = ctx.satd.copy()
    frame["generated"] = frame["test_status"] == "GENERATED"
    frame["mentions_issue_key"] = [bool(key) and key in (source or "") for key, source in
                                   zip(frame["task_key"], frame["test_source"])]
    table = frame.groupby(["condition", "rep"]).agg(
        items=("item_uid", "count"), generated_rate=("generated", "mean"),
        mentions_issue_key_rate=("mentions_issue_key", "mean")).reset_index()
    table.to_csv(ctx.out("qp3") / "stability.csv", index=False)


def qp4(ctx):
    long, mapping = unblinded(ctx, "QP3_QP4", ["rubric_A", "rubric_B"])
    rows = []
    for _, row in long.iterrows():
        for label in ("A", "B"):
            value = row[f"rubric_{label}"]
            if value is not None and not pd.isna(value):
                rows.append({"annotator": row["annotator"], "item_uid": row["item_uid"],
                             "condition": mapping.loc[row["item_uid"], label], "rubric": int(value)})
    scores = pd.DataFrame(rows, columns=["annotator", "item_uid", "condition", "rubric"])
    if scores.empty:
        return print("qp4: no completed annotations yet; skipped")
    table_rows = []
    for condition in ("with_context", "no_context"):
        subset = scores[scores["condition"] == condition]
        agreed, _, pairs = consolidate(subset, "rubric")
        counts = pd.Series(agreed).value_counts().reindex([0, 1, 2], fill_value=0)
        table_rows.append({"condition": condition, "score_0": counts[0], "score_1": counts[1], "score_2": counts[2],
                           "mean": float(np.mean(list(agreed.values()))) if agreed else float("nan"),
                           "weighted_kappa (overlap)": weighted_kappa([a for a, _ in pairs], [b for _, b in pairs], [0, 1, 2]),
                           "overlap_items": len(pairs)})
    table = pd.DataFrame(table_rows)
    figure, axis = plt.subplots(figsize=(4.5, 3))
    positions = np.arange(3)
    for offset, row in zip((-0.2, 0.2), table_rows):
        axis.bar(positions + offset, [row["score_0"], row["score_1"], row["score_2"]], width=0.4, label=row["condition"])
    axis.set_xticks(positions, ["0 ignores", "1 method only", "2 exercises SATD"], fontsize=7)
    axis.set_ylabel("items")
    axis.legend(fontsize=7)
    axis.set_title("QP4: does the test capture the SATD context?", fontsize=9)
    ctx.save("qp4", table, figure, lambda r: int(r["rep"]) == 1 and r["group"] == "B")


def main():
    args = parse_args(__doc__)
    ctx = Context(args)
    qp1(ctx)
    for step in (qp2, qp3, qp4):
        step(ctx)


if __name__ == "__main__":
    main()
