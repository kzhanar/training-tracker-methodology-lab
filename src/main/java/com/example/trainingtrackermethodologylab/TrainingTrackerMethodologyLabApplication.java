package com.example.trainingtrackermethodologylab;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.time.Clock;

@SpringBootApplication
public class TrainingTrackerMethodologyLabApplication {

    @Bean
    Clock trainingClock() {
        return Clock.systemDefaultZone();
    }

    public static void main(String[] args) {
        SpringApplication.run(TrainingTrackerMethodologyLabApplication.class, args);
    }
}
