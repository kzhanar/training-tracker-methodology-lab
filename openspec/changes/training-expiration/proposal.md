# Proposal

## Why

Some training courses must be repeated periodically, but the tracker currently records completions without indicating whether they remain valid. Employees and administrators therefore cannot identify upcoming or overdue required training.

## What Changes

- Add an optional positive `validityPeriodDays` to training courses. Omitted or null means no expiration.
- Derive `CURRENT`, `EXPIRING_SOON`, or `EXPIRED` for each completion using its course validity period and the current application date. The expiring-soon window is a fixed 30 days.
- Add each completion's status to the existing employee training-history response while preserving existing fields, including `completed`.
- Add `GET /employees/expired-training`, grouped by employee, to list required courses whose latest completion is expired.
- Preserve existing behavior for courses without a validity period and for clients that omit the new request field.
- Continue accepting future completion dates; report their status as `CURRENT` until the completion date.

## Capabilities

### New Capabilities
- `training-expiration`: Optional course validity, completion status, and listing employees with expired required training.

### Modified Capabilities
- None. No existing main specs are defined in this repository.

## Impact

The change affects the training creation/list API, the employee training-history response, and a new employee listing endpoint. It requires changes to the in-memory training and training-record services and their API models/controllers, plus API tests and README documentation. It does not require a database or new dependencies.