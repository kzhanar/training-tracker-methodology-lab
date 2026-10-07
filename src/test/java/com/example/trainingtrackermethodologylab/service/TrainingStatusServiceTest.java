package com.example.trainingtrackermethodologylab.service;

import com.example.trainingtrackermethodologylab.model.Employee;
import com.example.trainingtrackermethodologylab.model.Training;
import com.example.trainingtrackermethodologylab.model.TrainingRecordStatus;
import com.example.trainingtrackermethodologylab.model.TrainingStatus;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TrainingStatusServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 7);

    @Test
    void nonExpiringCourseIsCurrentWithoutExpirationDate() {
        TestFixture fixture = fixture(TODAY);
        Employee employee = fixture.employeeService().create("Alex", "alex@example.com");
        Training training = fixture.trainingService().create("Orientation", true, null);
        fixture.recordService().completeTraining(employee.id(), training.id(), TODAY.minusDays(1));

        TrainingRecordStatus status = fixture.statusService().listStatusForEmployee(employee.id()).get(0);

        assertEquals(TrainingStatus.CURRENT, status.status());
        assertNull(status.expiresOn());
    }

    @Test
    void reportsCurrentAt31DaysAndExpiringSoonAt30Days() {
        TestFixture fixture = fixture(TODAY);
        Employee employee = fixture.employeeService().create("Alex", "alex@example.com");
        Training training = fixture.trainingService().create("Annual Security", true, 31);
        fixture.recordService().completeTraining(employee.id(), training.id(), TODAY);

        TrainingRecordStatus current = fixture.statusService().listStatusForEmployee(employee.id()).get(0);
        TrainingRecordStatus expiringSoon = fixture.statusService(TODAY.plusDays(1))
                .listStatusForEmployee(employee.id())
                .get(0);

        assertEquals(LocalDate.of(2026, 11, 7), current.expiresOn());
        assertEquals(TrainingStatus.CURRENT, current.status());
        assertEquals(TrainingStatus.EXPIRING_SOON, expiringSoon.status());
    }

    @Test
    void reportsExpiringOnLastValidDayAndExpiredTheNextDay() {
        TestFixture fixture = fixture(TODAY);
        Employee employee = fixture.employeeService().create("Alex", "alex@example.com");
        Training training = fixture.trainingService().create("Annual Security", true, 31);
        fixture.recordService().completeTraining(employee.id(), training.id(), TODAY);

        TrainingRecordStatus lastValidDay = fixture.statusService(LocalDate.of(2026, 11, 7))
                .listStatusForEmployee(employee.id())
                .get(0);
        TrainingRecordStatus dayAfterExpiry = fixture.statusService(LocalDate.of(2026, 11, 8))
                .listStatusForEmployee(employee.id())
                .get(0);

        assertEquals(TrainingStatus.EXPIRING_SOON, lastValidDay.status());
        assertEquals(TrainingStatus.EXPIRED, dayAfterExpiry.status());
    }

    @Test
    void futureDatedRecordIsCurrentAndNotCompleted() {
        TestFixture fixture = fixture(TODAY);
        Employee employee = fixture.employeeService().create("Alex", "alex@example.com");
        Training training = fixture.trainingService().create("Annual Security", true, 30);
        fixture.recordService().completeTraining(employee.id(), training.id(), TODAY.plusDays(1));

        TrainingRecordStatus status = fixture.statusService().listStatusForEmployee(employee.id()).get(0);

        assertEquals(TrainingStatus.CURRENT, status.status());
        assertFalse(status.completed());
    }

    @Test
    void addsValidityDaysAcrossLeapDay() {
        LocalDate leapDay = LocalDate.of(2024, 2, 29);
        TestFixture fixture = fixture(leapDay);
        Employee employee = fixture.employeeService().create("Alex", "alex@example.com");
        Training training = fixture.trainingService().create("Annual Security", true, 30);
        fixture.recordService().completeTraining(employee.id(), training.id(), leapDay);

        TrainingRecordStatus status = fixture.statusService().listStatusForEmployee(employee.id()).get(0);

        assertEquals(LocalDate.of(2024, 3, 30), status.expiresOn());
    }

    @Test
    void expiredRequiredEmployeesUseLatestCompletedRecordAndIgnoreMissingOrOptionalCourses() {
        TestFixture fixture = fixture(TODAY);
        Training required = fixture.trainingService().create("Required Security", true, 30);
        Training optional = fixture.trainingService().create("Optional Security", false, 30);

        Employee expired = fixture.employeeService().create("Expired", "expired@example.com");
        fixture.recordService().completeTraining(expired.id(), required.id(), TODAY.minusDays(40));

        Employee refreshed = fixture.employeeService().create("Refreshed", "refreshed@example.com");
        fixture.recordService().completeTraining(refreshed.id(), required.id(), TODAY.minusDays(40));
        fixture.recordService().completeTraining(refreshed.id(), required.id(), TODAY.minusDays(10));

        Employee futureRetake = fixture.employeeService().create("Future Retake", "future@example.com");
        fixture.recordService().completeTraining(futureRetake.id(), required.id(), TODAY.minusDays(40));
        fixture.recordService().completeTraining(futureRetake.id(), required.id(), TODAY.plusDays(20));

        Employee optionalOnly = fixture.employeeService().create("Optional Only", "optional@example.com");
        fixture.recordService().completeTraining(optionalOnly.id(), optional.id(), TODAY.minusDays(40));

        fixture.employeeService().create("No Completion", "missing@example.com");

        assertEquals(List.of(expired, futureRetake), fixture.statusService()
                .listEmployeesWithExpiredRequiredTraining());
    }

    @Test
    void employeeWithMultipleExpiredRequiredCoursesIsReturnedOnce() {
        TestFixture fixture = fixture(TODAY);
        Employee employee = fixture.employeeService().create("Alex", "alex@example.com");
        Training firstRequired = fixture.trainingService().create("Security", true, 30);
        Training secondRequired = fixture.trainingService().create("Privacy", true, 60);
        fixture.recordService().completeTraining(employee.id(), firstRequired.id(), TODAY.minusDays(40));
        fixture.recordService().completeTraining(employee.id(), secondRequired.id(), TODAY.minusDays(70));

        List<Employee> expiredEmployees = fixture.statusService().listEmployeesWithExpiredRequiredTraining();

        assertEquals(1, expiredEmployees.size());
        assertTrue(expiredEmployees.contains(employee));
    }

    private TestFixture fixture(LocalDate today) {
        EmployeeService employeeService = new EmployeeService();
        TrainingService trainingService = new TrainingService();
        TrainingRecordService recordService = new TrainingRecordService(employeeService, trainingService);
        return new TestFixture(employeeService, trainingService, recordService, today);
    }

    private record TestFixture(
            EmployeeService employeeService,
            TrainingService trainingService,
            TrainingRecordService recordService,
            LocalDate today
    ) {
        TrainingStatusService statusService() {
            return statusService(today);
        }

        TrainingStatusService statusService(LocalDate date) {
            Clock clock = Clock.fixed(date.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
            return new TrainingStatusService(employeeService, trainingService, recordService, clock);
        }
    }
}
