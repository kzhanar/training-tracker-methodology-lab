# Spec Delta

## Purpose

Allows training courses to have an optional validity period and lets users identify the current status of completions and required training that has expired.

## ADDED Requirements

### Requirement: Training courses MAY have a validity period
The system SHALL accept an optional positive integer `validityPeriodDays` when creating a training course and SHALL expose that value when listing courses. An omitted or null value means the course does not expire.

#### Scenario: Create a course without a validity period
- **WHEN** a client creates a course without `validityPeriodDays` or sets it to null
- **THEN** the course is created without an expiration period and its listed `validityPeriodDays` is null

#### Scenario: Create a course with a positive validity period
- **WHEN** a client creates a course with a positive `validityPeriodDays`
- **THEN** the course is created and the same validity period is returned by course creation and listing

#### Scenario: Reject a non-positive validity period
- **WHEN** a client creates a course with `validityPeriodDays` equal to zero or less
- **THEN** the API returns HTTP 400 with a clear validation error

### Requirement: Completion status reflects course validity
The system SHALL derive each completion's status as `CURRENT`, `EXPIRING_SOON`, or `EXPIRED` from its completion date, course validity period, and the current application date.

#### Scenario: Course has no validity period
- **WHEN** a completion belongs to a course without `validityPeriodDays`
- **THEN** its status is `CURRENT` indefinitely

#### Scenario: Completion date is in the future
- **WHEN** a completion has a future `completedDate`
- **THEN** its status is `CURRENT` until that date, after which the normal validity-period rules apply

#### Scenario: Completion is current outside the warning window
- **WHEN** a completion has a validity period and its expiration date is more than 30 days after the current application date
- **THEN** its status is `CURRENT`

#### Scenario: Completion is expiring within 30 days
- **WHEN** a completion has a validity period, its expiration date is after the current application date, and that date is no more than 30 days away
- **THEN** its status is `EXPIRING_SOON`

#### Scenario: Completion expires on the current date
- **WHEN** the current application date is the completion date plus `validityPeriodDays`
- **THEN** its status is `EXPIRED`

#### Scenario: Completion expired before the current date
- **WHEN** the current application date is after the completion date plus `validityPeriodDays`
- **THEN** its status is `EXPIRED`

### Requirement: Employee training history reports completion status
The employee training-history API SHALL include each completion's derived status while preserving the existing response fields and completion-history behavior.

#### Scenario: List completion history with status
- **WHEN** a client requests an employee's training history
- **THEN** each returned completion includes its own status derived from that completion date and course validity period
- **AND** each item retains `employeeId`, `trainingId`, `completedDate`, and the existing `completed` field
- **AND** all completion records remain listed in the existing order

#### Scenario: Complete a course with a validity period
- **WHEN** a client records a completion for a course with a validity period
- **THEN** the creation response includes the completion's derived status and retains the existing response fields

### Requirement: Users can list employees with expired required training
The system SHALL provide `GET /employees/expired-training`, returning employees grouped with required courses whose latest completion is expired.

#### Scenario: Employee has expired required training
- **WHEN** an employee's latest completion for a required course has status `EXPIRED`
- **THEN** the endpoint includes that employee once with the course in `expiredTrainings`
- **AND** the entry contains an `employee` object with `id`, `name`, and `email`
- **AND** the `expiredTrainings` item includes `trainingId`, `title`, `completedDate`, and `expirationDate`

#### Scenario: Employee has several expired required courses
- **WHEN** an employee has more than one required course whose latest completion is expired
- **THEN** the endpoint includes one employee entry containing all of those courses

#### Scenario: Latest completion is not expired
- **WHEN** an employee has an earlier expired completion but a later completion for the same course whose status is not `EXPIRED`
- **THEN** that course is not included in the endpoint response

#### Scenario: Required course has never been completed
- **WHEN** an employee has no completion record for a required course
- **THEN** that course is not included in the expired-training response

#### Scenario: Course is not required
- **WHEN** the latest completion for an expired course belongs to a course that is not required
- **THEN** that course is not included in the expired-training response

#### Scenario: Required course does not expire
- **WHEN** the latest completion for a required course belongs to a course without a validity period
- **THEN** that course is not included in the expired-training response

#### Scenario: No employee has expired required training
- **WHEN** no employee has a required course whose latest completion is expired
- **THEN** the endpoint returns HTTP 200 with an empty array

### Requirement: Existing API behavior remains compatible
The system SHALL retain existing routes, request behavior, response fields, and validation behavior when clients do not use the optional validity period.

#### Scenario: Create a course using the existing request shape
- **WHEN** a client creates a course using only the existing `title` and `required` fields
- **THEN** course creation continues to succeed as before and the course has no expiration period

#### Scenario: Preserve existing completion fields and semantics
- **WHEN** a client requests completion history or records a completion
- **THEN** the existing fields remain present and the `completed` boolean retains its previous date-based meaning
