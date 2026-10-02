package com.caselock.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end coverage of the authentication flow: registration, login with
 * correct/incorrect credentials, and that a protected endpoint rejects
 * requests with no token at all. This is the most security-sensitive path
 * in the application, so it is tested through the real HTTP layer rather
 * than mocked.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void registerThenLoginSucceedsWithCorrectCredentials() throws Exception {
        String registerBody = """
                {
                  "username": "test_investigator",
                  "email": "test_investigator@caselock.demo",
                  "password": "StrongPass123!",
                  "fullName": "Test Investigator",
                  "role": "INVESTIGATOR"
                }
                """;

        mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content(registerBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.username").value("test_investigator"));

        String loginBody = """
                {"username": "test_investigator", "password": "StrongPass123!"}
                """;

        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content(loginBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.user.role").value("INVESTIGATOR"));
    }

    @Test
    void loginFailsWithWrongPassword() throws Exception {
        String registerBody = """
                {
                  "username": "test_wrongpass",
                  "email": "test_wrongpass@caselock.demo",
                  "password": "CorrectPass123!",
                  "fullName": "Wrong Pass Tester",
                  "role": "VIEWER"
                }
                """;
        mockMvc.perform(post("/api/auth/register").contentType("application/json").content(registerBody))
                .andExpect(status().isOk());

        String loginBody = """
                {"username": "test_wrongpass", "password": "IncorrectPassword!"}
                """;
        mockMvc.perform(post("/api/auth/login").contentType("application/json").content(loginBody))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("INVALID_CREDENTIALS"));
    }

    @Test
    void protectedEndpointRejectsRequestsWithNoToken() throws Exception {
        mockMvc.perform(get("/api/dashboard/stats"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void duplicateUsernameIsRejectedWithConflict() throws Exception {
        String body = """
                {
                  "username": "duplicate_user",
                  "email": "dup1@caselock.demo",
                  "password": "StrongPass123!",
                  "fullName": "Dup One",
                  "role": "VIEWER"
                }
                """;
        mockMvc.perform(post("/api/auth/register").contentType("application/json").content(body))
                .andExpect(status().isOk());

        String duplicateBody = """
                {
                  "username": "duplicate_user",
                  "email": "dup2@caselock.demo",
                  "password": "StrongPass123!",
                  "fullName": "Dup Two",
                  "role": "VIEWER"
                }
                """;
        mockMvc.perform(post("/api/auth/register").contentType("application/json").content(duplicateBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("USERNAME_TAKEN"));
    }
}
