package com.example.trainingtrackermethodologylab.api;

import java.time.LocalDate;

public record TrainingRecordResponse(
        Long employeeId,
        Long trainingId,
        LocalDate completedDate,
        boolean completed,
        String status
) {
}
