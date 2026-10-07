# training-tracker-methodology-lab

Minimal Java 17 Spring Boot Maven project used as a small brownfield baseline for future methodology comparisons.

## Scope

- Java 17
- Spring Boot REST API
- Maven
- JUnit 5 + Spring Boot Test
- In-memory storage only
- No database
- No authentication

This project intentionally keeps architecture and dependencies minimal.

## Baseline Domain Model

- `Employee` (`id`, `name`, `email`)
- `Training` (`id`, `title`, `required`, optional `validityPeriodDays`)
- `TrainingRecord` (`employeeId`, `trainingId`, `completedDate`)

The model is intentionally simple and in-memory only. Training status is derived from the completion date and course validity period; it is not persisted.

## Project Structure

```text
src/
  main/
    java/com/example/trainingtrackermethodologylab/
      TrainingTrackerMethodologyLabApplication.java
      api/
      model/
      service/
    resources/
      application.properties
  test/
    java/com/example/trainingtrackermethodologylab/
```

## Running

```bash
mvn spring-boot:run
```

## Testing

```bash
mvn test
```

## API Endpoints

- `POST /employees` - create employee
- `GET /employees` - list employees
- `GET /employees/expired-required-training` - list employees with an expired latest completed attempt for at least one required course
- `POST /trainings` - create training course
- `GET /trainings` - list training courses
- `POST /employees/{employeeId}/training/{trainingId}/complete` - assign/complete training for employee
- `GET /employees/{employeeId}/training` - list employee training records

Create employee payload example:

```json
{
  "name": "Alex Rivera",
  "email": "alex.rivera@example.com"
}
```

Create training payload example:

```json
{
  "title": "Secure Coding Basics",
  "required": true,
  "validityPeriodDays": 365
}
```

`validityPeriodDays` is optional and, when provided, must be a positive integer. Courses without a validity period remain current and omit that property from their JSON response. For valid courses, a completion remains current through its expiration date (completion date plus validity days); it is `EXPIRING_SOON` with 0–30 days remaining, inclusive, and `EXPIRED` starting the following day. Future-dated completions remain `CURRENT` until their completion date. Employee training records include this derived `status` alongside the existing `completed` field.

The expired-required-training endpoint returns an employee once if the latest completed attempt for any required course is expired. Future-dated attempts and required courses with no recorded completion do not qualify; older expired attempts are superseded by a later completed retake.

Complete training payload example:

```json
{
  "completedDate": "2026-10-07"
}
```
