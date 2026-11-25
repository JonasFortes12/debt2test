# Annotation guide

You receive `annotator_<name>.xlsx` with two sheets. Fill only the highlighted columns (dropdowns). Do not
reorder or delete rows. You do **not** see the automatic pre-labels or which test used issue context.

## Sheet QP2: documentation level of the SATD comment (`content_level`)

Judge the comment text only (read the method source for context, not to improve the comment).

| Level | Meaning | Test question |
|---|---|---|
| **N0** | Marker only | Nothing beyond `TODO`/`FIXME`/`HACK`-style words or a few filler words. |
| **N1** | Describes | Says *what* is wrong or missing. |
| **N2** | Explains | Says *why* it is debt (cause, consequence, constraint). |
| **N3** | Guides | Says *what to do* or how to fix it (a next step). |

Rules:
- Pick the **highest** level the comment reaches: a comment that explains and also says what to do is N3.
- Levels are cumulative in content, not in text length: one sentence can be N3.
- "Tracked" (a reference to an issue such as `PROJ-123`) is **not** a level. It is computed by rule and reported
  separately, so ignore issue keys when choosing N0–N3.
- Unsure between two levels: choose the lower one and write the doubt in `notes`.

Illustrative examples (invented, to calibrate; replace them with real corpus examples after step 2):

| Level | Example |
|---|---|
| N0 | `// TODO` |
| N1 | `// TODO: this does not handle null orders` |
| N2 | `// TODO: null orders crash the billing job because the cache returns null on a miss` |
| N3 | `// TODO: null orders crash billing (cache miss returns null); return Optional and handle it in the caller` |

## Sheet QP3_QP4: generated tests (`test_A`, `test_B`)

Two tests were generated for the same SATD. Which one used the issue's description is hidden and the order is random.

* **`rubric_A`, `rubric_B`** (QP4): does the test capture the SATD context? Score each test on its own.
  * `0`: ignores the SATD (generic or unrelated test).
  * `1`: exercises the method but not the problem the SATD describes.
  * `2`: exercises exactly the behaviour the SATD points at.
* **`preferred`** (QP3): `A` or `B`, whichever is the better test for repaying this SATD; `tie` if you cannot choose.
  Judge after scoring both. Do not decide by length or style alone.

The issue columns (`issue_key`, `issue_summary`, `issue_description`) are there to understand the SATD; they are
shown for every item.

## Overlap and disagreement

Some items are given to both annotators (about 20 %) to measure agreement (weighted kappa). Annotate
independently. Disagreements are listed in `results/qp*/disagreements.csv` and are excluded from the
distributions until adjudicated.
