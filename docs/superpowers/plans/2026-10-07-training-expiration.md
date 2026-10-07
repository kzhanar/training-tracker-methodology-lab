# Training Expiration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add optional training expiration, record status reporting, and an endpoint listing employees with expired required training while preserving existing API behavior.

**Architecture:** Extend the in-memory `Training` model and course-creation flow with an optional validity period in days. Add a clock-driven status service that evaluates records and selects employees from their latest completed record per required course. Keep existing training-history responses unchanged and expose the new information through separate endpoints.

**Tech Stack:** Java 17, Spring Boot 3.3.5, Maven, JUnit 5, Spring Boot Test, MockMvc.

**Spec:** [2026-10-07-training-expiration-design.md](../specs/2026-10-07-training-expiration-design.md)

## Global Constraints

- A supplied validity period must be a positive integer.
- A course with no validity period is `CURRENT`.
- The last valid date is `completedDate + N days`; the record is `EXPIRED` only after that date.
- A record that is not expired is `EXPIRING_SOON` when its last valid date is within 30 calendar days, including the 30th day and the last valid date.
- The existing `completed` field retains its current meaning and calculation; it is not a synonym for the new status.
- Keep `GET /employees/{employeeId}/training` and its response unchanged.
- Missing completion records do not qualify as expired. Optional courses do not qualify.
- A later completed record supersedes an older completion for the same course. Future-dated records do not supersede a completed record.
- Use an injectable `Clock` for date calculations so the boundary behavior is deterministic in tests.
- No persistence or course-update capability is introduced.

## Review Focus

- Absent or explicit `null` validity must omit the new course JSON property; pin this in Task 1's legacy-shape test.
- Non-integer or out-of-range JSON validity values must fail as HTTP 400 rather than silently truncate; pin this in Task 1's validation test.
- Calendar arithmetic across a leap day must still use days, not months; pin this in Task 2's date calculation test.
- Future-dated records must not supersede the latest actual completion in the expired-required query; pin this in Task 2's query test.
- Multiple expired required courses for one employee must not duplicate that employee; pin this in Task 2's selection test.

---

### Task 1: Adding optional course validity

**Files:**
- Modify: `src/main/java/com/example/trainingtrackermethodologylab/model/Training.java`
- Modify: `src/main/java/com/example/trainingtrackermethodologylab/api/CreateTrainingRequest.java`
- Modify: `src/main/java/com/example/trainingtrackermethodologylab/service/TrainingService.java`
- Modify: `src/main/java/com/example/trainingtrackermethodologylab/api/TrainingController.java`
- Test: `src/test/java/com/example/trainingtrackermethodologylab/api/BaselineApiTest.java`

**Interfaces:**
- Consumes: Existing `TrainingService.create(String title, boolean required)` flow and current course JSON contract.
- Produces: `CreateTrainingRequest(String title, boolean required, BigDecimal validityPeriodDays)`; `Training(Long id, String title, boolean required, Integer validityPeriodDays)`; and `TrainingService.create(String title, boolean required, Integer validityPeriodDays)`.

- [ ] **Step 1: Write failing API tests**

Add tests that:

- Create a course with omitted and explicit `null` `validityPeriodDays`; assert both succeed and the create/list JSON does not contain `validityPeriodDays`.
- Create a course with `validityPeriodDays: 90`; assert the returned course exposes `90`.
- Submit `0`, `-1`, a fractional value, and an integer outside the Java `Integer` range; assert each returns HTTP 400.
- Retain the existing create/list course assertions unchanged.

- [ ] **Step 2: Run the focused tests and verify the new assertions fail**

Run: `mvn -Dtest=BaselineApiTest test`
Expected: New positive-validity and invalid-validity assertions fail because the request/model/service do not support the field yet; existing baseline assertions pass.

- [ ] **Step 3: Add the optional validity property and validation**

Add `BigDecimal validityPeriodDays` to `CreateTrainingRequest` so fractional JSON values are not silently truncated, and add `Integer validityPeriodDays` to `Training`. In `TrainingController`, convert a non-null request value with `BigDecimal.intValueExact()` and map its `ArithmeticException` to HTTP 400. Update `TrainingService.create(String title, boolean required, Integer validityPeriodDays)` to reject non-null values below 1 and store valid values. Configure null omission for `Training` JSON so courses without validity retain their previous response shape.

