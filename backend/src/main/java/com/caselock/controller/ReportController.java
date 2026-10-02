package com.caselock.controller;

import com.caselock.dto.response.ApiResponse;
import com.caselock.dto.response.CaseReportResponse;
import com.caselock.service.ReportService;
import com.caselock.util.RequestUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'INVESTIGATOR', 'LEGAL_OFFICER', 'FORENSIC_ANALYST')")
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/{caseId}")
    public ResponseEntity<ApiResponse<CaseReportResponse>> report(@PathVariable Long caseId, HttpServletRequest httpRequest) {
        CaseReportResponse response = reportService.buildReport(caseId, RequestUtil.extractClientIp(httpRequest));
        return ResponseEntity.ok(ApiResponse.success("Case report generated.", response));
    }

    @GetMapping(value = "/{caseId}/pdf")
    public ResponseEntity<ByteArrayResource> pdf(@PathVariable Long caseId, HttpServletRequest httpRequest) {
        byte[] pdf = reportService.buildPdfReport(caseId, RequestUtil.extractClientIp(httpRequest));
        ContentDisposition disposition = ContentDisposition.attachment().filename("case-" + caseId + "-report.pdf").build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(MediaType.APPLICATION_PDF)
                .body(new ByteArrayResource(pdf));
    }
}
