# Training Expiration Methodology Comparison

## Evidence labels and scope

- **Measured** means verified from Git refs, diffs, tracked files, or command output.
- **Developer-recorded** means stated in the methodology artifacts or experiment records; it is not treated as independently verified behavior.
- **Missing** means the repository did not record or provide the evidence.
- **Evaluator interpretation** is a judgment based on the available evidence and is not a developer-reported result.

This comparison follows the dimensions, scale, quantitative fields, and report structure in [`.github/agents/methodology-evaluator.agent.md`](../.github/agents/methodology-evaluator.agent.md). The authoritative request is in [`experiment/change-request.md`](./change-request.md), with [`requirements.md`](../requirements.md) also treated as authoritative.

**Measured branch identity:** BMAD `fefe0146b4e64c02cb0713563c844b86f3228e9b`; OpenSpec `75600d6dd7f794e699e52a87f8230df5539ec757`; Superpowers `8cb8130291aece2900e5470727826c4df81b186a`. `git merge-base` confirmed that all three have the same baseline, `2944d10e45d21162d7563ab2120072a1fe7d0c97` (`main`). The branches were inspected by ref; none was checked out or modified for this comparison.

## 1. Executive Summary

All three branches implement the core request: optional course validity, derived training status, and a way to find employees with expired required training. Each methodology also documents choices for behavior the request leaves open. The choices are not identical, especially on whether the expiration date itself is expired.

An independent, HTTP-only acceptance suite is now available in [`experiment/acceptance/`](./acceptance/). It passed 9/9 cases against BMAD and Superpowers and 8/9 against OpenSpec. OpenSpec's only failure is the exact-expiration boundary: under the user-selected rule (valid through that date), it returned `EXPIRED` rather than `EXPIRING_SOON`. Each branch's own Maven suite also passed in isolated branch snapshots: BMAD 12/12, OpenSpec 13/13, Superpowers 23/23. These are measured test outcomes; they were not recorded in the original experiment results table.

The shared baseline and independent acceptance run are verified. [`experiment/results.md`](./results.md) and [`experiment/observations.md`](./observations.md) remain unfilled, so time, token, rework, and developer-experience comparisons are still unavailable. Static comparison favors Superpowers for testability and separation of status logic, OpenSpec for the smallest total Git diff, and BMAD for explicit acceptance-criteria traceability. No universal winner is inferred.

## 2. Comparison Table

Scale: 1 = poor, 2 = weak, 3 = adequate, 4 = strong, 5 = excellent. “Insufficient evidence” is used where the rubric cannot be scored from available evidence.