- [ ] **Step 4: Run focused tests and verify they pass**

Run: `mvn -Dtest=BaselineApiTest test`
Expected: PASS, including unchanged baseline course payloads and the new optional-field validation assertions.

- [ ] **Step 5: Commit the course-validity change**

```bash
git add src/main/java/com/example/trainingtrackermethodologylab/model/Training.java src/main/java/com/example/trainingtrackermethodologylab/api/CreateTrainingRequest.java src/main/java/com/example/trainingtrackermethodologylab/service/TrainingService.java src/main/java/com/example/trainingtrackermethodologylab/api/TrainingController.java src/test/java/com/example/trainingtrackermethodologylab/api/BaselineApiTest.java
git commit -m "feat: add optional training validity period"
```

### Task 2: Calculating status and expired employees

**Files:**
- Create: `src/main/java/com/example/trainingtrackermethodologylab/model/TrainingStatus.java`
- Create: `src/main/java/com/example/trainingtrackermethodologylab/model/TrainingRecordStatus.java`
- Create: `src/main/java/com/example/trainingtrackermethodologylab/service/TrainingStatusService.java`
- Modify: `src/main/java/com/example/trainingtrackermethodologylab/TrainingTrackerMethodologyLabApplication.java`
- Test: `src/test/java/com/example/trainingtrackermethodologylab/service/TrainingStatusServiceTest.java`

**Interfaces:**
- Consumes: `TrainingService`, `EmployeeService`, and `TrainingRecordService`; Task 1's `Training.validityPeriodDays()`.
- Produces: `TrainingStatus` enum values `CURRENT`, `EXPIRING_SOON`, and `EXPIRED`; `TrainingRecordStatus(Long employeeId, Long trainingId, LocalDate completedDate, boolean completed, TrainingStatus status, LocalDate expiresOn)`; `TrainingStatusService.listStatusForEmployee(long employeeId)`; and `TrainingStatusService.listEmployeesWithExpiredRequiredTraining()`.

- [ ] **Step 1: Write failing service tests**

In `TrainingStatusServiceTest`, construct the service with fixed clocks and assert:

- A course without validity returns `CURRENT` and `expiresOn == null`.
- `reportsCurrentAt31DaysAndExpiringSoonAt30Days`: for a completion dated `2026-10-07` with validity 31 days and a clock date of `2026-10-07`, status is `CURRENT`; with the clock date `2026-10-08`, status is `EXPIRING_SOON`.
- `reportsExpiringOnLastValidDayAndExpiredTheNextDay`: for that same completion, status is `EXPIRING_SOON` on `2026-11-07` (the last valid date) and `EXPIRED` on `2026-11-08`.
- A future-dated record remains `CURRENT` and has `completed == false`.
- `addsValidityDaysAcrossLeapDay`: a completion date of `2024-02-29` with validity 30 days has `expiresOn == 2024-03-30`.
- The expired-required query excludes missing records and optional courses, includes an expired latest completed required record, does not classify an employee as expired after a newer completed record, and does not let a future record supersede the latest completed record.
- An employee with multiple expired required courses is returned once.

- [ ] **Step 2: Run the focused service test and verify it fails**

Run: `mvn -Dtest=TrainingStatusServiceTest test`
Expected: FAIL because the status types and service do not exist yet.

- [ ] **Step 3: Implement status models and clock-driven service**

Add the enum and record types with the exact names and fields above. Implement `TrainingStatusService` with constructor dependencies `EmployeeService`, `TrainingService`, `TrainingRecordService`, and `Clock`. For each record, return `CURRENT` when its course is non-expiring or its completion date is in the future; otherwise compute `expiresOn = completedDate.plusDays(validityPeriodDays)`, return `EXPIRED` only when today is after `expiresOn`, and return `EXPIRING_SOON` when the record is not expired and the remaining days are at most 30. Compute `completed` as `!completedDate.isAfter(today)`.

