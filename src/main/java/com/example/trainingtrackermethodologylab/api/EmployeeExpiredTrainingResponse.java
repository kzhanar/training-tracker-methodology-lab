package com.example.trainingtrackermethodologylab.api;

import com.example.trainingtrackermethodologylab.model.Employee;

import java.util.List;

public record EmployeeExpiredTrainingResponse(
        Employee employee,
        List<ExpiredTrainingCourseResponse> expiredTrainings
) {
}