| Evaluation dimension | BMAD (`bmad`) | OpenSpec (`openspec`) | Superpowers (`superpowers`) |
|---|---|---|---|
| Requirement understanding | **4** — Plan captures optional validity, statuses, expired-required listing, and compatibility. | **4** — Proposal/spec cover the requested behaviors and API compatibility. | **4** — Design and plan cover the requested behaviors and existing history behavior. |
| Ambiguities discovered | **4** — Plan records explicit choices for units, the 30-day window, expiration boundary, future completions, and missing records. | **4** — Spec/design make explicit choices for these behaviors, including a different expiration boundary. | **4** — Design makes explicit choices for units, warning window, date boundaries, and future completions. |
| Planning quality | **4** — Detailed task plan, acceptance criteria, and requirement-to-test mapping are recorded. | **4** — Structured proposal, design, spec delta, and task list are present. | **4** — Separate design and sequenced implementation plan are present. |
| Brownfield code understanding | **4** — Integrates status with existing record/history behavior and retains legacy course JSON by omitting null validity. | **3** — Extends existing APIs and preserves existing fields, but a null validity property is serialized on legacy course responses. | **4** — Retains history behavior and isolates derived status behind a service while adding a status route. |
| Architecture/change quality | **3** — Compact implementation, but status computation is coupled to `TrainingRecordService` and uses the system date directly. | **3** — Reuses the existing record service and provides a grouped expired-training response; date access is not injectable. | **4** — Dedicated `TrainingStatusService`, typed status, and injectable `Clock` make the rule logic independently testable. |
| Backward compatibility | **4** — Existing completion fields remain; `Training` uses `NON_NULL` so courses without validity retain their prior JSON shape. | **3** — Existing request shape and completion fields are retained, but the new nullable course property changes the serialized response shape. | **4** — Existing completion fields remain and `NON_NULL` preserves the prior JSON shape for courses without validity. |
| Testing quality | **4** — Three feature tests cover validity, status boundaries/future dates, and expired-required listing. | **3** — Four focused API tests cover core status, validation, and listing scenarios, but use the current date and do not inject a clock. | **4** — Fourteen added tests include API coverage and fixed-clock service tests for date edges and employee selection. |
| Regression protection | **4** — Baseline behavior and independent legacy API cases pass; Maven suite passed 12/12. | **3** — Legacy API cases and Maven suite pass, but the selected exact-boundary acceptance case fails. | **4** — Baseline behavior and independent legacy API cases pass; Maven suite passed 23/23. |
| Verification quality | **4** — Independent HTTP suite passed 9/9; branch Maven suite passed 12/12. | **2** — Branch Maven suite passed 13/13, but independent suite passed 8/9 and exposed the exact-boundary mismatch. | **4** — Independent HTTP suite passed 9/9; branch Maven suite passed 23/23. |
| Change surface area | **1** — 475 changed files overall, including 435 `.agents/skills` files and 21 BMAD scripts; nine production source files changed. | **4** — 18 changed files overall, including five OpenSpec change artifacts and 11 production source files. | **2** — 111 changed files overall, including 74 `.agents/skills` files and 19 tracked compiled classes; 11 production source files changed. |
| Rework | **Insufficient evidence** — No rework measure is recorded. | **Insufficient evidence** — Git history shows setup followed by a revert, but this alone does not establish a rework-cycle count. | **Insufficient evidence** — No rework measure is recorded. |
| Token/process efficiency | **Insufficient evidence** — Timing, token, and interaction data are absent. | **Insufficient evidence** — Timing, token, and interaction data are absent. | **Insufficient evidence** — Timing, token, and interaction data are absent. |
| Traceability | **5** — Plan explicitly maps request elements and acceptance criteria to implementation/tests. | **4** — Proposal/spec/tasks are connected, though no comparable requirement-to-test matrix is recorded. | **3** — Design and plan are available, but no explicit requirement-to-test mapping table is recorded. |
| Developer experience | **Insufficient evidence** — No developer observations or feedback are recorded. | **Insufficient evidence** — No developer observations or feedback are recorded. | **Insufficient evidence** — No developer observations or feedback are recorded. |

**Evaluator interpretation:** Scores describe the quality of visible plans, implementation structure, tests, and change footprint. The verification scores incorporate both branch-authored Maven test runs and the independent suite. Test count alone is not treated as quality evidence.

## 3. Quantitative Metrics

The experiment’s quantitative fields were blank in [`experiment/results.md`](./results.md). The table distinguishes measured Git facts from unreported process metrics. Production line counts below cover `src/main` only; test counts are net additions in `@Test` methods relative to the common baseline.

| Metric | BMAD | OpenSpec | Superpowers |
|---|---:|---:|---:|
| Setup time | Not reported | Not reported | Not reported |
| Planning time | Not reported | Not reported | Not reported |
| Time before first code change | Not reported | Not reported | Not reported |
| Implementation time | Not reported | Not reported | Not reported |
| Total elapsed time | Not reported | Not reported | Not reported |
| Approximate input token usage | Not reported | Not reported | Not reported |
| Approximate output token usage | Not reported | Not reported | Not reported |
| Number of AI interactions | Not reported | Not reported | Not reported |
| Methodology/task documents added | 1 plan | 5 OpenSpec change files | 2 design/plan documents |
| Total files changed from baseline | 475 | 18 | 111 |
| Production files changed (`src/main`) | 9 | 11 | 11 |
| Production lines added / deleted | 95 / 7 | 138 / 9 | 216 / 7 |
| Total Git diff lines added / deleted | 51,828 / 18 | 560 / 12 | 12,065 / 11 |
| Dependencies added (`pom.xml`) | 0 | 0 | 0 |
| Tests added (net `@Test` methods) | 3 | 4 | 14 |
| Branch Maven tests passed / total | 12 / 12 | 13 / 13 | 23 / 23 |
| Independent acceptance tests passed / total | 9 / 9 | 8 / 9 | 9 / 9 |
| Independent acceptance tests failed | 0 | 1 (exact-expiration boundary) | 0 |
| Rework cycles | Not reported | Not reported | Not reported |
| Developer-recorded review findings | Not reported | Not reported | Not reported |

