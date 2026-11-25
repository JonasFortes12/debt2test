"""Step 3: draw the reproducible annotation sample and write one fill-in workbook per annotator."""
import math
import random
import re
import sys

import pandas as pd
from openpyxl import Workbook
from openpyxl.worksheet.datavalidation import DataValidation

from common import load_config, parse_args, verify_checksums, write_json

CELL_LIMIT = 32000  # Excel caps a cell at 32767 characters
MARKERS = re.compile(r"\b(todo|fixme|hack|xxx|tbd)\b", re.IGNORECASE)
LEVELS = ["N0", "N1", "N2", "N3"]
PREFERENCES = ["A", "B", "tie"]
RUBRIC = [0, 1, 2]

QP2_COLUMNS = ["item_uid", "repo_id", "sha", "file_path", "method_name", "comment", "method_source",
               "content_level", "notes"]
QP34_COLUMNS = ["item_uid", "repo_id", "sha", "file_path", "method_name", "comment", "issue_key",
                "issue_summary", "issue_description", "method_source", "test_A", "test_B",
                "preferred", "rubric_A", "rubric_B", "notes"]


def clip(value):
    text = "" if pd.isna(value) else str(value)
    return text if len(text) <= CELL_LIMIT else text[:CELL_LIMIT] + "\n[truncated]"


def load_raw(workdir):
    raw = workdir / "data" / "raw"
    verify_checksums(raw)
    items = pd.read_csv(raw / "items.csv", dtype=str, keep_default_na=False)
    references = pd.read_csv(raw / "references.csv", dtype=str, keep_default_na=False)
    items["rep"] = items["rep"].astype(int)
    return items[items["satd"] == "True"].copy(), references


def stratified_pick(frame, size, rng):
    """Round-robin over debt_type strata, each shuffled with the seeded rng: reproducible and balanced."""
    strata = {}
    for uid, debt_type in zip(frame["item_uid"], frame["debt_type"]):
        strata.setdefault(debt_type, []).append(uid)
    for uids in strata.values():
        uids.sort()
        rng.shuffle(uids)
    picked = []
    while len(picked) < size and any(strata.values()):
        for debt_type in sorted(strata):
            if strata[debt_type] and len(picked) < size:
                picked.append(strata[debt_type].pop())
    return picked


def warn_if_short(task, picked, wanted):
    if len(picked) < wanted:
        print(f"WARNING: {task}: pool has only {len(picked)} items, {wanted} requested; exporting all",
              file=sys.stderr)


def qp34_pool(items):
    """Group-B items matched to an issue in with_context, with a generated test in both conditions."""
    base = items[(items["rep"] == 1) & (items["group"] == "B") & (items["test_status"] == "GENERATED")]
    with_ctx = base[(base["condition"] == "with_context") & (base["context_status"] == "MATCHED")]
    no_ctx = base[base["condition"] == "no_context"].set_index("item_uid")
    return with_ctx[with_ctx["item_uid"].isin(no_ctx.index)].drop_duplicates("item_uid"), no_ctx


def qp2_pool(items):
    return items[(items["rep"] == 1) & (items["condition"] == "no_context")].drop_duplicates("item_uid")


def assign(uids, annotators, overlap, rng):
    """Both annotators get ceil(overlap*n) shared items; the rest alternate between them."""
    shared = set(uids[:math.ceil(overlap * len(uids))]) if len(annotators) > 1 else set()
    assignment = {name: [] for name in annotators}
    rest = [uid for uid in uids if uid not in shared]
    for position, uid in enumerate(rest):
        assignment[annotators[position % len(annotators)]].append(uid)
    for uid in shared:
        for name in annotators:
            assignment[name].append(uid)
    for uids_of in assignment.values():
        rng.shuffle(uids_of)
    return assignment, sorted(shared)


def auto_labels(items, references, uids):
    """Rule-based pre-labels, kept out of the annotators' files so they cannot anchor the annotation."""
    referenced = set(zip(references["run_id"], references["candidate_id"]))
    pool = items[(items["rep"] == 1) & (items["condition"] == "no_context")].drop_duplicates("item_uid")
    rows = []
    for _, row in pool[pool["item_uid"].isin(uids)].iterrows():
        words = re.findall(r"[A-Za-z0-9_]+", MARKERS.sub(" ", row["comment"]))
        rows.append({"item_uid": row["item_uid"], "auto_tracked": (row["run_id"], row["candidate_id"]) in referenced,
                     "auto_n0": len(words) < 3})
    return pd.DataFrame(rows, columns=["item_uid", "auto_tracked", "auto_n0"])


