package com.example.trainingtrackermethodologylab.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class TrainingExpirationApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void validityPeriodIsOptionalAndMustBePositive() throws Exception {
        createTraining("Annual Safety", true, 365);
        createTraining("Legacy Safety", true, null);

        mockMvc.perform(get("/trainings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].validityPeriodDays").value(365))
                .andExpect(jsonPath("$[1].validityPeriodDays").value(nullValue()));

        mockMvc.perform(post("/trainings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Explicitly unbounded",
                                  "required": false,
                                  "validityPeriodDays": null
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.validityPeriodDays").value(nullValue()));

        mockMvc.perform(post("/trainings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Invalid zero",
                                  "required": true,
                                  "validityPeriodDays": 0
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Training validity period must be a positive number of days"));

        mockMvc.perform(post("/trainings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Invalid negative",
                                  "required": true,
                                  "validityPeriodDays": -1
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Training validity period must be a positive number of days"));
    }

    @Test
    void completionStatusesRespectDateBoundariesAndPreserveHistory() throws Exception {
        createEmployee();
        createTraining("No expiration", true, null);
        createTraining("Expires today", true, 20);
        createTraining("Expires in 30 days", true, 60);
        createTraining("Expires in 31 days", true, 60);
        createTraining("Future completion", true, 1);

        LocalDate today = LocalDate.now();
        recordCompletion(1, today.minusYears(3), "CURRENT");
        recordCompletion(2, today.minusDays(20), "EXPIRED");
        recordCompletion(3, today.minusDays(30), "EXPIRING_SOON");
        recordCompletion(4, today.minusDays(29), "CURRENT");
        mockMvc.perform(post("/employees/1/training/5/complete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"completedDate":"%s"}
                                """.formatted(today.plusDays(1))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.completed").value(false))
                .andExpect(jsonPath("$.status").value("CURRENT"));

        mockMvc.perform(get("/employees/1/training"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("CURRENT"))
                .andExpect(jsonPath("$[1].status").value("EXPIRING_SOON"))
                .andExpect(jsonPath("$[2].status").value("CURRENT"))
                .andExpect(jsonPath("$[3].status").value("EXPIRED"))
                .andExpect(jsonPath("$[4].status").value("CURRENT"))
                .andExpect(jsonPath("$[4].completed").value(false))
                .andExpect(jsonPath("$[3].employeeId").value(1))
                .andExpect(jsonPath("$[3].trainingId").value(2))
                .andExpect(jsonPath("$[3].completedDate").value(today.minusDays(20).toString()));
    }

    @Test
    void expiredRequiredTrainingUsesLatestCompletionAndGroupsCourses() throws Exception {
        createEmployee();
        createTraining("Expired required", true, 1);
        createTraining("Retaken required", true, 120);
        createTraining("Never expires", true, null);
        createTraining("Expired optional", false, 1);
        createTraining("Second expired required", true, 1);
        createTraining("Not completed required", true, 1);

        LocalDate today = LocalDate.now();
        recordCompletion(1, today.minusDays(1), "EXPIRED");
        recordCompletion(2, today.minusDays(200), "EXPIRED");
        recordCompletion(2, today.minusDays(1), "CURRENT");
        recordCompletion(3, today.minusYears(2), "CURRENT");
        recordCompletion(4, today.minusDays(1), "EXPIRED");
        recordCompletion(5, today.minusDays(1), "EXPIRED");

        mockMvc.perform(get("/employees/expired-training"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].employee.id").value(1))
                .andExpect(jsonPath("$[0].employee.name").value("Jamie Example"))
                .andExpect(jsonPath("$[0].employee.email").value("jamie@example.com"))
                .andExpect(jsonPath("$[0].expiredTrainings.length()").value(2))
                .andExpect(jsonPath("$[0].expiredTrainings[0].trainingId").value(1))
                .andExpect(jsonPath("$[0].expiredTrainings[0].title").value("Expired required"))
                .andExpect(jsonPath("$[0].expiredTrainings[0].completedDate").value(today.minusDays(1).toString()))
                .andExpect(jsonPath("$[0].expiredTrainings[0].expirationDate").value(today.toString()))
                .andExpect(jsonPath("$[0].expiredTrainings[1].trainingId").value(5));
    }

    @Test
    void expiredRequiredTrainingReturnsEmptyArrayWhenNoCoursesAreExpired() throws Exception {
        createEmployee();

        mockMvc.perform(get("/employees/expired-training"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    private void createEmployee() throws Exception {
        mockMvc.perform(post("/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Jamie Example",
                                  "email": "jamie@example.com"
                                }
                                """))
                .andExpect(status().isCreated());
    }

    private void createTraining(String title, boolean required, Integer validityPeriodDays) throws Exception {
        String validityField = validityPeriodDays == null ? "" : ", \"validityPeriodDays\": " + validityPeriodDays;
        String request = """
                {
                  "title": "%s",
                  "required": %s%s
                }
                """.formatted(title, required, validityField);
        var result = mockMvc.perform(post("/trainings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
                .andExpect(status().isCreated());
        if (validityPeriodDays == null) {
            result.andExpect(jsonPath("$.validityPeriodDays").value(nullValue()));
        } else {
            result.andExpect(jsonPath("$.validityPeriodDays").value(validityPeriodDays));
        }
    }

    private void recordCompletion(long trainingId, LocalDate completedDate, String expectedStatus) throws Exception {
        mockMvc.perform(post("/employees/1/training/{trainingId}/complete", trainingId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"completedDate":"%s"}
                                """.formatted(completedDate)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(expectedStatus));
    }
}