**Measured scope notes:** BMAD’s total includes 435 `.agents/skills` files and 21 `_bmad/scripts` files. OpenSpec’s total includes five files under `openspec/changes/training-expiration/`. Superpowers’ total includes 74 `.agents/skills` files and 19 tracked `target/` class files (17 under `target/classes` and two under `target/test-classes`). These setup/build files are included in the Git diff total but are not application production source.

**Verification sources:** Branch Maven totals come from Surefire reports produced in temporary source snapshots exported from each branch tip. The independent totals come from [`experiment/acceptance/test_training_expiration_acceptance.py`](./acceptance/test_training_expiration_acceptance.py). No methodology branch ref or tracked file was changed to run either suite.

**Artifact details:** BMAD’s task plan is [`_bmad-output/plan-training-expiration.md`](../_bmad-output/plan-training-expiration.md); OpenSpec has a proposal, design, spec delta, task list, and `.openspec.yaml`; Superpowers has a design and plan under [`docs/superpowers/`](../docs/superpowers/). Commit counts from baseline are 2, 3, and 6 respectively; commit counts and timestamps are not treated as interaction or elapsed-time measurements.

## 4. Requirement/Ambiguity Findings

**Developer-recorded:** All three methodologies translate the high-level request into a positive whole-day `validityPeriodDays`, status derivation, and expired-required-training selection. Each records a 30-day warning window. BMAD and Superpowers specify that the course remains valid through its expiration date and becomes expired the following day. OpenSpec specifies that it becomes expired on the expiration date itself. The original change request does not define that boundary, so this is a difference in resolved ambiguity, not by itself a correctness failure.

The request also leaves unclear how future-dated completion records and repeated completions affect the expired-employee list. BMAD and Superpowers explicitly exclude future-dated records when choosing the latest completed attempt. OpenSpec defines selection as the latest completion date; its implementation chooses the latest dated record, including a future-dated one, whose status is `CURRENT`. This is consistent with its recorded choice but differs from the other branches. The independent suite verifies that a future-only completion does not qualify an employee, but does not test a future-dated retake following an expired completion; that interaction remains unresolved.

**Missing:** [`experiment/observations.md`](./observations.md) has no methodology-specific ambiguity or decision notes, and [`experiment/results.md`](./results.md) does not record how many ambiguities each developer identified. The choices above come from the methodology artifacts, not contemporaneous experiment observations.

## 5. Code and Architecture Findings

- **BMAD:** Adds nullable validity with `NON_NULL` serialization, derives a string status in `TrainingRecordService`, and exposes `GET /employees/expired-required-training`. Its expired-employee selection filters future-dated records before selecting the latest completed record. The implementation uses `LocalDate.now()` directly, so deterministic clock control is not built into the service.
- **OpenSpec:** Adds a `TrainingStatus` enum and derives status in `TrainingRecordService`; its grouped response is exposed at `GET /employees/expired-training`. The status rule treats the expiration date itself as `EXPIRED`, matching its spec. The `Training` record does not use `NON_NULL`, and no serialization configuration was found in `src/main`; consequently a course without validity serializes the new property as `null`. That is a visible response-shape change for clients that require an exact legacy schema, although the request and existing completion fields remain supported.
- **Superpowers:** Adds a dedicated `TrainingStatusService`, typed status/response models, and `GET /employees/{employeeId}/training/status`; it also exposes expired employees at `GET /employees/expired-training`. The injected `Clock` supports deterministic status tests. Its expiration boundary matches its documented behavior and tests.

**Evaluator interpretation:** The endpoint paths and response shapes differ, but the request did not prescribe a route or response schema for the new feature. Those differences are not treated as failures on their own. The OpenSpec `null` property is a compatibility risk for strict consumers, rather than proof that ordinary existing clients break. The branch-level source changes make no dependency edits.

## 6. Testing and Verification Findings

