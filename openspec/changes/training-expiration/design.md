# Design

## Context

The application is a Java 17 / Spring Boot REST API with in-memory employee, training, and completion collections. Courses are immutable records; completion records contain a `LocalDate`. The existing completion response includes a `completed` boolean calculated against the application's current date. There is no persistence layer, authentication, or existing main OpenSpec capability.

## Goals / Non-Goals

**Goals:**
- Add optional course validity without changing behavior for courses with no period.
- Calculate status from the current date and completion data rather than retaining a potentially stale status.
- Expose expired required training without changing the meaning of existing API fields.

**Non-Goals:**
- Persisting data across application restarts.
- Rejecting future completion dates or changing their existing acceptance behavior.
- Adding configurable warning windows, time zones, or course-update operations.
- Treating required courses with no completion record as expired.

## Decisions

1. **Represent validity as nullable whole days.** Add `validityPeriodDays` to course creation and course representations. A positive integer keeps the contract consistent with the API's date-only `LocalDate` model and avoids introducing duration parsing. Null represents courses that do not expire; zero and negative values are invalid.

2. **Derive status at read/response time.** Calculate the expiration date as `completedDate + validityPeriodDays`. A completion is `EXPIRED` on or after that date, `EXPIRING_SOON` when its expiration date is in the next 30 days but has not arrived, and otherwise `CURRENT`. A course without a validity period is always `CURRENT`. Future-dated completion records remain accepted and are `CURRENT` until their completion date. The current application date uses the same date basis as the existing `completed` field.

   Derivation avoids storing status that would become stale as time passes. The fixed 30-day window and expiration boundary are specified behavior, not configuration.

3. **Keep history rows distinct from employee-level course status.** Add a status to each completion response, calculated for that completion. For the expired-required-training listing, group by employee and course, select the latest completion date for each employee/course pair, and include a required course only when that latest completion is expired. An earlier expired row therefore does not make a retaken course expired.

4. **Add a grouped endpoint rather than changing an existing route's meaning.** `GET /employees/expired-training` returns one entry per employee, with the employee details and an `expiredTrainings` array containing course ID, title, latest completion date, and expiration date. Return employees in ascending ID order and courses in ascending training ID order for stable results. This endpoint is additive and leaves the existing training-history route intact.

5. **Preserve existing API fields and semantics.** Keep the `completed` field and its existing date-based meaning. Add `validityPeriodDays` to course responses and `status` to completion responses without removing or renaming existing fields. Requests using the existing course payload continue to create courses with no expiration period.

## Risks / Trade-offs

- [Strict clients may reject additional JSON properties even though the API changes are additive] → Keep all existing properties and routes unchanged, document the new fields, and cover legacy payloads and fields with regression tests.
- [The existing `LocalDate.now()` behavior follows the server's default time zone] → Reuse the existing date basis for consistent `completed` and status behavior; do not introduce a separate time-zone policy in this change.
- [In-memory data is lost on restart] → Keep the enhancement within the existing storage model; expiration metadata and status are also in-memory/derived.

## Migration Plan

No data migration is required. Existing courses have a null validity period and continue to behave as non-expiring courses. Rollback is an application-code rollback; all course and completion data is already ephemeral.
