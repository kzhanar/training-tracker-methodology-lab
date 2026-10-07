package com.example.trainingtrackermethodologylab.api;

import com.example.trainingtrackermethodologylab.model.TrainingStatus;

import java.time.LocalDate;

public record TrainingStatusResponse(
        Long employeeId,
        Long trainingId,
        LocalDate completedDate,
        boolean completed,
        TrainingStatus status,
        LocalDate expiresOn
) {
}
