package com.example.trainingtrackermethodologylab.api;

import com.example.trainingtrackermethodologylab.model.Employee;
import com.example.trainingtrackermethodologylab.service.EmployeeService;
import com.example.trainingtrackermethodologylab.service.TrainingRecordService;
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
@RequestMapping("/employees")
public class EmployeeController {

    private final EmployeeService employeeService;
    private final TrainingRecordService trainingRecordService;

    public EmployeeController(EmployeeService employeeService, TrainingRecordService trainingRecordService) {
        this.employeeService = employeeService;
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

    @GetMapping("/expired-required-training")
    public List<Employee> listEmployeesWithExpiredRequiredTraining() {
        return employeeService.listByIds(trainingRecordService.expiredRequiredTrainingEmployeeIds());
    }
}
