package com.caselock.controller;

import com.caselock.dto.response.ApiResponse;
import com.caselock.dto.response.ChainOfCustodyResponse;
import com.caselock.dto.response.PageResponse;
import com.caselock.service.ChainOfCustodyService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chain-of-custody")
@RequiredArgsConstructor
public class ChainOfCustodyController {

    private final ChainOfCustodyService chainOfCustodyService;

    @GetMapping("/case/{caseId}")
    public ResponseEntity<ApiResponse<PageResponse<ChainOfCustodyResponse>>> forCase(
            @PathVariable Long caseId,
            @PageableDefault(size = 20, sort = "eventTimestamp", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("Chain of custody retrieved.",
                chainOfCustodyService.timelineForCase(caseId, pageable)));
    }
}
