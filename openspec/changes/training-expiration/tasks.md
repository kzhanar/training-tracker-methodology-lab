# Tasks

## 1. Course validity period

- [x] 1.1 Add optional `validityPeriodDays` to course creation and course responses; validate positive values and verify omitted/null, positive, zero, and negative cases through focused API tests.
- [x] 1.2 Update the README course API documentation with the optional field and an example; verify the documented request and response match the course API contract.

## 2. Completion status and expired-training listing

- [x] 2.1 Derive per-completion status from the course validity and current application date while preserving `completed`; add API tests for no-expiration, future-dated completion, current, expiring-soon, and expiration-boundary cases.
- [x] 2.2 Implement `GET /employees/expired-training` using the latest completion per employee/course and required courses only; add API tests for grouped output, retakes, missing completions, non-required courses, and empty results.
- [x] 2.3 Document the status field and grouped expired-training endpoint in the README; verify the documented route and response fields against the API.

## 3. Integration verification

- [x] 3.1 Run `mvn test` and verify all existing baseline API tests and new expiration tests pass without changing existing route or `completed` semantics.
