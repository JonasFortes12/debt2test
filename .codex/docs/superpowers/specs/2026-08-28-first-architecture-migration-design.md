# First Architecture Migration Design

**Date:** 2026-08-28

**Status:** Approved design for implementation

**Related architecture:** `architecture.md`

## Purpose

This change migrates the current file-oriented proof of concept into the
ports-and-adapters structure described by `architecture.md`. The existing
workflow must remain usable: clone a Java repository, extract method comments,
classify SATD with the DebtHunter Weka models or the current heuristic fallback,
generate candidate tests with the configured LLM or the current mock behavior,
and write the existing JSON and Markdown artifacts.

The migration must be a real vertical migration. Empty module shells and
JSON-file coupling are not sufficient. The application flow will be owned by
`debt-orchestrator`, while provider-specific work remains in feature modules.

## Decisions

- Use the ports-first vertical migration approach.
- Keep Java 25 as the Maven compiler and runtime baseline.
- Keep the six-module Maven reactor: `debt-core`, `debt-extractor`,
  `debt-classifier`, `debt-context`, `debt-tester`, and `debt-orchestrator`.
- Use `debt-orchestrator` as the only application composition root.
- Keep JSON and Markdown as output artifacts, not module-to-module APIs.
- Preserve the current default workflow and report artifacts.
- Do not add Jira or Trello HTTP clients in this change.
- Provide deterministic context reference extraction and an explicit unmatched
  result so future Jira and Trello providers have a usable boundary.
- Keep the current mock LLM behavior when no API key is configured.

## Module Responsibilities

### `debt-core`

`debt-core` contains only provider-neutral domain types and ports. It has no
dependency on another Debt2Test module, JSON library, Weka, JavaParser, JGit,
HTTP client, dotenv library, or Spring.

The module defines:

- Pipeline request and execution option values.
- Repository workspace and source-location values.
- `SatdCandidate`, including candidate ID, repository-relative file path,
  method name, source line, comment, method source, and source provenance.
- `ClassifiedDebt`, including candidate ID, SATD result, debt type, optional
  confidence, classifier strategy, model version, status, and errors.
- `ExternalTaskSpec`, with provider-neutral provider, key, summary,
  description, acceptance criteria, labels, and URL fields.
- `EnrichedSatdDebt`, with the classified debt, optional external task,
  extracted references, context status, and errors.
- `GeneratedTest`, with candidate ID, source code, framework, provider, model,
  prompt version, generation status, validation status, and errors.
- Pipeline item and run statuses, typed pipeline errors, provenance values,
  stage results, and the aggregate `PipelineResult`.
- The stage ports: `RepositoryWorkspaceProvider`, `SatdExtractor`,
  `DebtClassifier`, `ContextEnricher`, `TestGenerator`, and `ReportSink`.

Lists in domain values are defensively copied. Domain values do not contain
credentials or raw provider payloads.

### `debt-extractor`

The extractor keeps JGit and JavaParser as implementation details. The current
repository cloning behavior becomes an implementation of the repository
workspace boundary, and the current AST traversal becomes an implementation of
`SatdExtractor`.

The extractor returns core candidates rather than its nested local candidate
class. Candidate paths in domain results are relative to the prepared
repository. A temporary absolute path may be kept only as non-reporting
execution metadata. A parse failure is attached to the affected file or stage
result, and traversal continues for other files when possible.

### `debt-classifier`

The classifier depends on `debt-core` only. `WekaDebtHunterClassifier` consumes
core candidates and implements the classifier port. The binary and multi-class
model behavior remains unchanged where possible, including the current
heuristic fallback.

The result records whether the DebtHunter model strategy or heuristic fallback
was used. Model loading, schema incompatibility, and prediction failures are
represented as typed classification errors or item errors instead of being
silently hidden. The classifier does not own pipeline sequencing or report
writing.

### `debt-context`

The context module depends on `debt-core` only. It contains the following
provider-neutral extension boundary:

- A deterministic reference extractor for comments and available source
  metadata.
- A normalized external-reference value.
- A context-provider contract with `supports` and `fetch` behavior.
- An ordered provider chain that stops at a successful match and preserves
  provider failures.
- A no-network implementation used by the first migrated CLI.

When no context provider is configured, extracted references produce an
explicit `CONTEXT_NOT_FOUND` result. A missing reference also produces
`CONTEXT_NOT_FOUND`. The module does not expose Jira-specific or Trello-specific
types in `debt-core`; those concepts will be implemented inside future
adapters.

### `debt-tester`

The tester depends on `debt-core` only. Existing OpenAI, Anthropic, and Gemini
HTTP adapters remain inside this module. Their provider-specific payloads and
authentication stay outside domain objects.

