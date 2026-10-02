package com.caselock.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Confirms role-based access control is enforced by the backend itself
 * (Spring Security @PreAuthorize), not just hidden in the UI: a VIEWER
 * account must be rejected with 403 when it tries to perform an
 * investigator/admin-only action such as creating a case, even with a
 * perfectly valid JWT.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CaseAuthorizationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void viewerCannotCreateACase() throws Exception {
        String viewerToken = registerAndLogin("case_auth_viewer", "VIEWER");

        String createCaseBody = """
                {
                  "title": "Unauthorized Attempt",
                  "caseType": "FRAUD",
                  "investigatorId": 1
                }
                """;

        mockMvc.perform(post("/api/cases")
                        .header("Authorization", "Bearer " + viewerToken)
                        .contentType("application/json")
                        .content(createCaseBody))
                .andExpect(status().isForbidden());
    }

    @Test
    void investigatorCanCreateACase() throws Exception {
        String investigatorToken = registerAndLogin("case_auth_investigator", "INVESTIGATOR");
        Long investigatorId = currentUserId(investigatorToken);

        String createCaseBody = """
                {
                  "title": "Authorized Investigator Case",
                  "caseType": "CRIMINAL",
                  "investigatorId": %d
                }
                """.formatted(investigatorId);

        mockMvc.perform(post("/api/cases")
                        .header("Authorization", "Bearer " + investigatorToken)
                        .contentType("application/json")
                        .content(createCaseBody))
                .andExpect(status().isOk());
    }

    private String registerAndLogin(String username, String role) throws Exception {
        String registerBody = """
                {
                  "username": "%s",
                  "email": "%s@caselock.demo",
                  "password": "StrongPass123!",
                  "fullName": "Test User %s",
                  "role": "%s"
                }
                """.formatted(username, username, username, role);
        mockMvc.perform(post("/api/auth/register").contentType("application/json").content(registerBody))
                .andExpect(status().isOk());

        String loginBody = """
                {"username": "%s", "password": "StrongPass123!"}
                """.formatted(username);
        MvcResult result = mockMvc.perform(post("/api/auth/login").contentType("application/json").content(loginBody))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.get("data").get("accessToken").asText();
    }

    private Long currentUserId(String token) throws Exception {
        MvcResult result = mockMvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/auth/me")
                                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        Long id = json.get("data").get("id").asLong();
        assertThat(id).isPositive();
        return id;
    }
}
