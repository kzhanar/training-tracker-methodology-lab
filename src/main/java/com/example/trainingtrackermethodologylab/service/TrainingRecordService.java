package com.example.trainingtrackermethodologylab.service;

import com.example.trainingtrackermethodologylab.model.TrainingRecord;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class TrainingRecordService {

    private final EmployeeService employeeService;
    private final TrainingService trainingService;
    private final List<TrainingRecord> records = new ArrayList<>();

    public TrainingRecordService(EmployeeService employeeService, TrainingService trainingService) {
        this.employeeService = employeeService;
        this.trainingService = trainingService;
    }

    public TrainingRecord completeTraining(long employeeId, long trainingId, LocalDate completedDate) {
        employeeService.getRequired(employeeId);
        trainingService.getRequired(trainingId);
        if (completedDate == null) {
            throw new IllegalArgumentException("Completed date is required");
        }

        TrainingRecord record = new TrainingRecord(employeeId, trainingId, completedDate);
        records.add(record);
        return record;
    }

    public List<TrainingRecord> listForEmployee(long employeeId) {
        employeeService.getRequired(employeeId);
        return records.stream()
                .filter(record -> record.employeeId().equals(employeeId))
                .sorted(Comparator.comparing(TrainingRecord::completedDate))
                .toList();
    }

    public Set<Long> expiredRequiredTrainingEmployeeIds() {
        LocalDate today = LocalDate.now();
        Map<Long, Map<Long, TrainingRecord>> latestCompletedAttempts = new LinkedHashMap<>();

        for (TrainingRecord record : records) {
            if (record.completedDate().isAfter(today)) {
                continue;
            }
            Map<Long, TrainingRecord> employeeAttempts = latestCompletedAttempts
                    .computeIfAbsent(record.employeeId(), ignored -> new LinkedHashMap<>());
            TrainingRecord previous = employeeAttempts.get(record.trainingId());
            if (previous == null || !record.completedDate().isBefore(previous.completedDate())) {
                employeeAttempts.put(record.trainingId(), record);
            }
        }

        Set<Long> expiredEmployeeIds = new LinkedHashSet<>();
        latestCompletedAttempts.forEach((employeeId, attemptsByTraining) -> {
            boolean hasExpiredRequiredTraining = attemptsByTraining.entrySet()
                    .stream()
                    .anyMatch(entry -> trainingService.getRequired(entry.getKey()).required()
                            && statusOf(entry.getValue(), today).equals("EXPIRED"));
            if (hasExpiredRequiredTraining) {
                expiredEmployeeIds.add(employeeId);
            }
        });
        return expiredEmployeeIds;
    }

    public String statusOf(TrainingRecord record) {
        return statusOf(record, LocalDate.now());
    }

    private String statusOf(TrainingRecord record, LocalDate today) {
        if (record.completedDate().isAfter(today)) {
            return "CURRENT";
        }
        Integer validityPeriodDays = trainingService.getRequired(record.trainingId()).validityPeriodDays();
        if (validityPeriodDays == null) {
            return "CURRENT";
        }
        LocalDate expirationDate = record.completedDate().plusDays(validityPeriodDays);
        if (today.isAfter(expirationDate)) {
            return "EXPIRED";
        }
        return java.time.temporal.ChronoUnit.DAYS.between(today, expirationDate) <= 30
                ? "EXPIRING_SOON"
                : "CURRENT";
    }
}
