package com.example.trainingtrackermethodologylab.service;

import com.example.trainingtrackermethodologylab.model.Training;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class TrainingService {

    private final AtomicLong idSequence = new AtomicLong(1);
    private final Map<Long, Training> trainings = new LinkedHashMap<>();

    public Training create(String title, boolean required, Integer validityPeriodDays) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Training title is required");
        }
        if (validityPeriodDays != null && validityPeriodDays < 1) {
            throw new IllegalArgumentException("Training validity period must be a positive integer");
        }
        long id = idSequence.getAndIncrement();
        Training training = new Training(id, title.trim(), required, validityPeriodDays);
        trainings.put(id, training);
        return training;
    }

    public List<Training> list() {
        return trainings.values()
                .stream()
                .sorted(Comparator.comparing(Training::id))
                .toList();
    }

    public Training getRequired(long trainingId) {
        Training training = trainings.get(trainingId);
        if (training == null) {
            throw new NoSuchElementException("Training not found");
        }
        return training;
    }
}
