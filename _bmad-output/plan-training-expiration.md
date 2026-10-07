---
title: 'Training expiration'
type: 'feature'
ticket: ''
created: '2026-10-07'
status: 'built'
baseline_revision: '29da51ed02f7b3a6e3e5d3c11ff093c64dfdeb91'
route: 'full'
route_source: 'auto'
risk: 'high'
review: ''
review_source: ''
lenses_ran: []
review_loop_iteration: 0
context:
  - 'requirements.md'
  - 'experiment/change-request.md'
  - 'experiment/experiment-plan.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** Some training courses expire and must be retaken, but the application cannot represent validity or identify training status and employees with expired required training.

**Approach:** Add optional course validity, identify training as CURRENT, EXPIRING_SOON, or EXPIRED, and provide a way to list employees whose required training has expired while preserving existing API behavior.

**Always:** Keep storage in memory; preserve existing training/completion routes and behavior for courses without validity; retain the existing `completed` response field and existing request forms.

**Never:** Add persistence, authentication, or unrelated course/employee management changes.

**Decision:** Represent optional validity as a positive integer number of days in `validityPeriodDays`.
**Decision:** Use a fixed 30-day window for EXPIRING_SOON.
**Decision:** Treat training as current through its expiration date; EXPIRED begins the following day.
**Decision:** EXPIRING_SOON includes 0 through 30 days remaining, inclusive.
**Decision:** A record's status describes that individual historical completion attempt, calculated from its completion date.
**Decision:** Courses without a validity period have status CURRENT.
**Decision:** The expired-employee listing excludes employees with no recorded completion for a required course.
**Decision:** For the employee listing, evaluate only the latest completed attempt for each required course; an older expired attempt does not make an employee overdue after a current retake.
**Decision:** Preserve acceptance of future completion dates and the existing `completed: false` behavior; a future-dated record reports CURRENT until its completion date, then follows normal status rules. It does not affect the expired-employee listing until that date.
**Decision:** Add `status` to existing employee training-record responses while retaining all existing response properties, including `completed`.
**Decision:** Preserve the existing JSON shape of courses without validity by omitting their null `validityPeriodDays` property.

</frozen-after-approval>

## Code Map

- `src/main/java/com/example/trainingtrackermethodologylab/model/Training.java` -- immutable course record; serialize validity only when set to preserve baseline course JSON.
- `src/main/java/com/example/trainingtrackermethodologylab/api/CreateTrainingRequest.java` -- current POST `/trainings` input; validity must remain optional for existing clients.
- `src/main/java/com/example/trainingtrackermethodologylab/service/TrainingService.java` -- validates and stores courses in insertion-ordered in-memory map; preserve IDs, ordering, and title validation.
- `src/main/java/com/example/trainingtrackermethodologylab/model/TrainingRecord.java` -- completion facts are employee, training, and `LocalDate`; no persistence or status field.
- `src/main/java/com/example/trainingtrackermethodologylab/service/TrainingRecordService.java` -- stores completion attempts in memory and lists all employee records sorted by completion date; has access to employee and training services.
- `src/main/java/com/example/trainingtrackermethodologylab/api/TrainingRecordController.java` -- routes `/employees/{employeeId}/training`; maps records to response and computes legacy `completed` using `LocalDate.now()`.
- `src/main/java/com/example/trainingtrackermethodologylab/api/TrainingRecordResponse.java` -- current record JSON includes employeeId, trainingId, completedDate, and completed.
- `src/main/java/com/example/trainingtrackermethodologylab/service/EmployeeService.java` and `src/main/java/com/example/trainingtrackermethodologylab/api/EmployeeController.java` -- employee lookup/listing and existing employee REST routes; reuse for overdue-employee results.
- `src/test/java/com/example/trainingtrackermethodologylab/api/BaselineApiTest.java` -- MockMvc integration coverage for existing JSON contracts, validation, and 404s; extend for additive behavior and regressions.
- `pom.xml` -- Spring Boot 3.3.5, Java 17, Maven; JUnit 5 and Spring Boot test dependencies already present.
- `README.md` -- describes intentionally minimal in-memory REST architecture and current absence of expiration logic.

## Story

As a training administrator, I want course validity and clear expiration status so that I can identify employees whose required training is expired and arrange retakes.

## Tasks & Acceptance

