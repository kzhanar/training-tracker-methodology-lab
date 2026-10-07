package com.example.trainingtrackermethodologylab.api;

import java.time.LocalDate;

public record ExpiredTrainingCourseResponse(
        Long trainingId,
        String title,
        LocalDate completedDate,
        LocalDate expirationDate
) {
}
