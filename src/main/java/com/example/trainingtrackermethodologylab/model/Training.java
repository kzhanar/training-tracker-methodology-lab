package com.example.trainingtrackermethodologylab.model;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record Training(Long id, String title, boolean required, Integer validityPeriodDays) {
}
