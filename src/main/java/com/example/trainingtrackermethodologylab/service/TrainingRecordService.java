package com.example.trainingtrackermethodologylab.service;

import com.example.trainingtrackermethodologylab.model.TrainingRecord;
import com.example.trainingtrackermethodologylab.model.Training;
import com.example.trainingtrackermethodologylab.model.TrainingStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class TrainingRecordService {

    private static final int EXPIRING_SOON_DAYS = 30;

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

    public TrainingStatus statusFor(TrainingRecord record, LocalDate asOfDate) {
        Training training = trainingService.getRequired(record.trainingId());
        return statusFor(record, training, asOfDate);
    }

    public List<TrainingRecord> listExpiredRequiredCompletions() {
        LocalDate asOfDate = LocalDate.now();
        Map<EmployeeTrainingKey, TrainingRecord> latestByEmployeeAndTraining = new HashMap<>();
        for (TrainingRecord record : records) {
            EmployeeTrainingKey key = new EmployeeTrainingKey(record.employeeId(), record.trainingId());
            latestByEmployeeAndTraining.merge(key, record, (existing, candidate) ->
                    candidate.completedDate().isAfter(existing.completedDate()) ? candidate : existing);
        }

        return latestByEmployeeAndTraining.values().stream()
                .filter(record -> {
                    Training training = trainingService.getRequired(record.trainingId());
                    return training.required()
                            && training.validityPeriodDays() != null
                            && statusFor(record, training, asOfDate) == TrainingStatus.EXPIRED;
                })
                .sorted(Comparator.comparing(TrainingRecord::employeeId)
                        .thenComparing(TrainingRecord::trainingId))
                .toList();
    }

    private TrainingStatus statusFor(TrainingRecord record, Training training, LocalDate asOfDate) {
        if (record.completedDate().isAfter(asOfDate) || training.validityPeriodDays() == null) {
            return TrainingStatus.CURRENT;
        }

        LocalDate expirationDate = record.completedDate().plusDays(training.validityPeriodDays());
        if (!asOfDate.isBefore(expirationDate)) {
            return TrainingStatus.EXPIRED;
        }
        if (!expirationDate.isAfter(asOfDate.plusDays(EXPIRING_SOON_DAYS))) {
            return TrainingStatus.EXPIRING_SOON;
        }
        return TrainingStatus.CURRENT;
    }

    private record EmployeeTrainingKey(Long employeeId, Long trainingId) {
    }
}
