package com.example.trainingtrackermethodologylab.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class BaselineApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void createAndListEmployees() throws Exception {
        mockMvc.perform(post("/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Jane Doe",
                                  "email": "jane.doe@example.com"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Jane Doe"))
                .andExpect(jsonPath("$.email").value("jane.doe@example.com"));

        mockMvc.perform(get("/employees"))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        [
                          {
                            "id": 1,
                            "name": "Jane Doe",
                            "email": "jane.doe@example.com"
                          }
                        ]
                        """));
    }

    @Test
    void createAndListTrainingCourses() throws Exception {
        mockMvc.perform(post("/trainings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Security Basics",
                                  "required": true
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Security Basics"))
                .andExpect(jsonPath("$.required").value(true))
                .andExpect(jsonPath("$.validityPeriodDays").doesNotExist());

        mockMvc.perform(get("/trainings"))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        [
                          {
                            "id": 1,
                            "title": "Security Basics",
                            "required": true
                          }
                        ]
                        """))
                .andExpect(jsonPath("$[0].validityPeriodDays").doesNotExist());
    }

    @Test
    void completeTrainingAndListEmployeeRecords() throws Exception {
        String today = LocalDate.now().toString();
        mockMvc.perform(post("/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "John Smith",
                                  "email": "john.smith@example.com"
                                }
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/trainings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Privacy 101",
                                  "required": false
                                }
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/employees/1/training/1/complete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "completedDate": "%s"
                                }
                                """.formatted(today)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.employeeId").value(1))
                .andExpect(jsonPath("$.trainingId").value(1))
                .andExpect(jsonPath("$.completedDate").value(today))
                .andExpect(jsonPath("$.completed").value(true))
                .andExpect(jsonPath("$.status").value("CURRENT"));

        mockMvc.perform(get("/employees/1/training"))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        [
                          {
                            "employeeId": 1,
                            "trainingId": 1,
                            "completedDate": "%s",
                            "completed": true,
                            "status": "CURRENT"
                          }
                        ]
                        """.formatted(today)));
    }

    @Test
    void unknownEmployeeIdReturnsNotFound() throws Exception {
        mockMvc.perform(get("/employees/999/training"))
                .andExpect(status().isNotFound());
    }

    @Test
    void unknownTrainingIdReturnsNotFound() throws Exception {
        mockMvc.perform(post("/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Taylor",
                                  "email": "taylor@example.com"
                                }
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/employees/1/training/999/complete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "completedDate": "2026-10-07"
                                }
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    void employeeValidationErrorsAreClear() throws Exception {
        mockMvc.perform(post("/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "",
                                  "email": "jane.doe@example.com"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Employee name is required"));

        mockMvc.perform(post("/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Jane Doe",
                                  "email": ""
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Employee email is required"));

        mockMvc.perform(post("/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Jane Doe",
                                  "email": "not-an-email"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Employee email must be a valid email address"));
    }

    @Test
    void trainingValidationErrorsAreClear() throws Exception {
        mockMvc.perform(post("/trainings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "",
                                  "required": true
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Training title is required"));
    }

    @Test
    void validityPeriodIsOptionalAndValidated() throws Exception {
        mockMvc.perform(post("/trainings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Optional Validity",
                                  "required": true,
                                  "validityPeriodDays": 30
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.validityPeriodDays").value(30));

        for (int invalidPeriod : new int[]{0, -1}) {
            mockMvc.perform(post("/trainings")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "title": "Invalid Validity",
                                      "required": true,
                                      "validityPeriodDays": %d
                                    }
                                    """.formatted(invalidPeriod)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error")
                            .value("Training validityPeriodDays must be a positive integer"));
        }

        mockMvc.perform(get("/trainings"))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        [
                          {
                            "id": 1,
                            "title": "Optional Validity",
                            "required": true,
                            "validityPeriodDays": 30
                          }
                        ]
                        """));
    }

    @Test
    void trainingStatusUsesApprovedBoundariesAndFutureDates() throws Exception {
        LocalDate today = LocalDate.now();
        createEmployee("Status Tester", "status.tester@example.com");

        createTraining("Current", true, 60);
        completeTraining(1, 1, today.minusDays(29), "CURRENT", true);

        createTraining("Thirty Days Left", true, 60);
        completeTraining(1, 2, today.minusDays(30), "EXPIRING_SOON", true);

        createTraining("Expires Today", true, 60);
        completeTraining(1, 3, today.minusDays(60), "EXPIRING_SOON", true);

        createTraining("Expired Yesterday", true, 60);
        completeTraining(1, 4, today.minusDays(61), "EXPIRED", true);

        createTraining("Future Completion", true, 1);
        completeTraining(1, 5, today.plusDays(1), "CURRENT", false);
    }

    @Test
    void expiredRequiredTrainingEmployeesUseLatestCompletedRequiredAttempts() throws Exception {
        LocalDate today = LocalDate.now();
        createEmployee("Retaken Employee", "retaken@example.com");
        createEmployee("Overdue Employee", "overdue@example.com");
        createEmployee("No Record Employee", "no.record@example.com");
        createEmployee("Future Completion Employee", "future@example.com");
        createEmployee("Future Retake Employee", "future.retake@example.com");

        createTraining("Required A", true, 10);
        createTraining("Required B", true, 10);
        createTraining("Optional", false, 10);

        completeTraining(1, 1, today.minusDays(11), "EXPIRED", true);
        completeTraining(1, 1, today, "EXPIRING_SOON", true);
        completeTraining(1, 3, today.minusDays(11), "EXPIRED", true);

        completeTraining(2, 1, today.minusDays(11), "EXPIRED", true);
        completeTraining(2, 2, today.minusDays(11), "EXPIRED", true);

        completeTraining(4, 1, today.plusDays(1), "CURRENT", false);
        completeTraining(5, 1, today.minusDays(11), "EXPIRED", true);
        completeTraining(5, 1, today.plusDays(1), "CURRENT", false);

        mockMvc.perform(get("/employees/expired-required-training"))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        [
                          {
                            "id": 2,
                            "name": "Overdue Employee",
                            "email": "overdue@example.com"
                          },
                          {
                            "id": 5,
                            "name": "Future Retake Employee",
                            "email": "future.retake@example.com"
                          }
                        ]
                        """));
    }

    @Test
    void completedDateValidationErrorIsClear() throws Exception {
        mockMvc.perform(post("/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Jordan Lee",
                                  "email": "jordan.lee@example.com"
                                }
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/trainings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Safety",
                                  "required": true
                                }
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/employees/1/training/1/complete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("completedDate is required"));
    }

    private void createEmployee(String name, String email) throws Exception {
        mockMvc.perform(post("/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "%s",
                                  "email": "%s"
                                }
                                """.formatted(name, email)))
                .andExpect(status().isCreated());
    }

    private void createTraining(String title, boolean required, Integer validityPeriodDays) throws Exception {
        String validity = validityPeriodDays == null ? "" : ",\n  \"validityPeriodDays\": " + validityPeriodDays;
        mockMvc.perform(post("/trainings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "%s",
                                  "required": %s%s
                                }
                                """.formatted(title, required, validity)))
                .andExpect(status().isCreated());
    }

    private void completeTraining(
            long employeeId,
            long trainingId,
            LocalDate completedDate,
            String expectedStatus,
            boolean expectedCompleted
    ) throws Exception {
        mockMvc.perform(post("/employees/{employeeId}/training/{trainingId}/complete", employeeId, trainingId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "completedDate": "%s"
                                }
                                """.formatted(completedDate)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(expectedStatus))
                .andExpect(jsonPath("$.completed").value(expectedCompleted));
    }
}
