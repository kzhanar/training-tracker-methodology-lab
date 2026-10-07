# Training Tracker Baseline Requirements

This document describes the current baseline capabilities of the application.

## Baseline Capabilities

1. **Employee management**
   - Users can create employees with a name and email.
   - Users can list all employees.

2. **Training course management**
   - Users can create training courses with a title and required flag.
   - Users can list all training courses.

3. **Training completion recording**
   - Users can record that an employee completed a specific training course on a given date.

4. **Employee training record visibility**
   - Users can list training records for a specific employee.

5. **In-memory data storage**
   - All application data is stored in memory only.
   - Data is not persisted across application restarts.

6. **Basic validation behavior**
   - Employee name is required.
   - Employee email is required and must be a valid email format.
   - Training title is required.
   - Completion date is required when recording training completion.
   - Invalid input returns clear validation error responses.
