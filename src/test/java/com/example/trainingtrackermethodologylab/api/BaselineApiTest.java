package com.example.trainingtrackermethodologylab.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

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
                .andExpect(jsonPath("$.required").value(true));

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
                        """));
    }

    @Test
    void completeTrainingAndListEmployeeRecords() throws Exception {
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
                                  "completedDate": "2026-10-07"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.employeeId").value(1))
                .andExpect(jsonPath("$.trainingId").value(1))
                .andExpect(jsonPath("$.completedDate").value("2026-10-07"));

        mockMvc.perform(get("/employees/1/training"))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        [
                          {
                            "employeeId": 1,
                            "trainingId": 1,
                            "completedDate": "2026-10-07"
                          }
                        ]
                        """));
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
}
