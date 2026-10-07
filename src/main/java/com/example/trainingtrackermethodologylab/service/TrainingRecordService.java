package com.example.trainingtrackermethodologylab.service;

import com.example.trainingtrackermethodologylab.model.TrainingRecord;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

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
}
