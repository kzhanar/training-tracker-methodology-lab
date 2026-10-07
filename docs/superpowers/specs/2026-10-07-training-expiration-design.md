# Training Expiration Design

## Goal

Add optional expiration tracking to training courses, expose training status, and
allow users to list employees with expired required training without breaking
existing API response contracts.

## Current behavior

- `Training` contains an ID, title, and required flag.
- `TrainingRecord` stores an employee ID, training ID, and completion date.
- Training records are held in memory and repeated completions are retained.
- `GET /employees/{employeeId}/training` returns the existing training-record
  response, including its `completed` boolean.
- A future completion date is represented as `completed: false`.
- The app uses Spring Boot REST controllers and integration tests with MockMvc.

## Data and validation

- Add nullable `validityPeriodDays` to a training course.
- Course creation accepts the new property optionally; omitted or `null` means
  the course does not expire.
- A supplied validity period must be a positive integer. Invalid values return
  HTTP 400 using the existing validation-error conventions.
- Preserve the JSON shape of existing courses when their validity period is
  absent. Courses configured with a validity period expose that value.
- No persistence or course-update capability is introduced.

## Status rules

Status is calculated from a completion record, its course's validity period,
and the current date:

- A course with no validity period is `CURRENT`.
- A future-dated record is `CURRENT` and remains `completed: false`; validity
  has not started.
- For a completed record with validity `N` days, its last valid date is
  `completedDate + N days`.
- The record is `EXPIRED` only after the last valid date; the last valid date
  itself remains valid.
- A record that is not expired is `EXPIRING_SOON` when its last valid date is
  within 30 calendar days, including the 30th day and the last valid date.
- All other records are `CURRENT`.
- `expiresOn` is the last valid date. It is `null` for a course without a
  validity period.
- The existing `completed` field retains its current meaning and calculation;
  it is not a synonym for the new status.
- Use an injectable `Clock` for date calculations so the boundary behavior is
  deterministic in tests.

## API

- Keep `GET /employees/{employeeId}/training` and its response unchanged.
- Add `GET /employees/{employeeId}/training/status`. It returns the employee's
  training records with `employeeId`, `trainingId`, `completedDate`,
  `completed`, `status`, and `expiresOn`.
- Return HTTP 404 from the new status endpoint for an unknown employee,
  following the existing employee-training lookup behavior.
- Add `GET /employees/expired-training`. It returns each matching employee
  once, using the existing employee representation.
- An employee matches when at least one required course has a recorded
  completion on or before the current date and that employee's latest such
  completion for that course is `EXPIRED`.
- A later completed record supersedes an older completion for the same course.
  Future-dated records do not supersede a completed record.
- Missing completion records do not qualify as expired. Optional courses do
  not qualify. No matches returns HTTP 200 with an empty array.
- The existing employee, training, completion, and training-history routes
  retain their existing behavior and response fields.

## Compatibility

Existing clients can continue using all current routes. The training-history
route is not extended; callers that need status use the new status route.
Training courses without a validity period retain their existing JSON shape.
When validity is configured, the course response contains the new non-null
`validityPeriodDays` property; clients must tolerate additive fields as usual
for JSON APIs.

The legacy `completed` property continues to indicate whether the completion
date is today or earlier, even when the training status is `EXPIRED`.

## Error handling

- Invalid validity periods return HTTP 400 with a clear validation message.
- Unknown employees on the status route return HTTP 404.
- The expired-employee route returns an empty array rather than an error when
  no employees match.
- Existing validation and not-found behavior remains unchanged elsewhere.

## Verification

Add focused tests for:

- Omitted, null, positive, zero, and negative validity periods.
- `CURRENT`, `EXPIRING_SOON`, and `EXPIRED` status boundaries, including the
  last-valid-date and 30-day boundaries.
- Non-expiring and future-dated records.
- The new status route response and unknown-employee behavior.
- Expired employee selection across required and optional courses, employees
  with no completion, repeated completions, and deduplicated employee output.
- Existing API payloads and baseline flows remaining compatible.

Run the focused tests first, then the complete Maven test suite to check for
regressions.
