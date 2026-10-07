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

The model is intentionally simple and in-memory only.
Completion status is derived from the training validity period and current application date.
Courses without a validity period remain non-expiring.

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
- `POST /trainings` - create training course
- `GET /trainings` - list training courses
- `POST /employees/{employeeId}/training/{trainingId}/complete` - assign/complete training for employee
- `GET /employees/{employeeId}/training` - list employee training records
- `GET /employees/expired-training` - list employees with expired required training and the affected courses

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

`validityPeriodDays` is optional and must be a positive integer when provided. Omitted or `null` means the course does not expire. The course creation and listing responses include this field.

Employee training history responses retain the existing `completed` field and add a `status` field for each completion:

- `CURRENT` - the completion has no validity period, is future-dated, or expires more than 30 days from the current application date.
- `EXPIRING_SOON` - the completion expires within the next 30 days.
- `EXPIRED` - the current application date is on or after `completedDate + validityPeriodDays`.

The expired-training endpoint groups results by employee. Its response includes an `employee` object and an `expiredTrainings` array containing each course's `trainingId`, `title`, latest `completedDate`, and `expirationDate`. It considers only required courses and the latest completion for each employee/course pair; courses never completed and courses without a validity period are not listed.

Example response:

```json
[
  {
    "employee": {
      "id": 1,
      "name": "Alex Rivera",
      "email": "alex.rivera@example.com"
    },
    "expiredTrainings": [
      {
        "trainingId": 1,
        "title": "Secure Coding Basics",
        "completedDate": "2025-10-06",
        "expirationDate": "2026-10-06"
      }
    ]
  }
]
```

Complete training payload example:

```json
{
  "completedDate": "2026-10-07"
}
```