def add_sheet(workbook, title, columns, rows, dropdowns):
    sheet = workbook.create_sheet(title)
    sheet.append(columns)
    for row in rows:
        sheet.append([clip(row.get(column, "")) for column in columns])
    for column, options in dropdowns.items():
        letter = chr(ord("A") + columns.index(column))
        validation = DataValidation(type="list", formula1='"' + ",".join(map(str, options)) + '"',
                                    allow_blank=True, showErrorMessage=True)
        sheet.add_data_validation(validation)
        validation.add(f"{letter}2:{letter}{max(len(rows) + 1, 2)}")
    sheet.freeze_panes = "A2"


def qp2_row(row):
    return {"item_uid": row["item_uid"], "repo_id": row["repo_id"], "sha": row["sha"],
            "file_path": row["file_path"], "method_name": row["method_name"], "comment": row["comment"],
            "method_source": row["method_source_code"]}


def qp34_row(row, tests_by_label):
    return {"item_uid": row["item_uid"], "repo_id": row["repo_id"], "sha": row["sha"],
            "file_path": row["file_path"], "method_name": row["method_name"], "comment": row["comment"],
            "issue_key": row["task_key"], "issue_summary": row["task_summary"],
            "issue_description": row["task_description"], "method_source": row["method_source_code"],
            "test_A": tests_by_label["A"], "test_B": tests_by_label["B"]}


def main():
    args = parse_args(__doc__, lambda p: p.add_argument(
        "--force", action="store_true", help="overwrite existing workbooks (destroys filled annotations)"))
    cfg = load_config(args)
    annotators = cfg["annotators"]
    out = args.workdir / "annotation"
    targets = [out / f"annotator_{name}.xlsx" for name in annotators]
    if any(path.exists() for path in targets) and not args.force:
        sys.exit("annotation workbooks already exist (possibly filled in); use --force to overwrite them")
    out.mkdir(parents=True, exist_ok=True)

    items, references = load_raw(args.workdir)
    rng = random.Random(cfg["study"]["seed"])
    sample_cfg = cfg["sample"]

    pool34, no_ctx = qp34_pool(items)
    picked34 = stratified_pick(pool34, sample_cfg["qp34_size"], rng)
    warn_if_short("QP3/QP4", picked34, sample_cfg["qp34_size"])
    pool2 = qp2_pool(items)
    picked2 = stratified_pick(pool2, sample_cfg["qp2_size"], rng)
    warn_if_short("QP2", picked2, sample_cfg["qp2_size"])

    assign34, shared34 = assign(picked34, annotators, sample_cfg["overlap"], rng)
    assign2, shared2 = assign(picked2, annotators, sample_cfg["overlap"], rng)

    with_ctx_by_uid = pool34.set_index("item_uid", drop=False)
    mapping_rows, test_labels = [], {}
    for uid in picked34:
        a_is_with_context = rng.random() < 0.5
        conditions = ("with_context", "no_context") if a_is_with_context else ("no_context", "with_context")
        mapping_rows.append({"item_uid": uid, "A": conditions[0], "B": conditions[1]})
        source = {"with_context": with_ctx_by_uid.loc[uid, "test_source"], "no_context": no_ctx.loc[uid, "test_source"]}
        test_labels[uid] = {"A": source[conditions[0]], "B": source[conditions[1]]}

    qp2_by_uid = pool2.set_index("item_uid", drop=False)
    for name, path in zip(annotators, targets):
        workbook = Workbook()
        workbook.remove(workbook.active)
        add_sheet(workbook, "QP2", QP2_COLUMNS, [qp2_row(qp2_by_uid.loc[uid]) for uid in assign2[name]],
                  {"content_level": LEVELS})
        rows34 = [qp34_row(with_ctx_by_uid.loc[uid], test_labels[uid]) for uid in assign34[name]]
        add_sheet(workbook, "QP3_QP4", QP34_COLUMNS, rows34,
                  {"preferred": PREFERENCES, "rubric_A": RUBRIC, "rubric_B": RUBRIC})
        workbook.save(path)

    pd.DataFrame(mapping_rows, columns=["item_uid", "A", "B"]).to_csv(out / "ab_mapping.csv", index=False)
    auto_labels(items, references, set(picked2)).to_csv(out / "auto_labels.csv", index=False)
    write_json(out / "sample_manifest.json", {
        "seed": cfg["study"]["seed"], "annotators": annotators,
        "qp2": {"items": sorted(picked2), "shared": shared2},
        "qp34": {"items": sorted(picked34), "shared": shared34}})
    print(f"QP2: {len(picked2)} items, QP3/QP4: {len(picked34)} items -> {out}")


if __name__ == "__main__":
    main()
