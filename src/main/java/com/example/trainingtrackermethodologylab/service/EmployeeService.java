package com.example.trainingtrackermethodologylab.service;

import com.example.trainingtrackermethodologylab.model.Employee;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class EmployeeService {
    private static final String EMAIL_PATTERN = "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$";

    private final AtomicLong idSequence = new AtomicLong(1);
    private final Map<Long, Employee> employees = new LinkedHashMap<>();

    public Employee create(String name, String email) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Employee name is required");
        }
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Employee email is required");
        }
        if (!email.trim().matches(EMAIL_PATTERN)) {
            throw new IllegalArgumentException("Employee email must be a valid email address");
        }
        long id = idSequence.getAndIncrement();
        Employee employee = new Employee(id, name.trim(), email.trim());
        employees.put(id, employee);
        return employee;
    }

    public List<Employee> list() {
        return employees.values()
                .stream()
                .sorted(Comparator.comparing(Employee::id))
                .toList();
    }

    public List<Employee> listByIds(Set<Long> employeeIds) {
        return employees.values()
                .stream()
                .filter(employee -> employeeIds.contains(employee.id()))
                .sorted(Comparator.comparing(Employee::id))
                .toList();
    }

    public Employee getRequired(long employeeId) {
        Employee employee = employees.get(employeeId);
        if (employee == null) {
            throw new NoSuchElementException("Employee not found");
        }
        return employee;
    }
}
