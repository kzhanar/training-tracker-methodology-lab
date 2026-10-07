package com.example.trainingtrackermethodologylab.api;

import java.math.BigDecimal;

public record CreateTrainingRequest(String title, boolean required, BigDecimal validityPeriodDays) {
}