**Execution:**
- [x] `src/main/java/com/example/trainingtrackermethodologylab/model/Training.java`, `src/main/java/com/example/trainingtrackermethodologylab/api/CreateTrainingRequest.java`, `src/main/java/com/example/trainingtrackermethodologylab/service/TrainingService.java` -- represent and validate the optional validity period while keeping omitted validity valid.
- [x] `src/main/java/com/example/trainingtrackermethodologylab/api/TrainingRecordResponse.java`, `src/main/java/com/example/trainingtrackermethodologylab/service/TrainingRecordService.java`, `src/main/java/com/example/trainingtrackermethodologylab/api/TrainingRecordController.java` -- expose status according to the approved calculation and preserve legacy completion behavior.
- [x] `src/main/java/com/example/trainingtrackermethodologylab/service/EmployeeService.java`, `src/main/java/com/example/trainingtrackermethodologylab/api/EmployeeController.java` -- add `GET /employees/expired-required-training`, returning each qualifying employee once.
- [x] `src/test/java/com/example/trainingtrackermethodologylab/api/BaselineApiTest.java` -- cover status boundaries, validity validation, overdue inclusion/exclusion, future completions, and unchanged legacy request/response behavior.
- [x] `README.md` -- document the optional validity input, status semantics, and listing route after decisions are resolved.

**Acceptance Criteria:**
- Given a client omits validity when creating a course, when it uses existing create/list and completion routes, then requests continue to succeed, course JSON does not gain a null validity field, and existing training-record response fields and `completed` behavior remain unchanged.
- Given a client creates a course with positive `validityPeriodDays`, when it lists courses, then the course exposes the same validity period in days.
- Given an employee training record has a course with positive validity, when its response is retrieved, then `status` is CURRENT when more than 30 days remain, EXPIRING_SOON when 0 through 30 days remain inclusive, and EXPIRED only after its expiration date.
- Given a course validity is N days and an attempt was completed on date D, when its expiration date is evaluated, then the expiration date is D plus N calendar days and remains current through that date.
- Given an employee has a future-dated completion, when its record is retrieved, then the existing `completed` value remains false and status is CURRENT until the completion date; it does not affect the expired-employee listing before that date.
- Given an employee has no recorded completion for a required course, when the expired-employee listing is requested, then that missing record alone does not include the employee.
- Given an employee retakes a required course, when the expired-employee listing is requested, then the latest completed attempt determines whether that course is expired; an older expired attempt does not list an employee with a current latest attempt.
- Given the latest completed attempts for an employee show at least one required course as EXPIRED, when the expired-employee listing is requested, then that employee appears exactly once; employees with only non-required expired courses do not appear.
- Given validity is zero or negative, when a course is created, then the API returns a clear 400 response and does not store a course; a positive `validityPeriodDays` value is accepted.

## Traceability

| Requirement | Story / acceptance criteria | Planned test evidence |
|---|---|---|
| Change request: optional course validity; existing courses without it continue working | Story; AC1, AC2, AC9 | Existing `createAndListTrainingCourses`; new `validityPeriodIsOptionalAndValidated` integration test |
| Change request: CURRENT, EXPIRING_SOON, EXPIRED | AC3, AC4, AC5 | New `trainingStatusUsesApprovedBoundaries` integration test; future-date assertion |
| Change request: list employees whose required training expired | AC6, AC7, AC8 | New `expiredRequiredTrainingEmployeesUseLatestRequiredAttempts` integration test |
| Change request: existing API behavior backward compatible | Frozen boundaries; AC1, AC5 | Existing course create/list and `completeTrainingAndListEmployeeRecords` regression tests |
| Baseline: in-memory storage and existing validation | Frozen boundaries; AC1, AC9 | Focused and full Maven test suites |

## Design Notes

Keep expiration status derived from the stored completion date and course validity; do not persist a second status that could become stale. Calculate each historical attempt's response status independently. For the employee listing, use only each course's latest completed attempt (completion date on or before today; for same-date attempts, use the most recently recorded attempt), require the course to be required, and include the employee once if any such attempt is expired. Return employee objects; no new database or status workflow is needed.

## Implementation Notes

Implemented optional positive course validity, derived record statuses, and an in-memory listing of employees with expired latest completed required training. The reviewed code follows the approved date and inclusion boundaries. No independent review lens was run.

## Verification

**Commands:**
- `mvn -Dtest=BaselineApiTest test` -- focused API contract, status, and expiration-list tests pass.
- `mvn test` -- all regression and context tests pass.

**Results:** Both commands passed; `BaselineApiTest` reports 11 tests, 0 failures, 0 errors, and 0 skipped.
