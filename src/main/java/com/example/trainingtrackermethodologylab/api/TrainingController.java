package com.example.trainingtrackermethodologylab.api;

import com.example.trainingtrackermethodologylab.model.Training;
import com.example.trainingtrackermethodologylab.service.TrainingService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/trainings")
public class TrainingController {

    private final TrainingService trainingService;

    public TrainingController(TrainingService trainingService) {
        this.trainingService = trainingService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Training create(@RequestBody CreateTrainingRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request body is required");
        }
        try {
            return trainingService.create(
                    request.title(),
                    request.required(),
                    toValidityPeriodDays(request.validityPeriodDays())
            );
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }

    @GetMapping
    public List<Training> list() {
        return trainingService.list();
    }

    private Integer toValidityPeriodDays(java.math.BigDecimal validityPeriodDays) {
        if (validityPeriodDays == null) {
            return null;
        }
        try {
            return validityPeriodDays.intValueExact();
        } catch (ArithmeticException ex) {
            throw new IllegalArgumentException("Training validity period must be a positive integer", ex);
        }
    }
}
