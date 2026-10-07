package com.example.trainingtrackermethodologylab.model;

import java.time.LocalDate;

public record TrainingRecord(Long employeeId, Long trainingId, LocalDate completedDate) {
}
