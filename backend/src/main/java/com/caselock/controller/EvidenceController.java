package com.caselock.controller;

import com.caselock.dto.request.EvidenceTransferRequest;
import com.caselock.dto.request.EvidenceUploadMetadata;
import com.caselock.dto.response.ApiResponse;
import com.caselock.dto.response.ChainOfCustodyResponse;
import com.caselock.dto.response.EvidenceResponse;
import com.caselock.dto.response.EvidenceSummaryResponse;
import com.caselock.dto.response.IntegrityVerificationResponse;
import com.caselock.dto.response.PageResponse;
import com.caselock.entity.Evidence;
import com.caselock.entity.enums.EvidenceCategory;
import com.caselock.exception.BadRequestException;
import com.caselock.service.ChainOfCustodyService;
import com.caselock.service.EvidenceService;
import com.caselock.util.RequestUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/evidence")
@RequiredArgsConstructor
public class EvidenceController {

    private final EvidenceService evidenceService;
    private final ChainOfCustodyService chainOfCustodyService;
    private final ObjectMapper objectMapper;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'INVESTIGATOR', 'FORENSIC_ANALYST')")
    public ResponseEntity<ApiResponse<EvidenceResponse>> upload(@RequestParam("file") MultipartFile file,
                                                                  HttpServletRequest httpRequest) {
        String metadataJson = extractMetadataString(httpRequest);

        if (metadataJson == null || metadataJson.isBlank()) {
            throw new BadRequestException("The evidence metadata could not be read. Please check the form and try again.");
        }

        EvidenceUploadMetadata metadata;
        try {
            metadata = objectMapper.readValue(metadataJson, EvidenceUploadMetadata.class);
        } catch (Exception ex) {
            throw new BadRequestException("The evidence metadata could not be read. Please check the form and try again.");
        }
        EvidenceResponse response = evidenceService.register(file, metadata, RequestUtil.extractClientIp(httpRequest));
        return ResponseEntity.ok(ApiResponse.success("Evidence registered and hash computed successfully.", response));
    }

    private String extractMetadataString(HttpServletRequest request) {
        String metadata = request.getParameter("metadata");
        if (metadata != null && !metadata.isBlank()) {
            return metadata;
        }

        org.springframework.web.multipart.MultipartHttpServletRequest multipartRequest =
                org.springframework.web.util.WebUtils.getNativeRequest(request, org.springframework.web.multipart.MultipartHttpServletRequest.class);
        if (multipartRequest != null) {
            MultipartFile metaFile = multipartRequest.getFile("metadata");
            if (metaFile != null && !metaFile.isEmpty()) {
                try {
                    String content = new String(metaFile.getBytes(), java.nio.charset.StandardCharsets.UTF_8);
                    if (!content.isBlank()) {
                        return content;
                    }
                } catch (Exception ignored) {
                }
            }
        }

        try {
            jakarta.servlet.http.Part part = request.getPart("metadata");
            if (part != null) {
                try (java.io.InputStream is = part.getInputStream()) {
                    String content = new String(is.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                    if (!content.isBlank()) {
                        return content;
                    }
                }
            }
        } catch (Exception ignored) {
        }

        return null;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<EvidenceSummaryResponse>>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long caseId,
            @RequestParam(required = false) EvidenceCategory category,
            @RequestParam(required = false) Long custodianId,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("Evidence retrieved.",
                evidenceService.search(keyword, caseId, category, custodianId, pageable)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<EvidenceResponse>> get(@PathVariable Long id, HttpServletRequest httpRequest) {
        EvidenceResponse response = evidenceService.recordAccess(id, RequestUtil.extractClientIp(httpRequest));
        return ResponseEntity.ok(ApiResponse.success("Evidence retrieved.", response));
    }

    @GetMapping("/{id}/download")
    @PreAuthorize("hasAnyRole('ADMIN', 'INVESTIGATOR', 'FORENSIC_ANALYST', 'LEGAL_OFFICER')")
    public ResponseEntity<ByteArrayResource> download(@PathVariable Long id, HttpServletRequest httpRequest) {
        Evidence evidence = evidenceService.loadEvidenceEntityForDownloadMetadata(id);
        byte[] bytes = evidenceService.download(id, RequestUtil.extractClientIp(httpRequest));

        ContentDisposition disposition = ContentDisposition.attachment().filename(evidence.getFileName()).build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(new ByteArrayResource(bytes));
    }

    @PostMapping("/{id}/verify")
    @PreAuthorize("hasAnyRole('ADMIN', 'FORENSIC_ANALYST')")
    public ResponseEntity<ApiResponse<IntegrityVerificationResponse>> verify(@PathVariable Long id,
                                                                               HttpServletRequest httpRequest) {
        IntegrityVerificationResponse response = evidenceService.verifyIntegrity(id, RequestUtil.extractClientIp(httpRequest));
        return ResponseEntity.ok(ApiResponse.success(response.message(), response));
    }

    @PostMapping("/{id}/transfer")
    @PreAuthorize("hasAnyRole('ADMIN', 'INVESTIGATOR', 'FORENSIC_ANALYST')")
    public ResponseEntity<ApiResponse<ChainOfCustodyResponse>> transfer(@PathVariable Long id,
                                                                          @Valid @RequestBody EvidenceTransferRequest request,
                                                                          HttpServletRequest httpRequest) {
        ChainOfCustodyResponse response = chainOfCustodyService.transfer(id, request, RequestUtil.extractClientIp(httpRequest));
        return ResponseEntity.ok(ApiResponse.success("Evidence transferred successfully.", response));
    }

    @GetMapping("/{id}/chain-of-custody")
    public ResponseEntity<ApiResponse<java.util.List<ChainOfCustodyResponse>>> chainOfCustody(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Chain of custody retrieved.",
                chainOfCustodyService.timelineForEvidence(id)));
    }

    @PostMapping("/{id}/seal")
    @PreAuthorize("hasAnyRole('ADMIN', 'INVESTIGATOR', 'FORENSIC_ANALYST')")
    public ResponseEntity<ApiResponse<ChainOfCustodyResponse>> seal(@PathVariable Long id,
                                                                      @RequestParam(required = false) String remarks,
                                                                      HttpServletRequest httpRequest) {
        ChainOfCustodyResponse response = chainOfCustodyService.recordAction(id, com.caselock.entity.enums.CustodyAction.SEALED,
                "Evidence sealed", remarks, RequestUtil.extractClientIp(httpRequest));
        return ResponseEntity.ok(ApiResponse.success("Evidence sealed successfully.", response));
    }

    @PostMapping("/{id}/reopen")
    @PreAuthorize("hasAnyRole('ADMIN', 'INVESTIGATOR', 'FORENSIC_ANALYST')")
    public ResponseEntity<ApiResponse<ChainOfCustodyResponse>> reopen(@PathVariable Long id,
                                                                        @RequestParam(required = false) String remarks,
                                                                        HttpServletRequest httpRequest) {
        ChainOfCustodyResponse response = chainOfCustodyService.recordAction(id, com.caselock.entity.enums.CustodyAction.REOPENED,
                "Evidence reopened", remarks, RequestUtil.extractClientIp(httpRequest));
        return ResponseEntity.ok(ApiResponse.success("Evidence reopened successfully.", response));
    }
}