**Independent test suite:** [`experiment/acceptance/`](./acceptance/) contains nine HTTP-level cases. The same suite was run against isolated snapshots of the three branch tips using a profile only to map each methodology's documented feature routes/response envelope. Expected behavior is common across profiles. The suite derives expected dates from one local date captured at startup; on the runs below that date was 2026-10-07. Each application and the suite ran on the same host and local date.

| Branch | Independent suite | Result |
|---|---:|---|
| BMAD | 9 cases | 9 passed, 0 failed |
| OpenSpec | 9 cases | 8 passed, 1 failed: exact expiration date returned `EXPIRED`, expected `EXPIRING_SOON` under the user-selected rule |
| Superpowers | 9 cases | 9 passed, 0 failed |

Cases covering legacy non-expiring training and API fields, expiring-course creation, current/warning/expired statuses, never-completed required training, expired-required selection and deduplication, invalid validity, unknown IDs, and future completion behavior passed on all branches. The precise boundary case is the only independent failure.

**Branch-authored tests:** Maven/Surefire tests passed on all three isolated snapshots: BMAD 12/12, OpenSpec 13/13, Superpowers 23/23. The common baseline had nine test methods. These authored tests are supplementary; the HTTP acceptance suite is the independent comparison.

**Determinism limitation:** The suite captures `date.today()` once for all cases in a run and derives completion dates relative to it, avoiding repeated reads of the clock and allowing all three branches to be tested on the same day. It does not inject a clock into the application. For replay on a different date, start both app and suite with the same system date/time zone, as described in [`experiment/acceptance/README.md`](./acceptance/README.md).

## 7. Efficiency and Token Findings

Setup/planning/implementation durations, elapsed time, token usage, and AI interaction counts are absent from the experiment records. No efficiency ranking is justified. The source-level and total Git diff sizes are measurable, but they are change-surface indicators—not proxies for time, token use, or developer effort. In particular, methodology setup files account for most of the BMAD and a substantial portion of the Superpowers branch diff.

## 8. Strengths by Methodology

- **BMAD:** Most explicit acceptance-criteria traceability in the inspected plan; it maps the request to implementation and test work. Its model also omits null validity fields, preserving the prior course JSON shape.
- **OpenSpec:** Smallest total branch diff and a complete proposal/design/spec/tasks artifact set. Its API tests are separated from the unchanged baseline test file.
- **Superpowers:** Strongest visible testability structure: a dedicated status service accepts a `Clock`, and focused unit tests cover expiration boundaries, future dates, and latest completed required training. It also preserves the prior course JSON shape.

These are evidence-backed characteristics only; they do not establish overall superiority.

## 9. Weaknesses / Overhead by Methodology

- **BMAD:** Its branch includes a large methodology setup footprint (435 skill files and 21 scripts) relative to nine production files changed. The status service’s direct use of the system date makes it less controllable than Superpowers’ clock-injected service.
- **OpenSpec:** Serializes `validityPeriodDays: null` for courses without validity, changing the old course response shape. Its status logic uses the current date directly, and its feature tests do not provide the same fixed-clock isolation as Superpowers.
- **Superpowers:** The branch includes 74 methodology skill files and 19 compiled `target/` artifacts, increasing the diff without adding runtime source behavior.

No developer-recorded overhead or review findings are available. The footprint observations above are measured; whether each setup file was intentionally required by the experiment is not established by the results record.

## 10. Best-Fit Scenarios

The evidence suggests dimension-specific fits only: BMAD’s visible plan is strongest when explicit requirement-to-test traceability is important; OpenSpec is the smallest branch diff and provides a structured spec change; Superpowers is strongest where deterministic status-rule testing and service-level isolation matter. The independent suite supplies additional observable results, but process and developer-experience measurements remain unavailable, so it does not support recommending one method for this change overall.

## 11. Overall Conclusion

The shared baseline control is verified by Git. The same independent HTTP suite was executed against all three branch snapshots, and each snapshot's Maven tests passed. Under the exact-expiration rule selected for this evaluation, OpenSpec has one observable mismatch: it returns `EXPIRED` on the expiration date, whereas BMAD and Superpowers return `EXPIRING_SOON`.

This supports a specific correctness distinction on the selected boundary and dimension-specific conclusions about artifacts, code structure, test design, and branch footprint. It does not establish a universal winner: timing/token/rework/developer-observation records remain absent, and the acceptance suite's date is captured from the local clock rather than injected into each application.
