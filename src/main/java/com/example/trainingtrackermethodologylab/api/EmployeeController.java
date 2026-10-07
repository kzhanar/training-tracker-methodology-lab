package com.example.trainingtrackermethodologylab.api;

import com.example.trainingtrackermethodologylab.model.Employee;
import com.example.trainingtrackermethodologylab.model.Training;
import com.example.trainingtrackermethodologylab.model.TrainingRecord;
import com.example.trainingtrackermethodologylab.service.EmployeeService;
import com.example.trainingtrackermethodologylab.service.TrainingRecordService;
import com.example.trainingtrackermethodologylab.service.TrainingService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/employees")
public class EmployeeController {

    private final EmployeeService employeeService;
    private final TrainingService trainingService;
    private final TrainingRecordService trainingRecordService;

    public EmployeeController(
            EmployeeService employeeService,
            TrainingService trainingService,
            TrainingRecordService trainingRecordService
    ) {
        this.employeeService = employeeService;
        this.trainingService = trainingService;
        this.trainingRecordService = trainingRecordService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Employee create(@RequestBody CreateEmployeeRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request body is required");
        }
        try {
            return employeeService.create(request.name(), request.email());
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }

    @GetMapping
    public List<Employee> list() {
        return employeeService.list();
    }

    @GetMapping("/expired-training")
    public List<EmployeeExpiredTrainingResponse> listEmployeesWithExpiredRequiredTraining() {
        Map<Long, List<ExpiredTrainingCourseResponse>> coursesByEmployee = new LinkedHashMap<>();
        for (TrainingRecord record : trainingRecordService.listExpiredRequiredCompletions()) {
            Training training = trainingService.getRequired(record.trainingId());
            coursesByEmployee.computeIfAbsent(record.employeeId(), ignored -> new ArrayList<>())
                    .add(new ExpiredTrainingCourseResponse(
                            training.id(),
                            training.title(),
                            record.completedDate(),
                            record.completedDate().plusDays(training.validityPeriodDays())
                    ));
        }

        return coursesByEmployee.entrySet().stream()
                .map(entry -> new EmployeeExpiredTrainingResponse(
                        employeeService.getRequired(entry.getKey()),
                        entry.getValue()
                ))
                .toList();
    }
}
