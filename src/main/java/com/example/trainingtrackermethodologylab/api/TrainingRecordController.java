package com.example.trainingtrackermethodologylab.api;

import com.example.trainingtrackermethodologylab.model.TrainingRecord;
import com.example.trainingtrackermethodologylab.model.TrainingRecordStatus;
import com.example.trainingtrackermethodologylab.service.TrainingRecordService;
import com.example.trainingtrackermethodologylab.service.TrainingStatusService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/employees/{employeeId}/training")
public class TrainingRecordController {

    private final TrainingRecordService trainingRecordService;
    private final TrainingStatusService trainingStatusService;

    public TrainingRecordController(
            TrainingRecordService trainingRecordService,
            TrainingStatusService trainingStatusService
    ) {
        this.trainingRecordService = trainingRecordService;
        this.trainingStatusService = trainingStatusService;
    }

    @PostMapping("/{trainingId}/complete")
    @ResponseStatus(HttpStatus.CREATED)
    public TrainingRecordResponse completeTraining(
            @PathVariable long employeeId,
            @PathVariable long trainingId,
            @RequestBody CompleteTrainingRequest request
    ) {
        if (request == null || request.completedDate() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "completedDate is required");
        }
        try {
            TrainingRecord record = trainingRecordService.completeTraining(employeeId, trainingId, request.completedDate());
            return toResponse(record);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        } catch (NoSuchElementException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage(), ex);
        }
    }

    @GetMapping
    public List<TrainingRecordResponse> listEmployeeTrainingRecords(@PathVariable long employeeId) {
        try {
            return trainingRecordService.listForEmployee(employeeId)
                    .stream()
                    .map(this::toResponse)
                    .toList();
        } catch (NoSuchElementException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage(), ex);
        }
    }

    @GetMapping("/status")
    public List<TrainingStatusResponse> listEmployeeTrainingStatus(@PathVariable long employeeId) {
        try {
            return trainingStatusService.listStatusForEmployee(employeeId)
                    .stream()
                    .map(this::toStatusResponse)
                    .toList();
        } catch (NoSuchElementException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage(), ex);
        }
    }

    // Keep boolean "completed" in API responses for backward compatibility with existing clients.
    private TrainingRecordResponse toResponse(TrainingRecord record) {
        boolean completed = record.completedDate() != null && !record.completedDate().isAfter(LocalDate.now());
        return new TrainingRecordResponse(record.employeeId(), record.trainingId(), record.completedDate(), completed);
    }

    private TrainingStatusResponse toStatusResponse(TrainingRecordStatus status) {
        return new TrainingStatusResponse(
                status.employeeId(),
                status.trainingId(),
                status.completedDate(),
                status.completed(),
                status.status(),
                status.expiresOn()
        );
    }
}
