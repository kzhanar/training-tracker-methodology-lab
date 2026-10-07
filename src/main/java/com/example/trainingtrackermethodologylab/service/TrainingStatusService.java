package com.example.trainingtrackermethodologylab.service;

import com.example.trainingtrackermethodologylab.model.Employee;
import com.example.trainingtrackermethodologylab.model.Training;
import com.example.trainingtrackermethodologylab.model.TrainingRecord;
import com.example.trainingtrackermethodologylab.model.TrainingRecordStatus;
import com.example.trainingtrackermethodologylab.model.TrainingStatus;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class TrainingStatusService {
    private static final long EXPIRING_SOON_DAYS = 30;

    private final EmployeeService employeeService;
    private final TrainingService trainingService;
    private final TrainingRecordService trainingRecordService;
    private final Clock clock;

    public TrainingStatusService(
            EmployeeService employeeService,
            TrainingService trainingService,
            TrainingRecordService trainingRecordService,
            Clock clock
    ) {
        this.employeeService = employeeService;
        this.trainingService = trainingService;
        this.trainingRecordService = trainingRecordService;
        this.clock = clock;
    }

    public List<TrainingRecordStatus> listStatusForEmployee(long employeeId) {
        LocalDate today = LocalDate.now(clock);
        return trainingRecordService.listForEmployee(employeeId)
                .stream()
                .map(record -> toStatus(record, trainingService.getRequired(record.trainingId()), today))
                .toList();
    }

    public List<Employee> listEmployeesWithExpiredRequiredTraining() {
        LocalDate today = LocalDate.now(clock);
        List<Employee> expiredEmployees = new ArrayList<>();

        for (Employee employee : employeeService.list()) {
            Map<Long, TrainingRecord> latestCompletions = new HashMap<>();
            for (TrainingRecord record : trainingRecordService.listForEmployee(employee.id())) {
                if (record.completedDate().isAfter(today)) {
                    continue;
                }
                latestCompletions.merge(
                        record.trainingId(),
                        record,
                        (existing, candidate) -> candidate.completedDate().isAfter(existing.completedDate())
                                ? candidate
                                : existing
                );
            }

            boolean hasExpiredRequiredTraining = latestCompletions.values()
                    .stream()
                    .anyMatch(record -> {
                        Training training = trainingService.getRequired(record.trainingId());
                        return training.required()
                                && toStatus(record, training, today).status() == TrainingStatus.EXPIRED;
                    });
            if (hasExpiredRequiredTraining) {
                expiredEmployees.add(employee);
            }
        }

        return List.copyOf(expiredEmployees);
    }

    private TrainingRecordStatus toStatus(TrainingRecord record, Training training, LocalDate today) {
        LocalDate expiresOn = training.validityPeriodDays() == null
                ? null
                : record.completedDate().plusDays(training.validityPeriodDays());
        boolean completed = !record.completedDate().isAfter(today);
        TrainingStatus status = TrainingStatus.CURRENT;

        if (expiresOn != null && completed) {
            if (today.isAfter(expiresOn)) {
                status = TrainingStatus.EXPIRED;
            } else if (ChronoUnit.DAYS.between(today, expiresOn) <= EXPIRING_SOON_DAYS) {
                status = TrainingStatus.EXPIRING_SOON;
            }
        }

        return new TrainingRecordStatus(
                record.employeeId(),
                record.trainingId(),
                record.completedDate(),
                completed,
                status,
                expiresOn
        );
    }
}
