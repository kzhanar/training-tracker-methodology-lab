package com.example.trainingtrackermethodologylab.model;

import java.time.LocalDate;

public record TrainingRecordStatus(
        Long employeeId,
        Long trainingId,
        LocalDate completedDate,
        boolean completed,
        TrainingStatus status,
        LocalDate expiresOn
) {
}