For expired-employee selection, inspect records per employee and course; consider only records dated today or earlier and use the latest such completion for each course. Include an employee once if any required course's latest completion is expired. Preserve employee list order.

Expose a Spring `Clock` bean returning `Clock.systemDefaultZone()` from the application configuration.

- [ ] **Step 4: Run focused service tests and verify they pass**

Run: `mvn -Dtest=TrainingStatusServiceTest test`
Expected: PASS for every status boundary and expired-employee selection case above.

- [ ] **Step 5: Commit the status calculation**

```bash
git add src/main/java/com/example/trainingtrackermethodologylab/model/TrainingStatus.java src/main/java/com/example/trainingtrackermethodologylab/model/TrainingRecordStatus.java src/main/java/com/example/trainingtrackermethodologylab/service/TrainingStatusService.java src/main/java/com/example/trainingtrackermethodologylab/TrainingTrackerMethodologyLabApplication.java src/test/java/com/example/trainingtrackermethodologylab/service/TrainingStatusServiceTest.java
git commit -m "feat: calculate training expiration status"
```

### Task 3: Exposing status endpoints and documenting usage

**Files:**
- Create: `src/main/java/com/example/trainingtrackermethodologylab/api/TrainingStatusResponse.java`
- Modify: `src/main/java/com/example/trainingtrackermethodologylab/api/TrainingRecordController.java`
- Modify: `src/main/java/com/example/trainingtrackermethodologylab/api/EmployeeController.java`
- Modify: `src/test/java/com/example/trainingtrackermethodologylab/api/BaselineApiTest.java`
- Modify: `README.md`

**Interfaces:**
- Consumes: `TrainingStatusService.listStatusForEmployee(long)` and `TrainingStatusService.listEmployeesWithExpiredRequiredTraining()`.
- Produces: `GET /employees/{employeeId}/training/status`, returning `TrainingStatusResponse(Long employeeId, Long trainingId, LocalDate completedDate, boolean completed, TrainingStatus status, LocalDate expiresOn)`; and `GET /employees/expired-training`, returning `List<Employee>`.

- [ ] **Step 1: Write failing endpoint and compatibility tests**

Add MockMvc tests asserting:

- The status route returns the six response fields and expected status/expiry for a configured course.
- The status route returns HTTP 404 for an unknown employee.
- The expired-training route returns each employee once when at least one required course has an expired latest completion, and excludes optional courses and employees with no completed record.
- The existing `GET /employees/{employeeId}/training` still exposes its existing fields and does not expose the new `status` or `expiresOn` fields.
- `GET /employees/expired-training` returns HTTP 200 with `[]` when no employee matches.

- [ ] **Step 2: Run focused API tests and verify they fail**

Run: `mvn -Dtest=BaselineApiTest test`
Expected: FAIL because the new routes and response type do not exist.

- [ ] **Step 3: Add response mapping and endpoints**

Add `TrainingStatusResponse` with the exact fields above. Add the nested status mapping to `TrainingRecordController`, map unknown employees to HTTP 404 using the existing controller convention, and do not alter the existing training-history handler or its response type. Add `GET /employees/expired-training` to `EmployeeController`, returning the status service's employee list.

- [ ] **Step 4: Update README endpoint and payload documentation**

Document `validityPeriodDays`, the status endpoint, the expired-employee endpoint, the 30-day threshold, and the fact that the existing history endpoint retains its current response shape.

- [ ] **Step 5: Run focused tests and the complete regression suite**

Run: `mvn -Dtest=BaselineApiTest,TrainingStatusServiceTest test`
Expected: PASS for the changed API and status behavior.

Run: `mvn test`
Expected: PASS for the full project test suite.

- [ ] **Step 6: Commit the API and documentation change**

```bash
git add src/main/java/com/example/trainingtrackermethodologylab/api/TrainingStatusResponse.java src/main/java/com/example/trainingtrackermethodologylab/api/TrainingRecordController.java src/main/java/com/example/trainingtrackermethodologylab/api/EmployeeController.java src/test/java/com/example/trainingtrackermethodologylab/api/BaselineApiTest.java README.md
git commit -m "feat: expose training expiration endpoints"
```
