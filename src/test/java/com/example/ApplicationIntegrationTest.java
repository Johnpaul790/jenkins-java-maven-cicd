package com.example;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ApplicationIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void homePageShouldReturnWelcomeMessage() {

        assertEquals(
                HttpStatus.OK,
                restTemplate.getForEntity("/", String.class).getStatusCode()
        );

        String body = restTemplate.getForObject("/", String.class);

        assertTrue(
                body.contains("Welcome to Java Maven Application")
        );
    }
}