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
- `Training` (`id`, `title`, `required`)
- `TrainingRecord` (`employeeId`, `trainingId`, `completedDate`)

The model is intentionally simple and in-memory only.
No expiration logic, validity windows, or status workflow is included yet.

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
  "required": true
}
```

Complete training payload example:

```json
{
  "completedDate": "2026-10-07"
}
```
