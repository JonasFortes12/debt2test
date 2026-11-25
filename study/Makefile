.PHONY: setup run collect annotate analyze test
setup:
	uv sync
run:
	uv run 1_run.py
collect:
	uv run 2_collect.py
annotate:
	uv run 3_annotation_export.py
analyze:
	uv run 4_analyze.py
test:
	uv run pytest -q
