package com.caselock.controller;

import com.caselock.dto.request.CaseCreateRequest;
import com.caselock.dto.request.CaseStatusUpdateRequest;
import com.caselock.dto.request.CaseUpdateRequest;
import com.caselock.dto.response.ApiResponse;
import com.caselock.dto.response.CaseResponse;
import com.caselock.dto.response.CaseSummaryResponse;
import com.caselock.dto.response.PageResponse;
import com.caselock.entity.enums.CasePriority;
import com.caselock.entity.enums.CaseStatus;
import com.caselock.service.CaseService;
import com.caselock.util.RequestUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cases")
@RequiredArgsConstructor
public class CaseController {

    private final CaseService caseService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'INVESTIGATOR')")
    public ResponseEntity<ApiResponse<CaseResponse>> create(@Valid @RequestBody CaseCreateRequest request,
                                                              HttpServletRequest httpRequest) {
        CaseResponse response = caseService.createCase(request, RequestUtil.extractClientIp(httpRequest));
        return ResponseEntity.ok(ApiResponse.success("Case created successfully.", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<CaseSummaryResponse>>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) CaseStatus status,
            @RequestParam(required = false) CasePriority priority,
            @RequestParam(required = false) Long investigatorId,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("Cases retrieved.",
                caseService.search(keyword, status, priority, investigatorId, pageable)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CaseResponse>> get(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Case retrieved.", caseService.getCase(id)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'INVESTIGATOR')")
    public ResponseEntity<ApiResponse<CaseResponse>> update(@PathVariable Long id,
                                                              @RequestBody CaseUpdateRequest request,
                                                              HttpServletRequest httpRequest) {
        CaseResponse response = caseService.updateCase(id, request, RequestUtil.extractClientIp(httpRequest));
        return ResponseEntity.ok(ApiResponse.success("Case updated successfully.", response));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'INVESTIGATOR')")
    public ResponseEntity<ApiResponse<CaseResponse>> updateStatus(@PathVariable Long id,
                                                                    @Valid @RequestBody CaseStatusUpdateRequest request,
                                                                    HttpServletRequest httpRequest) {
        CaseResponse response = caseService.updateStatus(id, request, RequestUtil.extractClientIp(httpRequest));
        return ResponseEntity.ok(ApiResponse.success("Case status updated successfully.", response));
    }
}