The test generation service implements `TestGenerator` and consumes
`EnrichedSatdDebt` values directly. It no longer reads `output/debt-report.json`
or acts as a pipeline stage coordinator. The no-key path emits the current mock
test source and a mock-success status. Provider failures produce generation
errors for the affected candidate while allowing other candidates to finish.

### `debt-orchestrator`

The orchestrator owns the application service and CLI entry point. It constructs
adapters, injects the stage ports, executes the pipeline, correlates results by
candidate ID, and sends the final result to report sinks.

The former `MainPipeline` responsibility moves here. The former `MainTester`
file-reading workflow is replaced by the single orchestrator flow. The
orchestrator may retain current default values for the repository URL, model
paths, output directory, and provider configuration, while also accepting
optional CLI configuration.

## Pipeline Flow

The application service executes these steps synchronously:

1. Validate the pipeline request and create a run ID.
2. Prepare the repository workspace.
3. Extract candidates from Java source.
4. Classify all extracted candidates.
5. Retain non-SATD classifications in the internal result and select SATD
   classifications for later stages.
6. Enrich selected SATD items through the context boundary.
7. Generate candidate tests for eligible enriched items.
8. Assemble the correlated `PipelineResult`.
9. Write `output/debt-report.json`, `output/debt-test-report.json`, and
   `output/debt-test-report.md` through report adapters.
10. Mark the run `COMPLETED` when no recoverable errors exist, otherwise mark it
    `COMPLETED_WITH_ERRORS`.

The current report fields remain available, including file path, method name,
line number, comment, method source, SATD result, debt type, generated test
source, and generation status. New architecture fields such as IDs, context
status, provenance, and typed errors are added by the report adapter without
making the files internal stage contracts.

## Error Policy

Fatal run failures are invalid configuration, failure to prepare any
repository workspace, or failure to initialize a required stage. They prevent
normal pipeline completion.

Recoverable failures are attached to the relevant item or stage result:

- A malformed Java file causes extraction failure for that file while other
  files continue.
- Classifier model incompatibility or prediction failure is visible in the
  result. If the selected configuration permits fallback, the heuristic result
  explicitly records fallback provenance.
- A missing issue reference or missing provider record is
  `CONTEXT_NOT_FOUND`.
- A context adapter failure is `CONTEXT_FAILED`.
- An LLM request or response parsing failure is `GENERATION_FAILED`.

Recoverable failures never become apparently successful items, and successful
items remain in the final result.

## Security and Provenance

- API keys, authorization headers, and resolved credentials never enter core
  values, reports, or log messages.
- Repository URLs are sanitized before logging or reporting when they contain
  credentials.
- Classifier results record strategy and model provenance, including explicit
  heuristic fallback use.
- Test results record provider, model, prompt version, and generation status.
- External task content and source code are treated as untrusted input to test
  prompts.
- Raw external responses are not retained by default.

## Testing Strategy

All tests run without live Jira, Trello, GitHub, OpenAI, Anthropic, or Gemini
credentials.

- `debt-core`: value validation, status semantics, immutable result values, and
  typed-error behavior.
- `debt-extractor`: Java fixtures for method comments, nested classes,
  overloaded methods, repository-relative paths, and malformed files; fake
  workspace tests cover acquisition boundaries.
- `debt-classifier`: deterministic heuristic tests and provenance/error tests;
  model contract tests use local artifacts or fixtures only.
- `debt-context`: issue-reference extraction, false positives, multiple
  references, provider-chain resolution, unmatched results, and provider
  failures using fakes.
- `debt-tester`: prompt input with and without context, mock generation,
  provider response parsing, and recorded generation failures.
- `debt-orchestrator`: fake-port tests for stage ordering, candidate ID
  correlation, non-SATD filtering, partial failures, run status, and report
  artifact creation.

## Acceptance Criteria

The first migration is complete when:

- The six modules build as a Maven reactor under Java 25.
- `debt-core` has no external or feature-module dependencies.
- Extractor, classifier, context, and tester compile independently against
  core contracts and do not depend directly on one another.
- The orchestrator runs the current workflow from repository acquisition
  through mock test generation without reading an intermediate JSON file.
- The current JSON and Markdown output artifacts are generated with existing
  information preserved and new status/provenance/error data represented.
- Context extraction and provider-chain boundaries can be tested offline, while
  Jira and Trello network adapters remain out of scope.
- A classifier, context, or test-generation item failure results in an explicit
  partial result and `COMPLETED_WITH_ERRORS` rather than silent data loss.
- No credential is written to logs or artifacts.
