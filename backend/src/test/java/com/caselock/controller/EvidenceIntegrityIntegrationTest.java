package com.caselock.controller;

import com.caselock.config.FileStorageProperties;
import com.caselock.entity.Evidence;
import com.caselock.repository.EvidenceRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Covers the single most important guarantee CaseLock makes: SHA-256
 * integrity verification correctly reports VERIFIED for untouched evidence
 * and COMPROMISED the moment the underlying file content changes, without
 * the immutable "original hash" ever being overwritten.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EvidenceIntegrityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EvidenceRepository evidenceRepository;

    @Autowired
    private FileStorageProperties fileStorageProperties;

    @Test
    void verifyIntegrityDetectsTamperingWithoutOverwritingOriginalHash() throws Exception {
        String adminToken = registerAndLogin("evidence_integrity_admin", "ADMIN");
        Long adminId = currentUserId(adminToken);

        // 1. Create a case as admin (ADMIN is allowed to create cases too).
        String createCaseBody = """
                {"title": "Integrity Test Case", "caseType": "CYBERCRIME", "investigatorId": %d}
                """.formatted(adminId);
        MvcResult caseResult = mockMvc.perform(post("/api/cases")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType("application/json")
                        .content(createCaseBody))
                .andExpect(status().isOk())
                .andReturn();
        Long caseId = objectMapper.readTree(caseResult.getResponse().getContentAsString())
                .get("data").get("id").asLong();

        // 2. Upload evidence.
        MockMultipartFile file = new MockMultipartFile(
                "file", "sample.txt", "text/plain", "original evidence bytes".getBytes(StandardCharsets.UTF_8));
        MockMultipartFile metadata = new MockMultipartFile(
                "metadata", "", "application/json",
                ("{\"caseId\": " + caseId + ", \"evidenceCategory\": \"TEXT_FILE\", \"description\": \"test\"}")
                        .getBytes(StandardCharsets.UTF_8));

        MvcResult uploadResult = mockMvc.perform(multipart("/api/evidence")
                        .file(file).file(metadata)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode uploadJson = objectMapper.readTree(uploadResult.getResponse().getContentAsString());
        Long evidenceId = uploadJson.get("data").get("id").asLong();
        String originalHash = uploadJson.get("data").get("originalSha256").asText();

        // 3. Verify immediately - should be VERIFIED, hash unchanged.
        mockMvc.perform(post("/api/evidence/" + evidenceId + "/verify")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.integrityStatus").value("VERIFIED"))
                .andExpect(jsonPath("$.data.match").value(true));

        // 4. Tamper with the file directly on disk (simulating unauthorized modification).
        Evidence evidence = evidenceRepository.findById(evidenceId).orElseThrow();
        Path storedFile = Paths.get(fileStorageProperties.path()).toAbsolutePath().normalize()
                .resolve(evidence.getStoredFileName());
        Files.writeString(storedFile, "TAMPERED CONTENT", StandardCharsets.UTF_8);

        // 5. Verify again - must now report COMPROMISED.
        mockMvc.perform(post("/api/evidence/" + evidenceId + "/verify")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.integrityStatus").value("COMPROMISED"))
                .andExpect(jsonPath("$.data.match").value(false));

        // 6. The immutable original hash must be exactly what was recorded at registration.
        Evidence reloaded = evidenceRepository.findById(evidenceId).orElseThrow();
        assertThat(reloaded.getOriginalSha256()).isEqualTo(originalHash);
        assertThat(reloaded.getCurrentSha256()).isNotEqualTo(originalHash);
    }

    private String registerAndLogin(String username, String role) throws Exception {
        String registerBody = """
                {"username": "%s", "email": "%s@caselock.demo", "password": "StrongPass123!",
                 "fullName": "Test User", "role": "%s"}
                """.formatted(username, username, role);
        mockMvc.perform(post("/api/auth/register").contentType("application/json").content(registerBody))
                .andExpect(status().isOk());

        String loginBody = """
                {"username": "%s", "password": "StrongPass123!"}
                """.formatted(username);
        MvcResult result = mockMvc.perform(post("/api/auth/login").contentType("application/json").content(loginBody))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("data").get("accessToken").asText();
    }

    private Long currentUserId(String token) throws Exception {
        MvcResult result = mockMvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/auth/me")
                                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("data").get("id").asLong();
    }
}
